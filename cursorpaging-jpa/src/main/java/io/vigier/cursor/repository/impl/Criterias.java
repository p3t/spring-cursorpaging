package io.vigier.cursor.repository.impl;

import io.vigier.cursor.Attribute;
import io.vigier.cursor.Order;
import io.vigier.cursor.QueryBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Wrapper of CriteriaQuery, CriteriaBuilder, Root and EntityType, and also adds some methods to build the
 * position-queries
 *
 * @param query
 * @param builder
 * @param root
 * @param entityType
 * @param <E>        EntityType
 */
record Criterias<E>(
        CriteriaQuery<E> query,
        CriteriaBuilder builder,
        Root<E> root,
        Class<E> entityType ) implements QueryBuilder {

    public static <T> Criterias<T> selectRoot( final Class<T> entityType, final EntityManager entityManager ) {
        final var builder = entityManager.getCriteriaBuilder();
        final var query = builder.createQuery( entityType );
        final var root = query.from( entityType );
        query.select( root );
        return new Criterias<>( query, builder, root, entityType );
    }

    @Override
    public void lessThan( final Attribute attribute,
            final Comparable<?> value ) {
        lessThan2( attribute, value );
    }

    private <V extends Comparable<? super V>> void lessThan2( final Attribute attribute, final Comparable<?> value ) {
        addWhere( builder().lessThan( path( attribute ), (V) value ) );
    }

    @Override
    public void greaterThan( final Attribute attribute, final Comparable<?> value ) {
        greaterThan2( attribute, value );
    }

    public <V extends Comparable<? super V>> void greaterThan2( final Attribute attribute, final Comparable<?> value ) {
        addWhere( builder().greaterThan( path( attribute ), (V) value ) );
    }

    @Override
    public void orderBy( final Attribute attribute, final Order order ) {
        final List<jakarta.persistence.criteria.Order> orderSpecs = new LinkedList<>( query().getOrderList() );
        orderSpecs.add( switch ( order ) {
            case ASC -> builder().asc( root().get( attribute.name() ) );
            case DESC -> builder().desc( root().get( attribute.name() ) );
        } );
        query().orderBy( orderSpecs );
    }

    @Override
    public void isIn( final Attribute attribute, final Collection<? extends Comparable<?>> value ) {
        addWhere( path( attribute ).in( value ) );
    }

    @Override
    public void isEqual( final Attribute attribute, final Comparable<?> value ) {
        addWhere( builder().equal( path( attribute ), value ) );
    }

    private <V extends Comparable<? super V>> Expression<V> path( final Attribute attribute ) {
        return (Expression<V>) this.root().get( attribute.name() ).as( attribute.type() );
    }

    private <V extends Comparable<? super V>> V value( final Comparable<?> value ) {
        return (V) value;
    }

    private void addWhere( final Predicate predicate ) {
        final var restriction = query().getRestriction();
        if ( restriction == null ) {
            query().where( predicate );
        } else {
            query().where( restriction, predicate );
        }
    }
}
