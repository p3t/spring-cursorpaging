package io.vigier.cursor;

import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

/**
 * A filter, which can be used to filter a query (remove elements from the result).
 * <p>
 * Currently only simple attributes can be filtered (no collections, or nested properties). If the provided filter value
 * is a collection a one-must-match ("attribute in my-filter-values") logic applies.
 *
 * @param <E> the entity type
 * @param <V> the type of the attribute-value
 */
@Builder( toBuilder = true )
@Getter
@Accessors( fluent = true )
public class Filter<E, V extends Comparable<? super V>> {

    /**
     * The attribute to filter on.
     */
    private final SingularAttribute<E, V> attribute;

    /**
     * The value to filter on.
     */
    @With
    private final V value;

    /**
     * Create a {@linkplain Filter} with a builder.
     *
     * @param creator the customizer for the builder
     * @param <E>     the entity type
     * @param <V>     the value/attribute type
     * @return a new {@linkplain Filter}
     */
    public static <E, V extends Comparable<? super V>> Filter<E, V> create(
            final Consumer<FilterBuilder<E, V>> creator ) {
        final var builder = Filter.<E, V>builder();
        creator.accept( builder );
        return builder.build();
    }

    /**
     * Create a {@linkplain Filter} with the given attribute and value (s).
     *
     * @param attribute the attribute to filter on
     * @param values    the value(s) to filter on
     * @param <E>       the entity type
     * @param <V>       the value/attribute type
     * @return a new {@linkplain Filter}
     */
    @SafeVarargs
    public static <E, V extends Comparable<? super V>> Filter<E, V> attributeIs(
            final SingularAttribute<E, V> attribute,
            final V... values ) {
        final FilterBuilder<E, V> builder = Filter.<E, V>builder().attribute( attribute );
        for ( final V v : values ) {
            builder.value( v );
        }
        return builder.build();
    }

    /**
     * Apply the filter to the query builder
     *
     * @param qb the query builder
     */
    public void apply( final QueryBuilder<E> qb ) {

        if ( attribute.isCollection() ) {
            // TODO: implement
        } else if ( value instanceof Collection<?> ) {
            qb.isIn( attribute, (Collection<?>) value );
        } else {
            qb.isEqual( attribute, value );
        }
    }
}
