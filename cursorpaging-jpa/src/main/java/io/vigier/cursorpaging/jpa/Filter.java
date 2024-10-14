package io.vigier.cursorpaging.jpa;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.With;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

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

    private enum Match {
        EQUAL, LIKE
    }

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

    @With
    @Builder.Default
    private final Match match = Match.EQUAL;

    public static class FilterBuilder {

        public FilterBuilder attribute( final SingularAttribute<?, ? extends Comparable<?>> attribute ) {
            this.attribute = Attribute.of( attribute );
            return this;
        }

        /**
         * Creates an attribute as path to an embedded entity's property.
         *
         * @param attributes the path to the property
         * @return the builder
         */
        @SafeVarargs
        public final FilterBuilder path( final SingularAttribute<?, ? extends Comparable<?>>... attributes ) {
            this.attribute = Attribute.path( attributes );
            return this;
        }

        public FilterBuilder attribute( final Attribute attribute ) {
            this.attribute = attribute;
            return this;
        }

        public FilterBuilder like( final Comparable<?>... values ) {
            match( Match.LIKE );
            this.values = new ArrayList<>( Arrays.asList( values ) );
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
     * Create a {@linkplain Filter} on an attribute matching the given value(s).
     *
     * @param attribute the attribute to filter on
     * @param values    the value(s) to filter for
     * @return a new {@linkplain Filter}
     */
    public static Filter attributeIs( final SingularAttribute<?, ? extends Comparable<?>> attribute,
            final Comparable<?>... values ) {
        return attributeIs( Attribute.of( attribute ), values );
    }

    /**
     * Create a {@linkplain Filter} on an attribute matching the given value(s).
     *
     * @param attribute the attribute to filter on
     * @param values    the value(s) to filter for
     * @return a new {@linkplain Filter}
     */
    public static Filter attributeIs( final SingularAttribute<?, ? extends Comparable<?>> attribute,
            final Collection<Comparable<?>> values ) {
        return attributeIs( Attribute.of( attribute ), values );
    }

    /**
     * Create a {@linkplain Filter} on an attribute matching the given value(s).
     *
     * @param attribute the attribute to filter on
     * @param values    the value(s) to filter for
     * @return a new {@linkplain Filter}
     */
    public static Filter attributeIs( final Attribute attribute, final Comparable<?>... values ) {
        return attributeIs( attribute, List.of( values ) );
    }

    /**
     * Create a {@linkplain Filter} on an attribute matching the given value(s).
     *
     * @param attribute the attribute to filter on
     * @param values    the value(s) to filter for
     * @return a new {@linkplain Filter}
     */
    public static Filter attributeIs( final Attribute attribute, final Collection<Comparable<?>> values ) {
        return Filter.create( b -> b.attribute( attribute ).values( values ) );
    }

    <T extends Comparable<? super T>> List<T> values( Class<T> valueType ) {
        return values.stream()
                .map( v -> valueType.isAssignableFrom( v.getClass() ) ? valueType.cast( v ) : null )
                .toList();
    }

    /**
     * Apply the filter to the query builder
     *
     * @param qb the query builder
     */
    public void apply( final QueryBuilder qb ) {
        switch ( match ) {
            case EQUAL -> applyEqual( qb );
            case LIKE -> applyLike( qb );
        }
    }

    private void applyLike( final QueryBuilder qb ) {
        final List<Predicate> predicates = values.stream()
                .filter( Objects::nonNull )
                .map( Object::toString )
                .filter( StringUtils::hasText )
                .map( v -> qb.isLike( attribute, v ) )
                .toList();
        if ( predicates.size() > 1 ) {
            qb.andWhere( qb.orOne( predicates ) );
        } else if ( predicates.size() == 1 ) {
            qb.andWhere( predicates.get( 0 ) );
        }
    }

    private void applyEqual( final QueryBuilder qb ) {
        final List<? extends Comparable<?>> filterValues = values.stream()
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
        if ( filterValues.size() > 1 ) {
            qb.andWhere( qb.isIn( attribute, filterValues ) );
        } else if ( filterValues.size() == 1 ) {
            qb.andWhere( qb.equalTo( attribute, filterValues.get( 0 ) ) );
        }
    }

    public boolean isEmpty() {
        return values.isEmpty() || values.stream()
                .allMatch( v -> Objects.isNull( v ) || (v instanceof final CharSequence cs && !StringUtils.hasText(
                        cs )) );
    }
}
