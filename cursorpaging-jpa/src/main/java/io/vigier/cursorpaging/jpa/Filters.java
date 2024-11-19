package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.filter.AndFilter;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.List;
import lombok.RequiredArgsConstructor;

public final class Filters {

    @RequiredArgsConstructor( staticName = "create" )
    public static class FilterCreator {

        private final Attribute attribute;

        /**
         * equal expression
         *
         * @param value the value to compare to
         * @return the filter for the operation
         */
        public Filter equalTo( final Comparable<?> value ) {
            return Filter.create( f -> f.attribute( attribute ).equalTo( attribute.verify( value ) ) );
        }

        /**
         * equal/in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter equalTo( final List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).equalTo( attribute.verify( values ) ) );
        }

        /**
         * in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter in( final List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).in( attribute.verify( values ) ) );
        }

        /**
         * in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter in( final Comparable<?>... values ) {
            return Filter.create( f -> f.attribute( attribute ).in( attribute.verify( values ) ) );
        }

        /**
         * like-in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter like( final String... values ) {
            return Filter.create( f -> f.attribute( attribute ).like( attribute.verify( values ) ) );
        }

        /**
         * like-in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter like( final List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).like( attribute.verify( values ) ) );
        }

        /**
         * Filter to select all values greater-than the given one.
         *
         * @param value The value to compare to
         * @return A filter for the operation
         */
        public Filter greaterThan( final Comparable<?> value ) {
            return Filter.create( f -> f.attribute( attribute ).greaterThan( attribute.verify( value ) ) );
        }

        /**
         * Greater than with multiple values means greaterThan max of all values
         *
         * @param values values to compare
         * @return the filter
         */
        public Filter greaterThan( final List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).greaterThan( attribute.verify( values ) ) );
        }

        /**
         * Filter to select all values less-than the given one.
         *
         * @param value The value to compare to
         * @return A filter for the operation
         */
        public Filter lessThan( final Comparable<?> value ) {
            return Filter.create( f -> f.attribute( attribute ).lessThan( attribute.verify( value ) ) );
        }

        /**
         * Less than with multiple values means lessThan the minimum of all values
         *
         * @param values values to compare
         * @return A filter for the operation
         */
        public Filter lessThan( final List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).lessThan( attribute.verify( values ) ) );
        }
    }

    private Filters() {
    }

    /**
     * Starts a filter-creation with the provided attribute
     *
     * @param attribute to be filtered on
     * @return the filter creator for the operation
     */
    public static FilterCreator attribute( final Attribute attribute ) {
        return FilterCreator.create( attribute );
    }

    /**
     * Starts a filter-creation with the provided attribute
     *
     * @param name of the attribute to be filtered on
     * @param type of the attribute to be filtered on
     * @return the filter creator for the operation
     */
    public static FilterCreator attribute( final String name, final Class<? extends Comparable<?>> type ) {
        return FilterCreator.create( Attribute.of( name, type ) );
    }

    /**
     * Starts filter-creation with a of of the provided attributes
     *
     * @param name1 of the first attribute in the of
     * @param type1 of the first attribute in the of
     * @param name2 of the second attribute in the of
     * @param type2 of the second attribute in the of
     * @return the filter creator for the operation
     */
    public static FilterCreator attribute( final String name1, final Class<? extends Comparable<?>> type1,
            final String name2, final Class<? extends Comparable<?>> type2 ) {
        return FilterCreator.create( Attribute.of( name1, type1, name2, type2 ) );
    }

    /**
     * Starts filter-creation with a of of the provided attributes
     */
    public static FilterCreator attribute( final jakarta.persistence.metamodel.Attribute<?, ?>... path ) {
        return attribute( Attribute.of( path ) );
    }

    /**
     * Starts filter-creation with a of of the provided attributes
     */
    public static FilterCreator attribute( final SingularAttribute<?, ? extends Comparable<?>> attribute ) {
        return attribute( Attribute.of( attribute ) );
    }

    /**
     * Starts filter-creation with a of of the provided attributes, ignoring the case in the subsequent operations.
     */
    public static FilterCreator ignoreCase( final Attribute attribute ) {
        return FilterCreator.create( attribute.withIgnoreCase() );
    }

    /**
     * Starts filter-creation with a of of the provided attributes, ignoring the case in the subsequent operations.
     */
    public static FilterCreator ignoreCase( final jakarta.persistence.metamodel.Attribute<?, ?>... path ) {
        return ignoreCase( Attribute.of( path ) );
    }

    /**
     * Starts filter-creation with a of of the provided attributes, ignoring the case in the subsequent operations.
     */
    public static FilterCreator ignoreCase( final SingularAttribute<?, ? extends Comparable<?>> attribute ) {
        return ignoreCase( Attribute.of( attribute ) );
    }

    /**
     * Create in filter combining the given filters by 'or'
     *
     * @param filters the filters to combine
     * @return the combined filter
     */
    public static OrFilter or( final QueryElement... filters ) {
        return OrFilter.of( filters );
    }

    /**
     * Create in filter combining the given filters by 'or'
     *
     * @param filters the filters to combine
     * @return the combined filter
     */
    public static OrFilter or( final List<QueryElement> filters ) {
        return OrFilter.of( filters );
    }

    /**
     * Create in filter combining the given filters by 'and'
     *
     * @param filters the filters to combine
     * @return the combined filter
     */
    public static AndFilter and( final QueryElement... filters ) {
        return AndFilter.of( filters );
    }

    /**
     * Create in filter combining the given filters by 'and'
     *
     * @param filters the filters to combine
     * @return the combined filter
     */
    public static AndFilter and( final List<QueryElement> filters ) {
        return AndFilter.of( filters );
    }
}
