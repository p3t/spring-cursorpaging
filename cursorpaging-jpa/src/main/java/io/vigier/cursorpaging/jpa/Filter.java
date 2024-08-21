package io.vigier.cursorpaging.jpa;

import jakarta.persistence.metamodel.SingularAttribute;
import java.util.List;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.With;
import lombok.experimental.Accessors;

/**
 * A filter, which can be used to filter a query (remove elements from the result).
 * <p>
 * Currently only simple attributes can be filtered (no collections, or nested properties). If the provided filter value
 * is a collection a one-must-match ("attribute in my-filter-values") logic applies.
 */
@Builder( toBuilder = true )
@Getter
@Accessors( fluent = true )
@EqualsAndHashCode
public class Filter {

    /**
     * The attribute to filter on.
     */
    private final Attribute attribute;

    /**
     * The value to filter on.
     */
    @With
    @Singular
    private final List<? extends Comparable<?>> values;

    public static class FilterBuilder {

        public FilterBuilder attribute( final SingularAttribute<?, ? extends Comparable<?>> attribute ) {
            this.attribute = Attribute.of( attribute );
            return this;
        }

        public FilterBuilder attribute( final Attribute attribute ) {
            this.attribute = attribute;
            return this;
        }
    }
    /**
     * Create a {@linkplain Filter} with a builder.
     *
     * @param creator the customizer for the builder
     * @return a new {@linkplain Filter}
     */
    public static Filter create( final Consumer<FilterBuilder> creator ) {
        final var builder = Filter.builder();
        creator.accept( builder );
        return builder.build();
    }

    /**
     * Create a {@linkplain Filter} with the given attribute and value (s).
     *
     * @param attribute the attribute to filter on
     * @param values    the value(s) to filter on
     * @return a new {@linkplain Filter}
     */
    public static Filter attributeIs( final SingularAttribute<?, ? extends Comparable<?>> attribute,
            final Comparable<?>... values ) {
        final Attribute attr = Attribute.of( attribute );
        final FilterBuilder builder = Filter.builder().attribute( attr );
        for ( final Comparable<?> v : values ) {
            builder.value( v );
        }
        return builder.build();
    }

    /**
     * Apply the filter to the query builder
     *
     * @param qb the query builder
     */
    public void apply( final QueryBuilder qb ) {
        if ( values.size() > 1 ) {
            qb.addWhere( qb.isIn( attribute, values ) );
        } else {
            qb.addWhere( qb.equalTo( attribute, values.get( 0 ) ) );
        }
    }
}
