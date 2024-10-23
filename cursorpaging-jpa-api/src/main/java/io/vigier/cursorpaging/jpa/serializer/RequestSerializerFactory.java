package io.vigier.cursorpaging.jpa.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;

@Builder
@RequiredArgsConstructor
public class RequestSerializerFactory {

    private final ConversionService conversionService;

    @Builder.Default
    private final Encrypter encrypter = Encrypter.getInstance();

    @Builder.Default
    private final Map<Class<?>, RequestSerializer<?>> entitySerializers = new ConcurrentHashMap<>();

    @SuppressWarnings( "unchecked" )
    public <T> RequestSerializer<T> forEntity( final Class<T> entityClass ) {
        return (RequestSerializer<T>) entitySerializers.computeIfAbsent( entityClass,
                k -> RequestSerializer.create( b -> b.encrypter( encrypter ).conversionService( conversionService ) ) );
    }
}
