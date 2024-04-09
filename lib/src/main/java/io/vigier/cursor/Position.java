package io.vigier.cursor;

import jakarta.persistence.metamodel.SingularAttribute;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanWrapper;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;

@Builder( toBuilder = true )
@Getter
@Accessors( fluent = true )
public class Position<E, V extends Comparable<? super V>> {

    private final SingularAttribute<E, V> attribute;
    private final V value;
    @Builder.Default
    private final Order order = Order.ASC;

    public static <E, V extends Comparable<? super V>> Position<E, V> create(
            final Consumer<PositionBuilder<E, V>> creator ) {
        final var builder = Position.<E, V>builder();
        creator.accept( builder );
        return builder.build();
    }

    /**
     * Creates a new {@link Position} with the given attribute pointing to the start of the first page
     * and which will follow the attribute in ascending order.
     *
     * @param attribute the attribute.
     * @param <E>       the entity type.
     * @param <V>       the value type.
     * @return the new {@link Position}.
     */
    public static <E, V extends Comparable<? super V>> Position<E, V> attributeAsc(
            final SingularAttribute<E, V> attribute ) {
        return create( b -> b.attribute( attribute ).order( Order.ASC ) );
    }

    /**
     * Creates a new {@link Position} with the given attribute pointing to the start of the first page and which will
     * follow the attribute in descending order.
     *
     * @param attribute the attribute.
     * @param <E>       the entity type.
     * @param <V>       the value type.
     * @return the new {@link Position}.
     */
    public static <E, V extends Comparable<? super V>> Position<E, V> attributeDesc(
            final SingularAttribute<E, V> attribute ) {
        return create( b -> b.attribute( attribute ).order( Order.DESC ) );
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
     * @param qb
     */
    public void apply( final QueryBuilder<E> qb ) {
        if ( !isFirst() ) {
            switch ( order ) {
                case ASC -> qb.greaterThan( attribute, value );
                case DESC -> qb.lessThan( attribute, value );
            }
        }
        qb.orderBy( attribute, order );
    }

    public Position<E, V> positionOf( final E entity ) {
        return toBuilder().value( attribute.getJavaType().cast( getValue( entity, attribute.getName() ) ) ).build();
    }

    private Object getValue( final E entity, final String attributeName ) {
        final BeanWrapper beanWrapper = new DirectFieldAccessFallbackBeanWrapper( entity );
        return beanWrapper.getPropertyValue( attributeName );
    }
}
