package io.vigier.cursorpaging.jpa;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
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
     * @param qb             builder where position information will be applied.
     * @param preconditions preconditions to apply.
     */
    public void apply( final QueryBuilder qb, final List<Predicate> preconditions ) {
        log.debug( "Position.apply ({} = {}, order = {})", attribute.name(), value, order );
        if ( !isFirst() ) {
            switch ( order ) {
                case ASC -> qb.addWhere( add( qb.greaterThan( attribute, value ), preconditions ) );
                case DESC -> qb.addWhere( add( qb.lessThan( attribute, value ), preconditions ) );
            }
        }
        qb.orderBy( attribute, order );
    }

    private List<Predicate> add( final Predicate condition, final List<Predicate> preconditions ) {
        final List<Predicate> all = new ArrayList<>( preconditions.size() + 1 );
        all.addAll( preconditions );
        all.add( condition );
        return all;
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

    public Predicate getEquals( final QueryBuilder cqb ) {
        return cqb.equalTo( attribute, value );
    }
}
