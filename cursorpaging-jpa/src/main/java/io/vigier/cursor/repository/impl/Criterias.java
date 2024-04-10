package io.vigier.cursor.repository.impl;

import io.vigier.cursor.Order;
import io.vigier.cursor.QueryBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
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
        Class<E> entityType ) implements QueryBuilder<E> {

    public static <T> Criterias<T> selectRoot( final Class<T> entityType, final EntityManager entityManager ) {
        final var builder = entityManager.getCriteriaBuilder();
        final var query = builder.createQuery( entityType );
        final var root = query.from( entityType );
        query.select( root );
        return new Criterias<>( query, builder, root, entityType );
    }

    @Override
    public <V extends Comparable<? super V>> void lessThan( final SingularAttribute<E, V> attribute,
            final V value ) {
        addWhere( builder().lessThan( path( attribute ), value ) );
    }

    @Override
    public <V extends Comparable<? super V>> void greaterThan( final SingularAttribute<E, V> attribute,
            final V value ) {
        addWhere( builder().greaterThan( path( attribute ), value ) );
    }

    @Override
    public <V extends Comparable<? super V>> void orderBy( final SingularAttribute<E, V> attribute,
            final Order order ) {
        final List<jakarta.persistence.criteria.Order> orderSpecs = new LinkedList<>( query().getOrderList() );
        orderSpecs.add( switch ( order ) {
            case ASC -> builder().asc( root().get( attribute.getName() ) );
            case DESC -> builder().desc( root().get( attribute.getName() ) );
        } );
        query().orderBy( orderSpecs );
    }

    @Override
    public <V extends Comparable<? super V>> void isIn( final SingularAttribute<E, V> attribute,
            final Collection<?> value ) {
        addWhere( path( attribute ).in( value ) );
    }

    @Override
    public <V extends Comparable<? super V>> void isEqual( final SingularAttribute<E, V> attribute, final V value ) {
        addWhere( builder().equal( path( attribute ), value ) );
    }

    private <V extends Comparable<? super V>> Expression<V> path( final SingularAttribute<E, V> attribute ) {
        return this.root().get( attribute.getName() ).as( attribute.getJavaType() );
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
