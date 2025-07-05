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

        cqb.andWhere( request.filters().toPredicate( cqb ) );
        request.rules().forEach( rule -> cqb.andWhere( rule.toPredicate( cqb ) ) );

        request.positions().forEach( position -> cqb.orderBy( position.attribute(), position.order() ) );

        final var results = entityManager.createQuery( cqb.query().distinct( true ) )
                .setMaxResults( getMaxResultSize( request ) )
                .getResultList();

        final PageRequest<E> self = request.enableTotalCount() && request.totalCount().isEmpty() ? request.copy(
                b -> b.totalCount( count( request ) ) ) : request;

        return Page.create( b -> b.content( toContent( results, self ) ) //
                .self( self ) //
                .next( toNextRequest( results, self ) ) //
                .entityType( entityInformation.getJavaType() ) );
    }

    private void addPositionQuery( final PageRequest<E> request, final CriteriaQueryBuilder<E, E> cqb ) {
        final List<Predicate> valueConditions = new LinkedList<>();

        if ( !request.isFirstPage() ) {

            // Example: The "from value to null case":
            //            ID                                   | date
            // (pos) ->   33b13398-fcfe-4845-a6e6-cfdfae34d0b2 | 2025-07-04 13:17:30.247433 +00:00
            //            18de3439-1bfa-40fb-bdff-4061097019e8 | null
            //            41084f1b-03f9-4ac9-8b4d-0318ce8bae66 | null
            // date is the first position-attribute, ID the second.
            // The position must use the ID from the next value because the date will be null (cannot be used anymore)
            // if the ID with 33 was used, the next page would skip the 18 record.
            // Using (always) the next value would be wrong for the selection within a block of records with the same value.

            var fromValueToNullCase = false;

            for ( final var position : request.positions() ) {
                if ( position.hasNextValue() ) {
                    if ( fromValueToNullCase ) {
                        cqb.orWhere( and( valueConditions, switch ( position.order() ) {
                            case ASC -> cqb.greaterThanOrEqualTo( position.attribute(), position.nextValue() );
                            case DESC -> cqb.lessThanOrEqualTo( position.attribute(), position.nextValue() );
                        } ) );
                    } else {
                        cqb.orWhere( and( valueConditions, switch ( position.order() ) {
                            case ASC -> cqb.greaterThan( position.attribute(), position.value() );
                            case DESC -> cqb.lessThan( position.attribute(), position.value() );
                        } ) );
                    }
                    valueConditions.add( cqb.equalTo( position.attribute(), position.value() ) );

                    if ( position.order() == Order.ASC ) {
                        cqb.orWhere( cqb.isNull( position.attribute() ) ); // nulls ara last
                    }
                    fromValueToNullCase = false;
                } else {
                    fromValueToNullCase = position.hasValue();
                    valueConditions.add( cqb.isNull( position.attribute() ) );
                    if ( position.order() == Order.DESC ) {
                        cqb.orWhere( cqb.cb().not( cqb.isNull( position.attribute() ) ) ); // nulls are first
                    }
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

        request.filters().forEach( filter -> cqb.andWhere( filter.toPredicate( cqb ) ) );
        request.rules().forEach( rule -> cqb.andWhere( rule.toCountPredicate( cqb ) ) );

        return entityManager.createQuery( cqb.query() ).getSingleResult();
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
        if ( hasNextPage( results, request ) ) {
            return results.subList( 0, request.pageSize() );
        }
        return results;
    }

    private PageRequest<E> toNextRequest( final List<E> results, final PageRequest<E> request ) {
        if ( hasNextPage( results, request ) ) {
            // FIXME: Only one value or last & next value?
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