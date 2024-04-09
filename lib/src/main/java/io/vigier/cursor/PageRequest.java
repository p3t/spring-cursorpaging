package io.vigier.cursor;

import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.With;
import lombok.experimental.Accessors;

/**
 * A request, which can be used to query a database for a page of entities.
 * <p>
 * The request uses a list of positions (one for each attribute which should be used to address the start of the page).
 * The combination of all positions must uniquely address a certain record in the table. This can i.e. achieved by
 * adding the primary id of the entity as secondary position, if the first position is not unique, but should be the
 * primary order (e.g. like a created-date).
 *
 * @param <E> the entity type
 */
@Builder( toBuilder = true )
@Getter
@Accessors( fluent = true )
public class PageRequest<E> {

    public static int DEFAULT_PAGE_SIZE = 100;

    @Singular( "position" )
    private final List<Position<E, ? extends Comparable<?>>> positions;

    @Singular
    private final List<Filter<E, ? extends Comparable<?>>> filters;

    @With
    @Builder.Default
    private final int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Adding some short-cut builder methods to create a request
     *
     * @param <E> the entity type
     */
    public static class PageRequestBuilder<E> {

        /**
         * Shortcut for creating a page request for the first page, having a position using the provided attribute in
         * ascending order.
         *
         * @param attribute the attribute used to create a position (ascending ordered)
         * @param <V>       the type of the attribute-value
         * @return the builder
         */
        public <V extends Comparable<? super V>> PageRequestBuilder<E> attributeAsc(
                final SingularAttribute<E, V> attribute ) {
            return firstPage( attribute, Order.ASC );
        }

        /**
         * Shortcut for creating a page request for the first page, having a position using the provided attribute in
         * descending order.
         *
         * @param attribute the attribute used to create a position (ascending ordered)
         * @param <V>       the type of the attribute-value
         * @return the builder
         */
        public <V extends Comparable<? super V>> PageRequestBuilder<E> attributeDesc(
                final SingularAttribute<E, V> attribute ) {
            return firstPage( attribute, Order.DESC );
        }

        /**
         * Shortcut for creating a position for an attribute given a certain order
         *
         * @param attribute the attribute used to create a position (ascending ordered)
         * @param <V>       the type of the attribute-value
         * @return the builder
         */
        public <V extends Comparable<? super V>> PageRequestBuilder<E> firstPage(
                final SingularAttribute<E, V> attribute, final Order order ) {
            return addPosition( Position.<E, V>create( b -> b.attribute( attribute ).order( order ) ) );
        }

        private <V extends Comparable<? super V>> PageRequestBuilder<E> addPosition( final Position<E, V> pos ) {
            if ( this.positions == null ) {
                this.positions = new ArrayList<>( 3 );
            }
            this.positions.add( pos );
            return this;
        }
    }

    public static <E> PageRequest<E> create( final Consumer<PageRequestBuilder<E>> creator ) {
        final var builder = PageRequest.<E>builder();
        creator.accept( builder );
        return builder.build();
    }

    /**
     * Shortcut to create a page-request with a single position, sorting ascending
     *
     * @param attribute the attribute to define a position
     * @param <E>       Entity type
     * @param <V>       Value type of attribute
     * @return A new page request
     */
    public static <E, V extends Comparable<? super V>> PageRequest<E> attributeAsc(
            final SingularAttribute<E, V> attribute ) {
        return create( b -> b.attributeAsc( attribute ) );
    }

    /**
     * Shortcut to create a page-request with a single position, sorting descending
     *
     * @param attribute the attribute to define a position
     * @param <E>       Entity type
     * @param <V>       Value type of attribute
     * @return A new page request
     */
    public static <E, V extends Comparable<? super V>> PageRequest<E> firstDesc(
            final SingularAttribute<E, V> attribute ) {
        return create( b -> b.attributeDesc( attribute ) );
    }


    /**
     * Create a new {@linkplain PageRequest} pointing to the position defined through the attributes of the provided
     * entity.
     *
     * @param entity The entity used as source for the position
     * @return A new PageRequest with the positions set to the values of the provided entity
     */
    public PageRequest<E> positionOf( final E entity ) {
        return create( b -> b.positions( positions.stream()
                .map( p -> p.positionOf( entity ) )
                .toList() ).pageSize( this.pageSize ) );
    }
}
