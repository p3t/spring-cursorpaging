package io.vigier.cursor;

import java.util.function.Consumer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

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
     * Checks if this is the position at the begin of the first page.
     *
     * @return true if the position is at the begin of the first page.
     */
    public boolean isFirst() {
        return value == null;
    }

    /**
     * Will apply the position information to the given {@link QueryBuilder}.
     *
     * @param qb builder where position information will be applied.
     */
    public void apply( final QueryBuilder qb ) {
        if ( !isFirst() ) {
            switch ( order ) {
                case ASC -> qb.greaterThan( attribute, value );
                case DESC -> qb.lessThan( attribute, value );
            }
        }
        qb.orderBy( attribute, order );
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
}
