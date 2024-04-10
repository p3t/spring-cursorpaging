package io.vigier.cursor.testapp;

import io.vigier.cursor.repository.bootstrap.CursorPageRepositoryFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories( repositoryFactoryBeanClass = CursorPageRepositoryFactoryBean.class )
public class TestApplication {

    public static void main( final String[] args ) {
        SpringApplication.run( TestApplication.class, args );
    }

}
