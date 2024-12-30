package io.vigier.cursorpaging.jpa.serializer;

import java.time.Instant;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Very simple conversion service which might help to do create some test without the need of setting up a more
 * sophisticated conversion service, but is not meant to be used in production.
 */
class SimpleConversionService implements ConversionService {
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
            return targetType.cast( source.toString() );
        }
        if ( targetType == Integer.class ) {
            return targetType.cast( Integer.valueOf( source.toString() ) );
        }
        if ( targetType == Long.class ) {
            return targetType.cast( Long.valueOf( source.toString() ) );
        }
        if ( targetType == Boolean.class ) {
            return targetType.cast( "true".equals( source.toString() ) );
        }
        if ( targetType == Instant.class ) {
            return targetType.cast( Instant.parse( source.toString() ) );
        }
        if ( targetType == Object.class ) {
            return targetType.cast( source );
        }
        return null;
    }

    @Override
    public Object convert( final Object source, final TypeDescriptor sourceType, final TypeDescriptor targetType ) {
        return convert( source, targetType.getObjectType() );
    }
}
