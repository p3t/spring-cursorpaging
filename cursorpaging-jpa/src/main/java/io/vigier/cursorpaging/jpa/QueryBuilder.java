package io.vigier.cursorpaging.jpa;

import java.util.Collection;

/**
 * Queries required by the cursor paging.
 *
 */
public interface QueryBuilder {

    /**
     * In case of descending order the positions are less than the last one on the previous page.
     *
     * @param attribute the attribute to use for the query
     * @param value     the value to use for the query
     */
    void lessThan( Attribute attribute, Comparable<?> value );

    /**
     * In case of ascending order the positions are greater than on the previous page
     *
     * @param attribute the attribute to use for the query
     * @param value     the value to use for the query
     */
    void greaterThan( Attribute attribute, Comparable<?> value );

    /**
     * Provide the order for the position query
     *
     * @param attribute the attribute to order by
     * @param order the order to use
     */
    void orderBy( Attribute attribute, Order order );

    /**
     * Used to filter out not matching entities
     *
     * @param attribute the attribute to filter on
     * @param value the value to filter on
     */
    void isIn( Attribute attribute, Collection<? extends Comparable<?>> value );

    /**
     * Used to filter out not matching entities
     *
     * @param attribute the attribute to filter on
     * @param value the value to filter on
     */
    void isEqual( Attribute attribute, Comparable<?> value );
}
