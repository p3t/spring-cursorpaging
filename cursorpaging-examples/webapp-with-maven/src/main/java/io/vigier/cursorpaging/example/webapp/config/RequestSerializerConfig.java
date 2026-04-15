package io.vigier.cursorpaging.example.webapp.config;

import io.vigier.cursorpaging.example.webapp.model.DataRecord;
import io.vigier.cursorpaging.jpa.serializer.Encrypter;
import io.vigier.cursorpaging.jpa.serializer.RequestSerializer;
import io.vigier.cursorpaging.jpa.serializer.RequestSerializerFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

@Configuration
@Slf4j
public class RequestSerializerConfig {

    @Value( "${cursorpaging.jpa.serializer.encrypter.secret:1234567890ABCDEFGHIJKlmnopqrst--}" )
    private String encrypterSecret;

    @Bean
    public RequestSerializerFactory requestSerializerFactory( final ConversionService conversionService,
            final EntityManager entityManager ) {
        log.info( "ConversionService: {}", conversionService.getClass().getName() );
        return RequestSerializerFactory.builder()
                .conversionService( conversionService )
                .encrypter( Encrypter.getInstance( encrypterSecret ) )
                .entityManager( entityManager )
                .build();
    }

    @Bean
    public RequestSerializer<DataRecord> dataRecordRequestSerializer(
            final RequestSerializerFactory serializerFactory ) {
        return serializerFactory.forEntity( DataRecord.class );
    }
}
