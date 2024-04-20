package io.vigier.cursor;

import jakarta.persistence.metamodel.SingularAttribute;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@Builder( toBuilder = true )
@Getter
@Accessors( fluent = true )
@EqualsAndHashCode
public class Position {

    private final Attribute attribute;
    private final Comparable<?> value;
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
     * Creates a new {@link Position} with the given attribute pointing to the start of the first page
     * and which will follow the attribute in ascending order.
     *
     * @param attribute the attribute.
     * @return the new {@link Position}.
     */
    public static Position attributeAsc( final SingularAttribute<?, ? extends Comparable<?>> attribute ) {
        return create( b -> b.attribute( Attribute.of( attribute ) ).order( Order.ASC ) );
    }

    /**
     * Creates a new {@link Position} with the given attribute pointing to the start of the first page and which will
     * follow the attribute in descending order.
     *
     * @param attribute the attribute.
     * @return the new {@link Position}.
     */
    public static Position attributeDesc( final SingularAttribute<?, ? extends Comparable<?>> attribute ) {
        return create( b -> b.attribute( Attribute.of( attribute ) ).order( Order.DESC ) );
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
