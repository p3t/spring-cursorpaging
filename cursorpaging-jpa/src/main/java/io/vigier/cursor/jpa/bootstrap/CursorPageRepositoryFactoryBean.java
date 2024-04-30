package io.vigier.cursor.jpa.bootstrap;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Factory bean for the CursorPageRepositoryFactory.
 *
 * @param <T> the type of the repository (factory)
 */
public class CursorPageRepositoryFactoryBean<T extends JpaRepository<Object, Serializable>>
        extends JpaRepositoryFactoryBean<T, Object, Serializable> {

    public CursorPageRepositoryFactoryBean( final Class<? extends T> repositoryInterface ) {
        super( repositoryInterface );
    }

    @Override
    protected @NonNull RepositoryFactorySupport createRepositoryFactory( @NonNull final EntityManager entityManager ) {
        return new CursorPageJpaRepositoryFactory( entityManager );
    }
}