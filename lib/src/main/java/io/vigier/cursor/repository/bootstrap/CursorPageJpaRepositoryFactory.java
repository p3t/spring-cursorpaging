package io.vigier.cursor.repository.bootstrap;

import io.vigier.cursor.repository.CursorPageRepository;
import io.vigier.cursor.repository.impl.CursorPageRepositoryImpl;
import jakarta.persistence.EntityManager;
import lombok.NonNull;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFragment;


public class CursorPageJpaRepositoryFactory extends JpaRepositoryFactory {

    /**
     * Creates a new {@link JpaRepositoryFactory}.
     *
     * @param entityManager must not be {@literal null}
     */
    public CursorPageJpaRepositoryFactory( @NonNull final EntityManager entityManager ) {
        super( entityManager );
    }

    @Override
    protected @NonNull RepositoryFragments getRepositoryFragments(
            @NonNull final RepositoryMetadata metadata,
            @NonNull final EntityManager entityManager,
            @NonNull final EntityPathResolver resolver,
            @NonNull final CrudMethodMetadata crudMethodMetadata ) {
        var fragments = super.getRepositoryFragments( metadata, entityManager, resolver, crudMethodMetadata );

        if ( CursorPageRepository.class.isAssignableFrom( metadata.getRepositoryInterface() ) ) {
            fragments = fragments.append(
                    RepositoryFragment.implemented( CursorPageRepository.class,
                            new CursorPageRepositoryImpl<>( getEntityInformation( metadata.getDomainType() ),
                                    entityManager ) ) );
        }
        return fragments;
    }
}
