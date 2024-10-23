package io.vigier.cursorpaging.jpa.serializer;


import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.SneakyThrows;
import org.springframework.core.convert.ConversionService;

@Builder
public class RequestSerializer<E> {

    @Builder.Default
    private Map<String, Attribute> attributes = new ConcurrentHashMap<>();

    @Builder.Default
    private final Encrypter encrypter = Encrypter.getInstance();

    private final ConversionService conversionService;

    @Builder.Default
    private final Map<String, RuleFactory> filterRuleFactories = new HashMap<>();

    public static class RequestSerializerBuilder<E> {

        public RequestSerializerBuilder<E> use( final Attribute attribute ) {
            if ( this.attributes$value == null ) {
                this.attributes$value = new ConcurrentHashMap<>();
            }
            this.attributes$value.put( attribute.name(), attribute );
            attributes$set = true;
            return this;
        }

        public RequestSerializerBuilder<E> filterRuleFactory( final String name, final RuleFactory factory ) {
            if ( this.filterRuleFactories$value == null ) {
                this.filterRuleFactories$value = new HashMap<>();
            }
            this.filterRuleFactories$value.put( name, factory );
            filterRuleFactories$set = true;
            return this;
        }
    }

    public static <E> RequestSerializer<E> create( final Consumer<RequestSerializerBuilder<E>> c ) {
        final RequestSerializerBuilder<E> builder = builder();
        c.accept( builder );
        return builder.build();
    }

    public static <E> RequestSerializer<E> create() {
        return create( b -> {
        } );
    }

    public byte[] toBytes( final PageRequest<E> page ) {
        updateAttributes( page );
        final Cursor.PageRequest dtoRequest = ToDtoMapper.<E>create( c -> c.pageRequest( page ) ).map();
        return encrypter.encrypt( dtoRequest.toByteArray() );
    }

    private void updateAttributes( final PageRequest<E> page ) {
        page.positions().forEach( p -> attributes.putIfAbsent( p.attribute().name(), p.attribute() ) );
        page.filters().attributes().forEach( a -> attributes.putIfAbsent( a.name(), a ) );
    }

    public Base64String toBase64( final PageRequest<E> page ) {
        return Base64String.encode( toBytes( page ) ).replace( "=", "" );
    }

    @SneakyThrows
    public PageRequest<E> toPageRequest( final byte[] data ) {
        final var request = Cursor.PageRequest.parseFrom( encrypter.decrypt( data ) );
        final FromDtoMapper<E> fromDtoMapper = FromDtoMapper.create( b -> b.request( request )
                .conversionService( conversionService )
                .ruleFactories( filterRuleFactories )
                .attributesByName( attributes ) );
        return fromDtoMapper.map();
    }

    public PageRequest<E> toPageRequest( final Base64String base64 ) {
        return toPageRequest( base64.decoded() );
    }
}
