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
     * Used to filter out not matching entities
     *
     * @param attribute the attribute to filter on
     * @param value     the value to filter on
     * @return the created predicate
     */
    Predicate isIn( Attribute attribute, Collection<? extends Comparable<?>> value );

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
     * @return the created predicate
     */
    Predicate equalTo( Attribute attribute, Comparable<?> value );

    /**
     * Get a is like predicate
     *
     * @param attribute the attribute
     * @param value     the value to be used as char-sequence
     * @return the created predicate
     */
    Predicate isLike( final Attribute attribute, final String value );

    /**
     * Get a greater than predicate for the given attribute and value
     *
     * @param attribute the attribute
     * @param value     the value to be compared
     * @return the created predicate
     */
    Predicate greaterThan( Attribute attribute, Comparable<?> value );

    /**
     * Get a greater than or equal to predicate for the given attribute and value
     *
     * @param attribute the attribute
     * @param value     the value to be compared
     * @return the created predicate
     */
    Predicate greaterThanOrEqualTo( final Attribute attribute, final Comparable<?> value );

    /**
     * Get a less than predicate for the given attribute and value
     *
     * @param attribute the attribute
     * @param value     the value to be compared
     * @return the created predicate
     */
    Predicate lessThan( final Attribute attribute, final Comparable<?> value );

    /**
     * Get a less than or equal to predicate for the given attribute and value
     *
     * @param attribute the attribute
     * @param value     the value to be compared
     * @return the created predicate
     */
    Predicate lessThanOrEqualTo( final Attribute attribute, final Comparable<?> value );

    /**
     * Low level access to add custom filter rules.
     *
     * @param conditions the predicates to be added to the query with an AND semantic
     */
    void andWhere( List<Predicate> conditions );

    /**
     * Low level access to add custom filter rules. This method modifies the query of the builder!
     *
     * @param conditions the predicates to be added to the query with an OR semantic
     */
    void orWhere( List<Predicate> conditions );

    /**
     * Low level access to add custom filter rules. This method modifies the query of the builder!
     *
     * @param predicate the predicate to be added to the query
     */
    void andWhere( final Predicate predicate );

    /**
     * Add an order by clause to the query
     *
     * @param attribute the attribute to order by
     * @param order     the order (ASC or DESC)
     */
    void orderBy( Attribute attribute, Order order );
}
