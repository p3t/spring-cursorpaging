package io.vigier.cursorpaging.jpa;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * A position, which can be used to address the start of a page.
 * <p>
 * The position uses an attribute and a value to address the start of a page. The order defines if the results should be
 * queried in ascending or descending order.
 */
@Builder( toBuilder = true )
@Getter
@Accessors( fluent = true )
@EqualsAndHashCode
@ToString
@Slf4j
public class Position {

    /**
     * Attribute used to create a position for an entity
     */
    private final Attribute attribute;

    /**
     * The current position from where on the results should be queried
     */
    private final Comparable<?> value;

    /**
     * The order in which the results should be queried
     */
    @Builder.Default
    private final Order order = Order.ASC;

    private boolean reversed;

    public static class PositionBuilder {

        /**
         * Builds a new position, verifies that at least one attribute has been provided
         *
         * @return a new position
         */
        public Position build() {
            if ( attribute == null ) {
                throw new IllegalStateException( "Attribute must not be null" );
            } else if ( attribute.attributes().isEmpty() ) {
                throw new IllegalStateException( "Attribute must not be empty" );
            } else {
                attribute.attributes().forEach( a -> {
                    if ( a.name() == null || a.name().isBlank() ) {
                        throw new IllegalStateException( "Attribute name must not be null or empty" );
                    } else if ( a.type() == null ) {
                        throw new IllegalStateException( "Attribute type must not be null" );
                    }
                } );
            }
            return new Position( attribute, value, order$value, reversed );
        }
    }

    /**
     * Creates a new {@link Position} with a builder.
     *
     * @param creator the customizer for the builder
     * @return a new {@link Position}
     */
    public static Position create( final Consumer<PositionBuilder> creator ) {
        final var builder = Position.builder();
        creator.accept( builder );
        return builder.build();
    }

    /**
     * Checks if this is the position at the start of the first page.
     *
     * @return true if the position is at the start of the first page.
     */
    public boolean isFirst() {
        return value == null;
    }

    private Order direction() {
        return switch ( order ) {
            case ASC -> reversed ? Order.DESC : Order.ASC;
            case DESC -> reversed ? Order.ASC : Order.DESC;
        };
    }

    /**
     * Will create a new {@link Position} taking over the attribute-values from the given entity.
     *
     * @param entity the entity.
     * @return the new {@link Position}.
     */
    public Position positionOf( final Object entity ) {
        return toBuilder().value( attribute.valueOf( entity ) )
                .build();
    }

    /**
     * Create a position from this position using a reverse result traversal.
     *
     * @return the new reversed position
     */
    public Position toReversed() {
        return toBuilder().reversed( true )
                .build();
    }

    /**
     * Create a predicate for this position using equal to.
     *
     * @param cqb the query builder
     * @return the predicate
     */
    public Predicate equalTo( final QueryBuilder cqb ) {
        return cqb.equalTo( attribute, value );
    }

    /**
     * Create a predicate for this position using greater than.
     *
     * @param cqb the query builder
     * @return the predicate
     */
    public Predicate greaterThan( final QueryBuilder cqb ) {
        return cqb.greaterThan( attribute, value );
    }

    /**
     * Create a predicate for this position using greater than.
     *
     * @param cqb the query builder
     * @return the predicate
     */
    public Predicate greaterThanOrEqualTo( final QueryBuilder cqb ) {
        return cqb.greaterThanOrEqualTo( attribute, value );
    }

    /**
     * Create a predicate for this position using less than.
     *
     * @param cqb the query builder
     * @return the predicate
     */
    public Predicate lessThan( final QueryBuilder cqb ) {
        return cqb.lessThan( attribute, value );
    }

    /**
     * Create a predicate for this position using less than or equal to.
     *
     * @param cqb the query builder
     * @return the predicate
     */
    public Predicate lessThanOrEqualTo( final QueryBuilder cqb ) {
        return cqb.lessThanOrEqualTo( attribute, value );
    }

    /**
     * Create a condition for this position.
     *
     * @param cqb the query builder
     * @return the condition
     */
    public Predicate condition( final QueryBuilder cqb ) {
        return switch ( direction() ) {
            case ASC -> reversed() ? greaterThanOrEqualTo( cqb ) : greaterThan( cqb );
            case DESC -> reversed() ? lessThanOrEqualTo( cqb ) : lessThan( cqb );
        };
    }

    /**
     * Create condition for this position and combine it with the given conditions using AND.
     *
     * @param cqb           the query builder
     * @param andConditions the conditions to combine with
     * @return the combined conditions
     */
    public List<Predicate> conditionsAnd( final QueryBuilder cqb, final List<Predicate> andConditions ) {
        final List<Predicate> conditions = new ArrayList<>( andConditions.size() + 1 );
        conditions.addAll( andConditions );
        conditions.add( condition( cqb ) );
        return Collections.unmodifiableList( conditions );
    }
}
