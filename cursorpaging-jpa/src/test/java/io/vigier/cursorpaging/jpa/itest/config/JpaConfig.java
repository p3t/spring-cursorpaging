package io.vigier.cursorpaging.jpa.itest.config;

import io.vigier.cursorpaging.jpa.bootstrap.CursorPageRepositoryFactoryBean;
import io.vigier.cursorpaging.jpa.itest.repository.DataRecordRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Moved the {@link EnableJpaRepositories} annotation to a separate configuration class, because otherwise a MockMvcTest
 * would require an entity manager factory (bean).
 */
@Configuration
@EnableJpaRepositories( basePackageClasses = DataRecordRepository.class,
        repositoryFactoryBeanClass = CursorPageRepositoryFactoryBean.class )
public class JpaConfig {

}
