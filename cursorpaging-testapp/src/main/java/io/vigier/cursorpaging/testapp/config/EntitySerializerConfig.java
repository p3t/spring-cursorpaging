package io.vigier.cursorpaging.testapp.config;

import io.vigier.cursorpaging.jpa.serializer.Encrypter;
import io.vigier.cursorpaging.jpa.serializer.EntitySerializer;
import io.vigier.cursorpaging.jpa.serializer.EntitySerializerFactory;
import io.vigier.cursorpaging.testapp.model.DataRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

@Configuration
public class EntitySerializerConfig {

    @Value( "${cursorpaging.jpa.serializer.encrypter.secret:1234567890ABCDEFGHIJKlmnopqrst--}" )
    private String encrypterSecret;

    @Bean
    public EntitySerializerFactory entitySerializerFactory( final ConversionService conversionService ) {
        return EntitySerializerFactory.builder()
                .conversionService( conversionService )
                .encrypter( Encrypter.getInstance( encrypterSecret ) )
                .build();
    }

    @Bean
    public EntitySerializer<DataRecord> dataRecordEntitySerializer( final EntitySerializerFactory serializerFactory ) {
        return serializerFactory.forEntity( DataRecord.class );
    }
}
