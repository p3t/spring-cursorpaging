package io.vigier.cursor;

import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;

public record SingleAttribute(
        String name,
        Class<?> type ) {

    public static SingleAttribute of( final String name, final Class<?> type ) {
        return new SingleAttribute( name, type );
    }

    public static SingleAttribute of( final SingularAttribute<?, ?> attribute ) {
        return new SingleAttribute( attribute.getName(), attribute.getJavaType() );
    }

    Comparable<?> valueOf( final Object entity ) {
        final Object value = new DirectFieldAccessFallbackBeanWrapper( entity ).getPropertyValue( name );
        return (Comparable<?>) value;
    }
}
