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

    private final SingularAttribute<E, V> attribute;
    @With
    private final V value;

    public static <E, V extends Comparable<? super V>> Filter<E, V> create(
            final Consumer<FilterBuilder<E, V>> creator ) {
        final var builder = Filter.<E, V>builder();
        creator.accept( builder );
        return builder.build();
    }

    @SafeVarargs
    public static <E, V extends Comparable<? super V>> Filter<E, V> attributeIs( final SingularAttribute<E, V> name,
            final V... values ) {
        final FilterBuilder<E, V> builder = Filter.<E, V>builder().attribute( name );
        for ( final V v : values ) {
            builder.value( v );
        }
        return builder.build();
    }

    public void apply( final QueryBuilder<E> c ) {

        if ( attribute.isCollection() ) {
            // TODO: implement
        } else if ( value instanceof Collection<?> ) {
            c.isIn( attribute, (Collection<?>) value );
        } else {
            c.isEqual( attribute, value );
        }
    }
}
