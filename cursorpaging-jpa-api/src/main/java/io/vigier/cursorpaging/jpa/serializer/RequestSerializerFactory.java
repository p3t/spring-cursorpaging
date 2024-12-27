package io.vigier.cursorpaging.jpa.serializer;

import io.vigier.cursorpaging.jpa.serializer.RequestSerializer.RequestSerializerBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;

import static java.util.Objects.requireNonNull;

/**
 * The {@link RequestSerializerFactory} is a factory for creating {@link RequestSerializer}s for entities. It
 * distributes the common parts, i.e. the encrypter and the conversion service, but also does a caching of the
 * serializer in order to reuse them when they are requested more than once. Reuse is important as the serializer need
 * to know the (java-) types of the properties/attributes when they <b>de</b>serialize a request. This mapping can be
 * pre-configured (save in a cluster/multi-node setup) or they learn it when a request is serialized.
 */
@Builder
@RequiredArgsConstructor
public class RequestSerializerFactory {

    @Builder.Default
    private final ConversionService conversionService = new SimpleConversionService();

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

    public static RequestSerializerFactory create() {
        return RequestSerializerFactory.builder()
                .build();
    }

    @SuppressWarnings( "unchecked" )
    public <T> RequestSerializer<T> forEntity( final Class<T> entityClass ) {
        return (RequestSerializer<T>) entitySerializers.computeIfAbsent( entityClass,
                k -> RequestSerializer.create( entityClass )
                        .apply( b -> b.encrypter( encrypter ).conversionService( conversionService ) ) );
    }

    /**
     * Can be used to pre-configure serializers - builder will only be invoked one time per entity-class.
     *
     * @param entityClass class for which the serializers should serialize/deserialize page-requests
     * @param b           the customizer to configure the serializer
     * @param <T>         the entity-type
     * @return this factory
     */
    public <T> RequestSerializerFactory configure( final Class<T> entityClass,
            final Consumer<RequestSerializerBuilder<T>> b ) {
        entitySerializers.computeIfAbsent( entityClass, k -> RequestSerializer.create( entityClass ).apply( c -> {
            c.encrypter( encrypter ).conversionService( conversionService );
            b.accept( c );
        } ) );
        return this;
    }

}
