package io.vigier.cursorpaging.jpa;


import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Singular;


/**
 * Attributes are used to describe a property of an entity which is used within a {@linkplain Position}
 * <p>
 * The attribute uses a list of single attributes to address a certain value of an entity. This "address" is later used
 * to generate queries.
 */
@Builder( toBuilder = true )
@RequiredArgsConstructor
@EqualsAndHashCode
public class Attribute {

    @Singular
    private final List<SingleAttribute> attributes;

    private final boolean ignoreCase;

    public Attribute( List<SingleAttribute> attributes ) {
        this.attributes = attributes;
        this.ignoreCase = false;
    }

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

    List<SingleAttribute> attributes() {
        return attributes;
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

    /**
     * Instructs equal, like and in-operations to ignore the case of the value. Is ignored for not-character attributes
     * and for greater/lower than operations!
     *
     * @param ignoreCase instruction to ignore character case for comparison operations.
     * @return New attribute with flag set.
     */
    Attribute ignoreCase( boolean ignoreCase ) {
        return toBuilder().ignoreCase( ignoreCase )
                .build();
    }

    /**
     * Enables the ignore case flag for this attribute.
     *
     * @return New attribute with flag set to true.
     * @see #ignoreCase(boolean)
     */
    public Attribute withIgnoreCase() {
        return ignoreCase( true );
    }

    /**
     * Get the state of the ignore case flag.
     *
     * @return true when comparison should be case-insensitive
     * @see #ignoreCase(boolean)
     */
    public boolean ignoreCase() {
        return ignoreCase;
    }

    public Comparable<?> verify( Comparable<?> v ) {
        if ( type().isAssignableFrom( v.getClass() ) ) {
            return type().cast( v );
        } else if ( v instanceof Integer && type().isAssignableFrom( Long.class ) ) {
            return Long.valueOf( (Integer) v );
        } else if ( v instanceof Long && type().isAssignableFrom( Integer.class ) && (Long) v <= Integer.MAX_VALUE ) {
            return Integer.valueOf( v.toString() );
        } else {
            throw new IllegalArgumentException(
                    "Value %s (%s) is not of type %s".formatted( v, v.getClass().getName(), type() ) );
        }
    }

    public List<? extends Comparable<?>> verify( List<? extends Comparable<?>> values ) {
        return values.stream().map( this::verify ).toList();
    }

    public Comparable<?>[] verify( Comparable<?>... values ) {
        return Arrays.stream( values ).map( this::verify ).toArray( Comparable<?>[]::new );
    }
}
