package io.vigier.cursorpaging.jpa.serial;


import io.vigier.cursor.Attribute;
import io.vigier.cursor.PageRequest;
import io.vigier.cursorpaging.jpa.serial.dto.Cursor;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.SneakyThrows;

@Builder
public class Serializer {

    @Builder.Default
    private Map<String, Attribute> attributes = new ConcurrentHashMap<>();

    @Builder.Default
    private final Encrypter encrypter = Encrypter.getInstance();

    public static class SerializerBuilder {

        public SerializerBuilder use( final Attribute attribute ) {
            if ( this.attributes$value == null ) {
                this.attributes$value = new ConcurrentHashMap<>();
            }
            this.attributes$value.put( attribute.name(), attribute );
            attributes$set = true;
            return this;
        }
    }

    public static Serializer create( final Consumer<SerializerBuilder> c ) {
        final var builder = builder();
        c.accept( builder );
        return builder.build();
    }

    public static Serializer create() {
        return create( b -> {
        } );
    }

    public <E> byte[] toBytes( final PageRequest<E> page ) {
        updateAttributes( page );
        final Cursor.PageRequest dtoRequest = ToDtoMapper.of( page ).map();
        return encrypter.encrypt( dtoRequest.toByteArray() );
    }

    private void updateAttributes( final PageRequest<?> page ) {
        page.positions().forEach( p -> attributes.putIfAbsent( p.attribute().name(), p.attribute() ) );
        page.filters().forEach( f -> attributes.putIfAbsent( f.attribute().name(), f.attribute() ) );
    }

    public String toBase64( final PageRequest<?> page ) {
        return new String( Base64.getUrlEncoder().encode( toBytes( page ) ) );
    }

    @SneakyThrows
    public <E> PageRequest<E> toPageRequest( final byte[] data ) {
        final var request = Cursor.PageRequest.parseFrom( encrypter.decrypt( data ) );
        return FromDtoMapper.<E>of( request ).using( attributes ).map();
    }

    public <E> PageRequest<E> toPageRequest( final String base64 ) {
        return toPageRequest( Base64.getUrlDecoder().decode( base64 ) );
    }
}
