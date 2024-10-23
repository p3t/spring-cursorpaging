package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.filter.FilterBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

/**
 * A filter, which can be used to filter a query (remove elements from the result).
 * <p>
 * Currently only simple attributes can be filtered (no collections, or nested properties). If the provided filter value
 * is a collection a one-must-match ("attribute in my-filter-values") logic applies.
 */
@Getter
@Accessors( fluent = true )
@EqualsAndHashCode
public abstract class Filter implements QueryElement {

    /**
     * The attribute to filter on.
     */
    private final Attribute attribute;

    /**
     * The value to filter on.
     */
    @Singular
    private final List<? extends Comparable<?>> values;

    /**
     * Get a new {@linkplain FilterBuilder}
     *
     * @return the builder
     */
    public static FilterBuilder builder() {
        return new FilterBuilder();
    }

    /**
     * Creator method to build a new Filter.
     * @param c the consumer for the builder
     * @return a new Filter instance
     */
    public static Filter create( final Consumer<FilterBuilder> c ) {
        FilterBuilder builder = builder();
        c.accept( builder );
        return builder.build();
    }

    /**
     * Constructs a Filter for the attribute and values.
     * @param attribute Attribute (must not be `null`)
     * @param values the values used by the filter, must not be `null`, but can be empty or contain `null` or empty strings (which will be ignored)
     */
    protected Filter( final Attribute attribute, final List<? extends Comparable<?>> values ) {
        this.attribute = attribute;
        this.values = values.stream()
                .map( v -> v instanceof final CharSequence cs && !StringUtils.hasText( cs ) ? null : v )
                .filter( Objects::nonNull )
                .toList();
    }

    <T extends Comparable<? super T>> List<T> values( Class<T> valueType ) {
        return values.stream()
                .map( v -> valueType.isAssignableFrom( v.getClass() ) ? valueType.cast( v ) : null )
                .toList();
    }

    @Override
    public List<Attribute> attributes() {
        return List.of( attribute );
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Apply the filter to the query builder
     *
     * @param qb the query builder
     */
    public Predicate toPredicate( final QueryBuilder qb ) {
        if ( !values.isEmpty() ) {
            return toFilterPredicate( qb, values );
        }
        return qb.cb().and();
    }

    protected abstract Predicate toFilterPredicate( final QueryBuilder qb, List<? extends Comparable<?>> cleanValues );
}
