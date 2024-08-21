package io.vigier.cursorpaging.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;

/**
 * Queries required by the cursor paging.
 *
 */
public interface QueryBuilder {

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
     * @param value     the value to filter on
     * @return the predicate to add to the query
     */
    Predicate isIn( Attribute attribute, Collection<? extends Comparable<?>> value );

    /**
     * Low level access to add custom filter rules
     *
     * @param predicate the predicate to add
     */
    void addWhere( final Predicate predicate );

    void addWhere( final List<Predicate> predicate );

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

    /**
     * Get an equal predicate for the given attribute and value
     *
     * @param attribute the attribute
     * @param value     the value to be compared
     * @return the predicate which can be added to the query
     */
    Predicate equalTo( Attribute attribute, Comparable<?> value );

    /**
     * Get a greater than predicate for the given attribute and value
     *
     * @param attribute the attribute
     * @param value     the value to be compared
     * @return the predicate which can be added to the query
     */
    Predicate greaterThan( Attribute attribute, Comparable<?> value );

    /**
     * Get a less than predicate for the given attribute and value
     *
     * @param attribute the attribute
     * @param value     the value to be compared
     * @return the predicate which can be added to the query
     */
    Predicate lessThan( final Attribute attribute, final Comparable<?> value );
}
