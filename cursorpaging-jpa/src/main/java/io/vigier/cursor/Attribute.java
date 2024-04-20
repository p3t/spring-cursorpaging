package io.vigier.cursor;

import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;

public record Attribute(
        String name,
        Class<? extends Comparable<?>> type ) {

    public static Attribute of( final String name, final Class<? extends Comparable<?>> type ) {
        return new Attribute( name, type );
    }

    public static Attribute of( final SingularAttribute<?, ? extends Comparable<?>> sa ) {
        return Attribute.of( sa.getName(), sa.getJavaType() );
    }

    Comparable<?> valueOf( final Object entity ) {
        final Object value = new DirectFieldAccessFallbackBeanWrapper( entity ).getPropertyValue( name );
        return type.cast( value );
    }
}
