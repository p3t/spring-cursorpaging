package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.bootstrap.CursorPageRepositoryFactoryBean;
import io.vigier.cursorpaging.jpa.config.CursorPageAutoConfigure;
import io.vigier.cursorpaging.jpa.impl.CursorPageRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@ExtendWith( MockitoExtension.class )
public class AutoConfigurationTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private EntityManagerFactory entityManagerFactory;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration( AutoConfigurations.of( CursorPageAutoConfigure.class ) );

    // Currently the configuration does not produce any bean..
    private final ApplicationContextRunner enabledContextRunner = new ApplicationContextRunner()
            .withConfiguration( AutoConfigurations.of( CursorPageAutoConfigure.class ) )
            .withBean( EntityManager.class, () -> entityManager )
            .withBean( EntityManagerFactory.class, () -> entityManagerFactory );

    @Test
    void isDisabledWhenNoEntityMangerIsPresent() {
        contextRunner.run( context -> {
            Assertions.assertThat( context ).doesNotHaveBean( CursorPageRepositoryImpl.class );
            Assertions.assertThat( context ).doesNotHaveBean( CursorPageRepositoryFactoryBean.class );
        } );
    }

    @Test
    void shouldLoadWhenEntityManagerIsPresent() {
        enabledContextRunner.run( context -> {
            Assertions.assertThat( context ).hasSingleBean( CursorPageAutoConfigure.class );
        } );
    }
}
