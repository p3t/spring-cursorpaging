package io.vigier.cursor.jpa.serializer;


import io.vigier.cursor.jpa.Attribute;
import io.vigier.cursor.jpa.PageRequest;
import io.vigier.cursor.jpa.serializer.dto.Cursor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.SneakyThrows;
import org.springframework.core.convert.ConversionService;

@Builder
public class EntitySerializer<E> {

    @Builder.Default
    private Map<String, Attribute> attributes = new ConcurrentHashMap<>();

    @Builder.Default
    private final Encrypter encrypter = Encrypter.getInstance();

    private final ConversionService conversionService;

    public static class EntitySerializerBuilder<E> {

        public EntitySerializerBuilder<E> use( final Attribute attribute ) {
            if ( this.attributes$value == null ) {
                this.attributes$value = new ConcurrentHashMap<>();
            }
            this.attributes$value.put( attribute.name(), attribute );
            attributes$set = true;
            return this;
        }
    }

    public static <E> EntitySerializer<E> create( final Consumer<EntitySerializerBuilder<E>> c ) {
        final EntitySerializerBuilder<E> builder = builder();
        c.accept( builder );
        return builder.build();
    }

    public static <E> EntitySerializer<E> create() {
        return create( b -> {
        } );
    }

    public byte[] toBytes( final PageRequest<E> page ) {
        updateAttributes( page );
        final Cursor.PageRequest dtoRequest = ToDtoMapper.of( page ).map();
        return encrypter.encrypt( dtoRequest.toByteArray() );
    }

    private void updateAttributes( final PageRequest<E> page ) {
        page.positions().forEach( p -> attributes.putIfAbsent( p.attribute().name(), p.attribute() ) );
        page.filters().forEach( f -> attributes.putIfAbsent( f.attribute().name(), f.attribute() ) );
    }

    public String toBase64( final PageRequest<E> page ) {
        return new String( Base64.getUrlEncoder().encode( toBytes( page ) ) );
    }

    public String toByteString( final PageRequest<E> page ) {
        return new String( toBytes( page ), StandardCharsets.UTF_8 );
    }

    public PageRequest<E> fromByteString( final String byteString ) {
        return toPageRequest( byteString.getBytes(StandardCharsets.UTF_8) );
    }

    @SneakyThrows
    public PageRequest<E> toPageRequest( final byte[] data ) {
        final var request = Cursor.PageRequest.parseFrom( encrypter.decrypt( data ) );
        final FromDtoMapper<E> fromDtoMapper = FromDtoMapper.create(
                b -> b.request( request ).conversionService( conversionService ).attributesByName( attributes ) );
        return fromDtoMapper.map();
    }

    public PageRequest<E> toPageRequest( final String base64 ) {
        return toPageRequest( Base64.getUrlDecoder().decode( base64 ) );
    }
}
