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
        public Filter equalTo( Comparable<?> value ) {
            return Filter.create( f -> f.attribute( attribute ).equalTo( value ) );
        }

        /**
         * equal/in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter equalTo( List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).equalTo( values ) );
        }

        /**
         * in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter in( List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).in( values ) );
        }

        /**
         * in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter in( Comparable<?>... values ) {
            return Filter.create( f -> f.attribute( attribute ).in( values ) );
        }

        /**
         * like-in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter like( String... values ) {
            return Filter.create( f -> f.attribute( attribute ).like( values ) );
        }

        /**
         * like-in expression
         *
         * @param values the value to compare to
         * @return the filter for the operation
         */
        public Filter like( List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).like( values ) );
        }

        /**
         * Filter to select all values greater-than the given one.
         *
         * @param value The value to compare to
         * @return A filter for the operation
         */
        public Filter greaterThan( Comparable<?> value ) {
            return Filter.create( f -> f.attribute( attribute ).greaterThan( value ) );
        }

        /**
         * Greater than with multiple values means greaterThan max of all values
         *
         * @param values values to compare
         * @return the filter
         */
        public Filter greaterThan( final List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).greaterThan( values ) );
        }

        /**
         * Filter to select all values less-than the given one.
         *
         * @param value The value to compare to
         * @return A filter for the operation
         */
        public Filter lessThan( Comparable<?> value ) {
            return Filter.create( f -> f.attribute( attribute ).lessThan( value ) );
        }

        /**
         * Less than with multiple values means lessThan the minimum of all values
         *
         * @param values values to compare
         * @return A filter for the operation
         */
        public Filter lessThan( List<? extends Comparable<?>> values ) {
            return Filter.create( f -> f.attribute( attribute ).lessThan( values ) );
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
    public static FilterCreator attribute( final String name, Class<? extends Comparable<?>> type ) {
        return FilterCreator.create( Attribute.of( name, type ) );
    }

    /**
     * Starts filter-creation with a path of the provided attributes
     *
     * @param name1 of the first attribute in the path
     * @param type1 of the first attribute in the path
     * @param name2 of the second attribute in the path
     * @param type2 of the second attribute in the path
     * @return the filter creator for the operation
     */
    public static FilterCreator attribute( final String name1, Class<? extends Comparable<?>> type1, String name2,
            Class<? extends Comparable<?>> type2 ) {
        return FilterCreator.create( Attribute.path( name1, type1, name2, type2 ) );
    }

    /**
     * Starts filter-creation with a path of the provided attributes
     */
    public static FilterCreator attribute( final jakarta.persistence.metamodel.Attribute<?, ?>... path ) {
        return attribute( Attribute.path( path ) );
    }

    /**
     * Starts filter-creation with a path of the provided attributes
     */
    public static FilterCreator attribute( final SingularAttribute<?, ? extends Comparable<?>> attribute ) {
        return attribute( Attribute.of( attribute ) );
    }

    /**
     * Starts filter-creation with a path of the provided attributes, ignoring the case in the subsequent operations.
     */
    public static FilterCreator ignoreCase( final Attribute attribute ) {
        return FilterCreator.create( attribute.withIgnoreCase() );
    }

    /**
     * Starts filter-creation with a path of the provided attributes, ignoring the case in the subsequent operations.
     */
    public static FilterCreator ignoreCase( final jakarta.persistence.metamodel.Attribute<?, ?>... path ) {
        return ignoreCase( Attribute.path( path ) );
    }

    /**
     * Starts filter-creation with a path of the provided attributes, ignoring the case in the subsequent operations.
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
