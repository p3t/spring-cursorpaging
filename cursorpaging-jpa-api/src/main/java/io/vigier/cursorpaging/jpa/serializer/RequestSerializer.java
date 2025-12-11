package io.vigier.cursorpaging.jpa.serializer;


import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.FilterRule;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.QueryElement;
import io.vigier.cursorpaging.jpa.filter.FilterList;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.lang.Nullable;

@Builder
public class RequestSerializer<E> {

    @Builder.Default
    private Map<String, Attribute> attributes = new ConcurrentHashMap<>();

    @Builder.Default
    private final Encrypter encrypter = Encrypter.getInstance();

    @Getter
    private Class<E> entityType;

    @Builder.Default
    private final ConversionService conversionService = getConversionService();

    @Builder.Default
    private final Map<String, RuleFactory> filterRuleFactories = new HashMap<>();

    static ConversionService getConversionService() {
        final DefaultConversionService cs = new DefaultConversionService();
        Jsr310Converters.getConvertersToRegister().forEach( cs::addConverter );
        // The Object to object converter would try to find a constructor or method in the target
        // type for constrcuting the object from a string. This is not desired, as the
        // Serializer used "toString" for serializing, and if this string is not by intend
        // the string representation of the object, the conversion would create very strange
        // deserialization results (SerializerTest -> shouldThrowExceptionWhenNotDeserializable).
        cs.removeConvertible( Object.class, Object.class );
        return cs;
    }

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

    public interface RequestSerializerCreator<E> extends
            Function<Consumer<RequestSerializerBuilder<E>>, RequestSerializer<E>> {
        default RequestSerializer<E> withDefaults() {
            return apply( b -> {} );
        }
    }

    public static <E> RequestSerializerCreator<E> create( final Class<E> entityClass ) {
        return c -> RequestSerializer.create( entityClass, c );
    }

    public static <E> RequestSerializer<E> create( final Class<E> entityClass,
            final Consumer<RequestSerializerBuilder<E>> c ) {
        final RequestSerializerBuilder<E> builder = builder();
        builder.entityType( entityClass );
        c.accept( builder );
        return builder.build();
    }

    public byte[] toBytes( final PageRequest<E> page ) {
        updateAttributes( page );
        verifyFilterRuleFactories( page );
        final Cursor.PageRequest dtoRequest = ToDtoMapper.<E>create( c -> c.pageRequest( page ) ).map();
        return encrypter.encrypt( dtoRequest.toByteArray() );
    }

    private void verifyFilterRuleFactories( final PageRequest<E> page ) {
        page.filters().forEach( this::verifyFilterRuleFactories );
    }

    private void verifyFilterRuleFactories( final QueryElement f ) {
        switch ( f ) {
            case final FilterRule fr when (filterRuleFactories.get( fr.name() ) == null) ->
                    throw new SerializerException(
                            "No factory registered for filter rule with name: " + fr.name() + " (" + fr.getClass()
                                    .getName() + ")" );
            case final FilterList fl -> fl.forEach( this::verifyFilterRuleFactories );
            default -> {
                // nothing to do
            }
        }
    }

    private void updateAttributes( final PageRequest<?> page ) {
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

    /**
     * Converts a base64 encoded String into a page-request
     *
     * @param cursorStr The encoded string (can be {@code null})
     * @return a present optional if the input value was present.
     */
    public Optional<PageRequest<E>> stringToPageRequest( @Nullable final String cursorStr ) {
        return Optional.ofNullable( cursorStr != null ? (!cursorStr.isBlank() ? cursorStr : null) : null )
                .map( Base64String::new )
                .map( this::toPageRequest );
    }
}
