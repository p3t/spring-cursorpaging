package io.vigier.cursorpaging.jpa;


import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Singular;


/**
 * Attributes are used to describe a property of an entity which is used within a {@linkplain Position}
 * <p>
 * The attribute uses a list of single attributes to address a certain value of an entity. This "address" is later used
 * to generate queries.
 */
@Builder( toBuilder = true )
public record Attribute( @Singular List<SingleAttribute> attributes ) {

    /**
     * Creates a new attribute from a name and a type.
     *
     * @param name the name of the attribute
     * @param type the type of the attribute
     * @return the attribute
     */
    public static Attribute of( final String name, final Class<? extends Comparable<?>> type ) {
        return new Attribute( List.of( SingleAttribute.of( name, type ) ) );
    }

    /**
     * Creates a new attribute from a singular attribute.
     *
     * @param sa the property from the JPA metamodel
     * @return a new Attribute
     */
    public static Attribute of( final SingularAttribute<?, ? extends Comparable<?>> sa ) {
        return new Attribute( List.of( SingleAttribute.of( sa ) ) );
    }

    /**
     * Creates a new attribute with a path of single attributes. This is needed to address properties of embedded
     * entities
     *
     * @param path Path pointing to an attribute (of an embedded entity)
     * @return The attribute describing the path and the type of the single attributes
     */
    public static Attribute path( final jakarta.persistence.metamodel.Attribute<?, ?>... path ) {
        return new Attribute( Arrays.stream( path ).map( SingleAttribute::of ).toList() );
    }

    /**
     * Creates a new attribute with a path of single attributes. This is needed to address properties of embedded
     * entities
     *
     * @param path Path pointing to an attribute (of an embedded entity)
     * @return The attribute describing the path and the type of the single attributes
     */
    public static Attribute path( final SingleAttribute... path ) {
        return new Attribute( Arrays.asList( path ) );
    }
    
    public static Attribute path( final String name1, final Class<?> type1, final String name2, final Class<?> type2 ) {
        return new Attribute( List.of( SingleAttribute.of( name1, type1 ), SingleAttribute.of( name2, type2 ) ) );
    }

    public static Attribute path( final String name1, final Class<?> type1, final String name2, final Class<?> type2,
            final String name3, final Class<?> type3 ) {
        return new Attribute( List.of( SingleAttribute.of( name1, type1 ), SingleAttribute.of( name2, type2 ),
                SingleAttribute.of( name3, type3 ) ) );
    }

    Comparable<?> valueOf( final Object entity ) {
        Object result = entity;
        for ( final SingleAttribute a : attributes ) {
            result = a.valueOf( result );
        }
        if ( !(result instanceof Comparable<?>) ) {
            throw new IllegalStateException( "Attribute %s is not a comparable: %s".formatted( toString(), result ) );
        }
        return (Comparable<?>) result;
    }

    @Override
    public String toString() {
        return name() + " : " + type().getSimpleName();
    }

    /**
     * Get the name/complete path-name of this attribute
     *
     * @return The attribute-path pointing the property of an entity
     */
    public String name() {
        return String.join( ".", attributes.stream().map( SingleAttribute::name ).toList() );
    }

    /**
     * Generate a criteria expression/path using the given entity-root.
     *
     * @param root The root of the entity
     * @param <E>  Entity type
     * @param <V>  Value type
     * @return The path to the attribute
     */
    public <E, V extends Comparable<? super V>> Expression<V> path( final Root<E> root ) {
        Path<?> path = root;
        String name = "";
        for ( final SingleAttribute a : attributes ) {
            name = a.name();
            path = path.get( name );
        }
        return path.getParentPath().get( name );
    }

    /**
     * Get the type of the last attribute in the path
     *
     * @param <V> Expected Value type
     * @return the value type
     */
    @SuppressWarnings( "unchecked" )
    public <V extends Comparable<? super V>> Class<V> type() {
        return (Class<V>) attributes.get( attributes.size() - 1 ).type();
    }
}
