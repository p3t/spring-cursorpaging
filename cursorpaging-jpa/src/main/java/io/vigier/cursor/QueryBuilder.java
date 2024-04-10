package io.vigier.cursor;

import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Collection;

/**
 * Queries required by the cursor paging.
 *
 * @param <E> The entity type
 */
public interface QueryBuilder<E> {

    /**
     * In case of descending order the positions are less than the last one on the previous page.
     *
     * @param attribute the attribute to use for the query
     * @param value     the value to use for the query
     * @param <V>       the type of the attribute
     */
    <V extends Comparable<? super V>> void lessThan( SingularAttribute<E, V> attribute,
            V value );

    /**
     * In case of ascending order the positions are greater than on the previous page
     *
     * @param attribute the attribute to use for the query
     * @param value     the value to use for the query
     * @param <V>       the type of the attribute
     */
    <V extends Comparable<? super V>> void greaterThan( SingularAttribute<E, V> attribute,
            V value );

    /**
     * Provide the order for the position query
     *
     * @param attribute the attribute to order by
     * @param order the order to use
     * @param <V> the type of the attribute
     */
    <V extends Comparable<? super V>> void orderBy( SingularAttribute<E, V> attribute,
            Order order );

    /**
     * Used to filter out not matching entities
     *
     * @param attribute the attribute to filter on
     * @param value the value to filter on
     * @param <V> the type of the value
     */
    <V extends Comparable<? super V>> void isIn( SingularAttribute<E, V> attribute, Collection<?> value );

    /**
     * Used to filter out not matching entities
     *
     * @param attribute the attribute to filter on
     * @param value the value to filter on
     * @param <V> the type of the value
     */
    <V extends Comparable<? super V>> void isEqual( SingularAttribute<E, V> attribute, V value );
}
