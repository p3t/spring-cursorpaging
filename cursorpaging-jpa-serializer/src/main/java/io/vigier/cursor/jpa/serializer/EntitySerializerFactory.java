package io.vigier.cursor.jpa.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;

@Builder
@RequiredArgsConstructor
public class EntitySerializerFactory {

    private final ConversionService conversionService;

    @Builder.Default
    private final Encrypter encrypter = Encrypter.getInstance();

    @Builder.Default
    private final Map<Class<?>, EntitySerializer<?>> entitySerializers = new ConcurrentHashMap<>();

    @SuppressWarnings( "unchecked" )
    public <T> EntitySerializer<T> forEntity( final Class<T> entityClass ) {
        return (EntitySerializer<T>) entitySerializers.computeIfAbsent( entityClass,
                k -> EntitySerializer.create( b -> b.encrypter( encrypter ).conversionService( conversionService ) ) );
    }
}
