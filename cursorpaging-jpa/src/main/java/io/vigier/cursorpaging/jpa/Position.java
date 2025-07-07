package io.vigier.cursorpaging.jpa;

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
     * The current position-value from where on the next results should be queried. I.e. the last value on the current
     * page.
     */
    private final Comparable<?> value;

    /**
     * The position-value from where on the next results should be queried I.e. the first value on the next page.
     */
    private final Comparable<?> nextValue;

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
            } else if ( order$value == null ) {
                throw new IllegalStateException(
                        "Order for position/attribute: '" + attribute.name() + "' must not be null" );
            } else {
                attribute.attributes().forEach( a -> {
                    if ( a.name() == null || a.name().isBlank() ) {
                        throw new IllegalStateException( "Attribute name must not be null or empty" );
                    } else if ( a.type() == null ) {
                        throw new IllegalStateException( "Attribute type must not be null" );
                    }
                } );
            }
            return new Position( attribute, value, nextValue, order$value, reversed );
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
     * Checks if this is the position has a value.
     *
     * @return true if the position has a value.
     */
    public boolean hasValue() {
        return value != null;
    }

    public boolean hasNextValue() {
        return nextValue != null;
    }

    /**
     * Will create a new {@link Position} taking over the attribute-values from the given entity.
     *
     * @param entity the entity.
     * @return the new {@link Position}.
     */
    public Position positionOf( final Object entity, final Object nextEntity ) {
        return toBuilder().value( attribute.valueOf( entity ) ).nextValue( attribute.valueOf( nextEntity ) )
                .build();
    }

    /**
     * Create a position from this position using a reverse result traversal.
     *
     * @return the new reversed position
     */
    public Position toReversed() {
        // maybe switch nextValue and value?
        final var theValue = value();
        return toBuilder().reversed( true )
                .value( nextValue )
                .nextValue( theValue )
                .order( order == Order.ASC ? Order.DESC : Order.ASC )
                .build();
    }
}
