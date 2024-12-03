package io.vigier.cursorpaging.jpa.serializer;


import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.SneakyThrows;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

@Builder
public class RequestSerializer<E> {

    @Builder.Default
    private Map<String, Attribute> attributes = new ConcurrentHashMap<>();

    @Builder.Default
    private final Encrypter encrypter = Encrypter.getInstance();

    @Builder.Default
    private final ConversionService conversionService = new ConversionService() {
        @Override
        public boolean canConvert( final Class<?> sourceType, final Class<?> targetType ) {
            if ( sourceType == null || targetType == null ) {
                return false;
            }
            return sourceType.isAssignableFrom( String.class ) && (targetType.isAssignableFrom( Long.class )
                    || targetType.isAssignableFrom( Integer.class ) || targetType.isAssignableFrom( Boolean.class )
                    || targetType.isAssignableFrom( String.class ) || targetType.isAssignableFrom( Object.class ));
        }

        @Override
        public boolean canConvert( final TypeDescriptor sourceType, final TypeDescriptor targetType ) {
            if ( sourceType == null || targetType == null ) {
                return false;
            }
            return canConvert( sourceType.getType(), targetType.getType() );
        }

        @Override
        public <T> T convert( final Object source, final Class<T> targetType ) {
            if ( targetType == String.class ) {
                return (T) source.toString();
            }
            if ( targetType == Integer.class ) {
                return (T) Integer.valueOf( source.toString() );
            }
            if ( targetType == Long.class ) {
                return (T) Long.valueOf( source.toString() );
            }
            if ( targetType == Boolean.class ) {
                return (T) Boolean.valueOf( source.toString().equals( "true" ) );
            }
            if ( targetType == Object.class ) {
                return (T) source;
            }
            return null;
        }

        @Override
        public Object convert( final Object source, final TypeDescriptor sourceType, final TypeDescriptor targetType ) {
            return convert( source, targetType.getObjectType() );
        }
    };

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
