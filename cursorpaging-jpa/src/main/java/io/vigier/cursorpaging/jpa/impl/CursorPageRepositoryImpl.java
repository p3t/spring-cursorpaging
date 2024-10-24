package io.vigier.cursorpaging.jpa.impl;


import io.vigier.cursorpaging.jpa.Page;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.repository.CursorPageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.LinkedList;
import java.util.List;
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

    private static final int ADDED_TO_PAGE_SIZE = 1;
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
        final CriteriaQueryBuilder<E, E> cqb = CriteriaQueryBuilder.forEntity( entityInformation.getJavaType(),
                entityManager );

        final List<Predicate> positionEquals = new LinkedList<>();
        request.positions().forEach( position -> {
            if ( !request.isFirstPage() ) {
                cqb.orWhere( position.conditionsAnd( cqb, positionEquals ) );
                positionEquals.add( position.equalTo( cqb ) );
            }
            cqb.orderBy( position.attribute(), position.order() );
        } );
        cqb.andWhere( request.filters().toPredicate( cqb ) );
        request.rules().forEach( rule -> cqb.andWhere( rule.toPredicate( cqb ) ) );

        final var results = entityManager.createQuery( cqb.query().distinct( true ) )
                .setMaxResults( getMaxResultSize( request ) )
                .getResultList();

        final PageRequest<E> self = request.enableTotalCount() && request.totalCount().isEmpty() ? request.copy(
                b -> b.totalCount( count( request ) ) ) : request;

        return Page.create( b -> b.content( toContent( results, self ) ) //
                .self( self ) //
                .next( toNextRequest( results, self ) ) );
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
        if ( results.size() <= request.pageSize() ) {
            return results;
        }
        return results.subList( 0, request.pageSize() );
    }

    private PageRequest<E> toNextRequest( final List<E> results, final PageRequest<E> request ) {
        if ( results.size() <= request.pageSize() ) {
            return null;
        }
        return request.positionOf( getLast( results ) );
    }

    private E getLast( final List<E> results ) {
        return results.get( results.size() - 1 - ADDED_TO_PAGE_SIZE );
    }

}