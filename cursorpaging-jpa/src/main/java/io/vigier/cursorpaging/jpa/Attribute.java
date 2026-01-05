package io.vigier.cursorpaging.jpa;

import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Attributes are used to describe a property of an entity which is used within
 * a {@linkplain Position}
 * <p>
 * The attribute uses a list of single attributes to address a certain value of
 * an entity. This "address" is later used
 * to generate queries.
 */
@Builder(toBuilder = true)
@RequiredArgsConstructor
@EqualsAndHashCode
@Slf4j
public class Attribute {

    private static final ConversionService CONVERSION_SERVICE = new DefaultConversionService();

    @Singular
    private final List<SingleAttribute> attributes;

    private final boolean ignoreCase;

    public Attribute(final List<SingleAttribute> attributes) {
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
    public static Attribute of(final String name, final Class<? extends Comparable<?>> type) {
        return new Attribute(List.of(SingleAttribute.of(name, type)));
    }

    /**
     * Creates a new attribute from a singular attribute.
     *
     * @param sa the property from the JPA metamodel
     * @return a new Attribute
     */
    public static Attribute of(final SingularAttribute<?, ? extends Comparable<?>> sa) {
        return new Attribute(List.of(SingleAttribute.of(sa)));
    }

    /**
     * Creates a new attribute as path of single attributes. This is needed to
     * address properties of embedded entities
     *
     * @param path Path pointing to an attribute (of an embedded entity)
     * @return The attribute describing the path and the type of the single
     *         attributes
     */
    public static Attribute of(final jakarta.persistence.metamodel.Attribute<?, ?>... path) {
        return new Attribute(Arrays.stream(path).map(SingleAttribute::of).toList());
    }

    /**
     * Creates a new attribute with as path of single attributes. This is needed to
     * address properties of embedded
     * entities
     *
     * @param path Path pointing to an attribute (of an embedded entity)
     * @return The attribute describing the path and the type of the single
     *         attributes
     */
    public static Attribute of(final SingleAttribute... path) {
        return new Attribute(Arrays.asList(path));
    }

    /**
     * Creates an attribute as path of 2 attributes
     *
     * @param name1 Attribute name 1
     * @param type1 Type of attribute 1
     * @param name2 Attribute name 2
     * @param type2 Type of attribute 2
     * @return The attribute describing the path and type to the specified attribute
     */
    public static Attribute of(final String name1, final Class<?> type1, final String name2, final Class<?> type2) {
        return new Attribute(List.of(SingleAttribute.of(name1, type1), SingleAttribute.of(name2, type2)));
    }

    /**
     * Creates an attribute as path of 3 attributes
     *
     * @param name1 Attribute name 1
     * @param type1 Type of attribute 1
     * @param name2 Attribute name 2
     * @param type2 Type of attribute 2
     * @param name3 Attribute name 3
     * @param type3 Type of attribute 3
     * @return The attribute describing the path and type to the specified attribute
     */
    public static Attribute of(final String name1, final Class<?> type1, final String name2, final Class<?> type2,
            final String name3, final Class<?> type3) {
        return new Attribute(List.of(SingleAttribute.of(name1, type1), SingleAttribute.of(name2, type2),
                SingleAttribute.of(name3, type3)));
    }

    @Nullable
    Comparable<?> valueOf(@Nullable final Object entity) {
        Object result = entity;
        for (final SingleAttribute a : attributes) {
            result = a.valueOf(result);
        }
        if (result == null || result instanceof Comparable<?>) {
            return (Comparable<?>) result;
        }
        throw new IllegalStateException("Attribute %s is not a comparable: %s".formatted(toString(), result));
    }

    public List<SingleAttribute> attributes() {
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
        return String.join(".", attributes.stream().map(SingleAttribute::name).toList());
    }

    /**
     * Generate a criteria expression/path using the given entity-root.
     *
     * @param root The root of the entity
     * @param <E>  Entity type
     * @param <V>  Value type
     * @return The path-expression to the attribute
     */
    public <E, V extends Comparable<? super V>> Expression<V> path(final Root<E> root) {
        if (attributes.size() == 1) {
            return root.get(attributes.getFirst().name());
        }
        Path<?> path = root;
        String name = "";
        final var attrIter = attributes.iterator();
        while (attrIter.hasNext()) {
            final var a = attrIter.next();
            name = a.name();
            // Intermediate attribute - create explicit join
            if (path instanceof Root || (path instanceof Join<?, ?> && attrIter.hasNext())) {
                path = ((From<?, ?>) path).join(name, JoinType.LEFT);
            } else {
                // Fallback to implicit join (might create inner join)
                path = path.get(name);
            }
        }
        return path.getParentPath().get(name);
    }

    /**
     * Get the type of the last attribute in the of
     *
     * @param <V> Expected Value type
     * @return the value type
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<? super V>> Class<V> type() {
        return (Class<V>) attributes.getLast().type();
    }

    /**
     * Instructs equal, like and in-operations to ignore the case of the value. Is
     * ignored for not-character attributes
     * and for greater/lower than operations!
     *
     * @param ignoreCase instruction to ignore character case for comparison
     *                   operations.
     * @return New attribute with flag set.
     */
    Attribute ignoreCase(final boolean ignoreCase) {
        return toBuilder().ignoreCase(ignoreCase)
                .build();
    }

    /**
     * Enables the ignore case flag for this attribute.
     *
     * @return New attribute with flag set to true.
     * @see #ignoreCase(boolean)
     */
    public Attribute withIgnoreCase() {
        return ignoreCase(true);
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

    public Comparable<?> verify(final Comparable<?> value) {
        if (value == null || value.getClass() == type()) {
            return value;
        }
        if (CONVERSION_SERVICE.canConvert(value.getClass(), type())) {
            return (Comparable<?>) CONVERSION_SERVICE.convert(value, TypeDescriptor.valueOf(type()));
        }
        throw new IllegalArgumentException(
                "Value %s (%s) is not of type %s".formatted(value, value.getClass().getName(), type()));
    }

    public List<? extends Comparable<?>> verify(final List<? extends Comparable<?>> values) {
        return values != null ? values.stream().map(this::verify).toList() : List.of();
    }

    public Comparable<?>[] verify(final Comparable<?>... values) {
        return values != null ? Arrays.stream(values).map(this::verify).toArray(Comparable<?>[]::new) : null;
    }
}
