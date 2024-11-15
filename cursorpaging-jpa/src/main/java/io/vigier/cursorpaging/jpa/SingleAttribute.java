package io.vigier.cursorpaging.jpa;

import lombok.EqualsAndHashCode;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;

/**
 * Single attribute of an entity, i.e. not an embedded entity
 */
public record SingleAttribute(
        String name,
        Class<?> type ) {

    /**
     * Create a new instance of SingleAttribute.
     *
     * @param name the name of the attribute.
     * @param type the type of the attribute.
     * @return the new instance.
     */
    public static SingleAttribute of( final String name, final Class<?> type ) {
        return new SingleAttribute( name, type );
    }

    /**
     * Create a new instance of SingleAttribute.
     *
     * @param attribute the attribute from the JPA metamodel.
     * @return the new instance.
     */
    public static SingleAttribute of( final jakarta.persistence.metamodel.Attribute<?, ?> attribute ) {
        return new SingleAttribute( attribute.getName(), attribute.getJavaType() );
    }

    /**
     * Get the value of the attribute from the entity.
     *
     * @param entity entity which does have the attribute defined by this instance.
     * @return the value of the attribute if found.
     */
    Comparable<?> valueOf( final Object entity ) {
        return (Comparable<?>) new DirectFieldAccessFallbackBeanWrapper( entity ).getPropertyValue( name );
    }
}
