package io.vigier.cursorpaging.jpa.impl;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Wrapper of CriteriaQuery, CriteriaBuilder, Root and EntityType, and also adds some methods to build the
 * position-queries
 *
 * @param <E>        EntityType
 * @param <R>        ResultType
 */
@Getter
@Accessors( fluent = true )
@Builder
@RequiredArgsConstructor
public class CriteriaQueryBuilder<E, R> implements QueryBuilder {

    private final CriteriaQuery<R> query;
    private final CriteriaBuilder cb;
    private final Root<E> root;
    private final Class<E> entityType;
    private final EntityManager entityManager;

    public static <T> CriteriaQueryBuilder<T, T> forEntity( final Class<T> entityType,
            final EntityManager entityManager ) {
        final var cb = entityManager.getCriteriaBuilder();
        final var query = cb.createQuery( entityType );
        final var root = query.from( entityType );
        query.select( root );
        return CriteriaQueryBuilder.<T, T>builder()
                .query( query )
                .cb( cb )
                .root( root )
                .entityType( entityType )
                .entityManager( entityManager )
                .build();
    }

    public static <E> CriteriaQueryBuilder<E, Long> forCount( final Class<E> entityType,
            final EntityManager entityManager ) {
        final var cb = entityManager.getCriteriaBuilder();
        final var query = cb.createQuery( Long.class );
        final var root = query.from( entityType );
        query.select( cb.count( root ) );
        return CriteriaQueryBuilder.<E, Long>builder()
                .query( query )
                .cb( cb )
                .root( root )
                .entityType( entityType )
                .entityManager( entityManager )
                .build();
    }

    @Override
    public void lessThan( final Attribute attribute, final Comparable<?> value ) {
        whereLessThan( attribute, attribute.type().cast( value ) );
    }

    private <V extends Comparable<? super V>> void whereLessThan( final Attribute attribute, final V value ) {
        addWhere( cb().lessThan( attribute.path( root ), value ) );
    }

    @Override
    public void greaterThan( final Attribute attribute, final Comparable<?> value ) {
        whereGreaterThan( attribute, attribute.type().cast( value ) );
    }

    private <V extends Comparable<? super V>> void whereGreaterThan( final Attribute attribute, final V value ) {
        addWhere( cb().greaterThan( attribute.path( root ), value ) );
    }

    @Override
    public void orderBy( final Attribute attribute, final Order order ) {
        final List<jakarta.persistence.criteria.Order> orderSpecs = new LinkedList<>( query().getOrderList() );
        orderSpecs.add( switch ( order ) {
            case ASC -> cb().asc( attribute.path( root ) );
            case DESC -> cb().desc( attribute.path( root ) );
        } );
        query().orderBy( orderSpecs );
    }

    @Override
    public void isIn( final Attribute attribute, final Collection<? extends Comparable<?>> value ) {
        addWhere( attribute.path( root ).in( value ) );
    }

    @Override
    public void isEqual( final Attribute attribute, final Comparable<?> value ) {
        addWhere( cb().equal( attribute.path( root ), value ) );
    }

    @Override
    public void addWhere( final Predicate predicate ) {
        final var restriction = query().getRestriction();
        if ( restriction == null ) {
            query().where( predicate );
        } else {
            query().where( restriction, predicate );
        }
    }
}
