package io.vigier.cursorpaging.example.webapp.config;

import io.vigier.cursorpaging.example.webapp.model.DataRecord;
import io.vigier.cursorpaging.jpa.rsql.filter.RsqlFilterFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RsqlFactoryConfig {

    @Bean
    public RsqlFilterFactory<DataRecord> dataRecordRsqlFilterFactory( final EntityManager entityManager ) {
        return new RsqlFilterFactory<>( entityManager, entityManager.getMetamodel().entity( DataRecord.class ) );
    }
}

