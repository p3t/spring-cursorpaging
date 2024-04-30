package io.vigier.cursor.testapp;

import io.vigier.cursor.jpa.bootstrap.CursorPageRepositoryFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.hateoas.config.EnableHypermediaSupport;

@SpringBootApplication
@EnableHypermediaSupport( type = { EnableHypermediaSupport.HypermediaType.HAL } )
@EnableJpaRepositories( repositoryFactoryBeanClass = CursorPageRepositoryFactoryBean.class )
public class TestApplication {

    public static void main( final String[] args ) {
        SpringApplication.run( TestApplication.class, args );
    }

}
