package io.vigier.cursorpaging.jpa.impl;


import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.Page;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.repository.CursorPageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;

/**
 * Implementation of the CursorPageRepository.
 *
 * @param <E> the type of the data.
 */
@Slf4j
public class CursorPageRepositoryImpl<E> implements CursorPageRepository<E> {

    private static final int ADDED_TO_PAGE_SIZE = 1; // just for readability MUST be 1!
    private final JpaEntityInformation<E, ?> entityInformation;
    private final EntityManager entityManager;

    /**
     * Creates a new {@link CursorPageRepositoryImpl}.
     *
     * @param domainClass   the domain class.
     * @param entityManager the entity manager.
     */
    @SuppressWarnings( "unused" )
    public CursorPageRepositoryImpl( final Class<E> domainClass, final EntityManager entityManager ) {
        this( JpaEntityInformationSupport.getEntityInformation( domainClass, entityManager ), entityManager );
    }

    /**
     * Creates a new {@link CursorPageRepositoryImpl}.
     *
     * @param entityInformation the entity information.
     * @param entityManager     the entity manager.
     */
    public CursorPageRepositoryImpl( final JpaEntityInformation<E, ?> entityInformation,
            final EntityManager entityManager ) {
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    @Override
    public Page<E> loadPage( final PageRequest<E> request ) {
        if ( request == null || request.pageSize() < 0 ) {
            throw new IllegalArgumentException( "Invalid page request: " + request );
        }
        final CriteriaQueryBuilder<E, E> cqb = CriteriaQueryBuilder.forEntity( entityInformation.getJavaType(),
                entityManager );

        addPositionQuery( request, cqb );

        cqb.andWhere( request.filters()
                .toPredicate( cqb ) );

        request.positions()
                .forEach( position -> cqb.orderBy( position.attribute(), position.order() ) );

        final var results = entityManager.createQuery( cqb.query()
                        .distinct( true ) )
                .setMaxResults( getMaxResultSize( request ) )
                .getResultList();

        final PageRequest<E> self = request.enableTotalCount() && request.totalCount()
                .isEmpty() ? request.copy( b -> b.totalCount( count( request ) ) ) : request;

        return Page.create( b -> b.content( toContent( results, self ) ) //
                .self( self ) //
                .next( toNextRequest( results, self ) ) //
                .entityType( entityInformation.getJavaType() ) );
    }

    private void addPositionQuery( final PageRequest<E> request, final CriteriaQueryBuilder<E, E> cqb ) {
        final List<Predicate> valueConditions = new LinkedList<>();

        if ( !request.isFirstPage() ) {

            // Keyset condition on the values of the last record of the current page:
            //   (a > :a) OR (a = :a AND b > :b) OR (a = :a AND b = :b AND id > :id)
            // Null values follow the database ordering (PostgreSQL): ASC => nulls last, DESC => nulls first.
            // A reversed request flips the order and with it the null placement, so the same rules apply.
            for ( final var position : request.positions() ) {
                final var attribute = position.attribute();
                if ( position.hasValue() ) {
                    cqb.orWhere( and( valueConditions, switch ( position.order() ) {
                        case ASC -> cqb.cb()
                                .or( cqb.greaterThan( attribute, position.value() ),
                                        cqb.isNull( attribute ) ); // nulls are last
                        case DESC -> cqb.lessThan( attribute, position.value() );
                    } ) );
                    valueConditions.add( cqb.equalTo( attribute, position.value() ) );
                } else {
                    if ( position.order() == Order.DESC ) {
                        cqb.orWhere( and( valueConditions, cqb.cb()
                                .not( cqb.isNull( attribute ) ) ) ); // nulls are first
                    }
                    valueConditions.add( cqb.isNull( attribute ) );
                }
            }
        }
    }

    public List<Predicate> and( final List<Predicate> andConditions, final Predicate condition ) {
        final List<Predicate> conditions = new ArrayList<>( andConditions.size() + 1 );
        conditions.addAll( andConditions );
        conditions.add( condition );
        return Collections.unmodifiableList( conditions );
    }

    @Override
    public long count( final PageRequest<E> request ) {
        final CriteriaQueryBuilder<E, Long> cqb = CriteriaQueryBuilder.forCount( entityInformation.getJavaType(),
                entityManager );

        request.filters()
                .forEach( filter -> cqb.andWhere( filter.toPredicate( cqb ) ) );

        return entityManager.createQuery( cqb.query() )
                .getSingleResult();
    }

    private int getMaxResultSize( final PageRequest<E> request ) {
        // we add one to check if there are more pages
        return request.pageSize() + ADDED_TO_PAGE_SIZE;
    }

    /**
     * Truncate the result list to the desired size if needed (was increased by 1 in order to find out if there are more
     * records to fetch after this page)
     *
     * @param results the result list
     * @param request request used to fetch the results
     * @return the truncated list
     */
    private List<E> toContent( final List<E> results, final PageRequest<E> request ) {
        final var pageContent = truncateResultsToRequestSize( results, request );
        if ( request.isReversed() ) {
            Collections.reverse( pageContent );
        }
        return pageContent;
    }

    private static <E> List<E> truncateResultsToRequestSize( final List<E> results, final PageRequest<E> request ) {
        if ( hasNextPage( results, request ) ) {
            return results.subList( 0, request.pageSize() );
        }
        return results;
    }

    private PageRequest<E> toNextRequest( final List<E> results, final PageRequest<E> request ) {
        if ( hasNextPage( results, request ) ) {
            return request.positionOf( getLastOnPage( results, request ), getFirstOnNextPage( results, request ) );
        }
        return null;
    }

    private static <E> boolean hasNextPage( final List<E> results, final PageRequest<E> request ) {
        return results.size() > request.pageSize();
    }

    private E getLastOnPage( final List<E> results, final PageRequest<E> request ) {
        if ( !hasNextPage( results, request ) ) {
            throw new NoSuchElementException( "No more pages available, getting last not applicable" );
        }
        return results.get( results.size() - 1 - ADDED_TO_PAGE_SIZE );
    }

    private E getFirstOnNextPage( final List<E> results, final PageRequest<E> request ) {
        if ( !hasNextPage( results, request ) ) {
            throw new NoSuchElementException( "No more pages available" );
        }
        return results.getLast();
    }

}