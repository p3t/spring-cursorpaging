package io.vigier.cursorpaging.jpa.bootstrap;

import jakarta.persistence.EntityManager;
import lombok.NonNull;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Factory bean for the CursorPageRepositoryFactory.
 *
 * @param <T> the type of the repository (factory)
 */
public class CursorPageRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends
        JpaRepositoryFactoryBean<T, S, ID> {

    public CursorPageRepositoryFactoryBean( final Class<? extends T> repositoryInterface ) {
        super( repositoryInterface );
    }

    @Override
    protected @NonNull RepositoryFactorySupport createRepositoryFactory( @NonNull final EntityManager entityManager ) {
        return new CursorPageJpaRepositoryFactory( entityManager );
    }
}