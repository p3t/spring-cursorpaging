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
@Builder( toBuilder = true )
@RequiredArgsConstructor
public class CriteriaQueryBuilder<E, R> implements QueryBuilder {

    public enum AppendMode {
        AND, OR
    }

    private final CriteriaQuery<R> query;
    private final CriteriaBuilder cb;
    private final Root<E> root;
    private final Class<E> entityType;
    private final EntityManager entityManager;
    @Builder.Default
    private final AppendMode appendMode = AppendMode.AND;

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

    public CriteriaQueryBuilder<E, R> orCondition() {
        return toBuilder().appendMode( AppendMode.OR )
                .build();
    }

    @Override
    public Predicate lessThan( final Attribute attribute, final Comparable<?> value ) {
        return createLessThan( attribute, attribute.type().cast( value ) );
    }

    private <V extends Comparable<? super V>> Predicate createLessThan( final Attribute attribute, final V value ) {
        return cb.lessThan( attribute.path( root ), value );
    }

    @Override
    public Predicate greaterThan( final Attribute attribute, final Comparable<?> value ) {
        return createGreaterThan( attribute, attribute.type().cast( value ) );
    }

    private <V extends Comparable<? super V>> Predicate createGreaterThan( final Attribute attribute, final V value ) {
        return cb().greaterThan( attribute.path( root ), value );
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
    public Predicate isIn( final Attribute attribute, final Collection<? extends Comparable<?>> value ) {
        return attribute.path( root ).in( value );
    }

    @Override
    public Predicate equalTo( final Attribute attribute, final Comparable<?> value ) {
        return cb.equal( attribute.path( root ), value );
    }

    @Override
    public void addWhere( final List<Predicate> conditions ) {
        final var restriction = query.getRestriction();
        if ( restriction == null ) {
            query.where( conditions.toArray( new Predicate[0] ) );
        } else {
            switch ( appendMode ) {
                case AND -> query.where( cb.and( conditions.toArray( new Predicate[0] ) ) );
                case OR -> query.where( cb.or( restriction, cb.and( conditions.toArray( new Predicate[0] ) ) ) );
            }
        }
    }

    @Override
    public void addWhere( final Predicate predicate ) {
        addWhere( List.of( predicate ) );
    }
}
