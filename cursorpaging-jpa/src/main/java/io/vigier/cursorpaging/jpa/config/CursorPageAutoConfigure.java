package io.vigier.cursorpaging.jpa.config;

import io.vigier.cursorpaging.jpa.repository.CursorPageRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for CursorPageRepository.
 * For using the cursor page repository in your application, you need to add:
 *  <pre>{@code
 *     @EnableJpaRepositories( repositoryFactoryBeanClass = CursorPageRepositoryFactoryBean.class )
 * }</pre>
 * to your Application class.
 */
@AutoConfiguration
@ComponentScan( basePackageClasses = { CursorPageRepository.class } )
@ConditionalOnBean( EntityManagerFactory.class )
public class CursorPageAutoConfigure {


}
