package io.vigier.cursorpaging.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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

    /**
     * Low level access to add custom filter rules
     *
     * @param predicate
     */
    void addWhere( final Predicate predicate );

    /**
     * Low level access to the query for the root-entity
     *
     * @param <R> the return type
     * @return the query
     */
    <R> CriteriaQuery<R> query();

    /**
     * Low level access to the criteria builder
     *
     * @return the criteria builder
     */
    CriteriaBuilder cb();

    /**
     * Low level access to the root
     *
     * @param <E> the entity type
     * @return the root
     */
    <E> Root<E> root();

    /**
     * Low level access to the entity manager
     *
     * @return the entity manager
     */
    EntityManager entityManager();
}
