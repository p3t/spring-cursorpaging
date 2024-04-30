package io.vigier.cursor.jpa.impl;


import io.vigier.cursor.jpa.Page;
import io.vigier.cursor.jpa.PageRequest;
import io.vigier.cursor.jpa.repository.CursorPageRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;

/**
 * Implementation of the CursorPageRepository.
 *
 * @param <E> the type of the data.
 */
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
        final Criterias<E> c = Criterias.fromEntity( entityInformation.getJavaType(), entityManager );

        request.positions().forEach( position -> position.apply( c ) );
        request.filters().forEach( filter -> filter.apply( c ) );

        final var results = entityManager.createQuery( c.query() )
                .setMaxResults( getMaxResultSize( request ) )
                .getResultList();

        return Page.create( b -> b.content( toContent( results, request ) )
                .self( request )
                .next( toNextRequest( results, request ) ) );
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