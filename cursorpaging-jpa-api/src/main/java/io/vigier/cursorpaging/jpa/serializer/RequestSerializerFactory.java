package io.vigier.cursorpaging.jpa.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;

import static java.util.Objects.requireNonNull;

@Builder
@RequiredArgsConstructor
public class RequestSerializerFactory {

    private final ConversionService conversionService;

    @Builder.Default
    private final Encrypter encrypter = Encrypter.getInstance();

    @Builder.Default
    private final Map<Class<?>, RequestSerializer<?>> entitySerializers = new ConcurrentHashMap<>();

    public static class RequestSerializerFactoryBuilder {
        public <T> RequestSerializerFactoryBuilder serialalizer( final RequestSerializer<T> s ) {
            if ( this.entitySerializers$value == null ) {
                this.entitySerializers$value = new ConcurrentHashMap<>();
            }
            this.entitySerializers$set = true;
            this.entitySerializers$value.put(
                    requireNonNull( s.getEntityType(), "Serializer must have an entity-type" ), s );
            return this;
        }
    }

    public static RequestSerializerFactory create( final Consumer<RequestSerializerFactoryBuilder> c ) {
        final RequestSerializerFactoryBuilder b = RequestSerializerFactory.builder();
        c.accept( b );
        return b.build();
    }

    @SuppressWarnings( "unchecked" )
    public <T> RequestSerializer<T> forEntity( final Class<T> entityClass ) {
        return (RequestSerializer<T>) entitySerializers.computeIfAbsent( entityClass,
                k -> RequestSerializer.create( entityClass )
                        .apply( b -> b.encrypter( encrypter ).conversionService( conversionService ) ) );
    }

}
