package io.vigier.cursor;

import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
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
@EqualsAndHashCode
@ToString
public class PageRequest<E> {

    /**
     * The default size used to fetch the page if non is provided
     */
    public static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * The positions used to address the start of a page.
     */
    @Singular( "position" )
    private final List<Position> positions;

    /**
     * The filters to apply to the query (removing results)
     */
    @Singular
    private final List<Filter> filters;

    /**
     * The size of the page to fetch
     */
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
         * Shortcut for creating a position for an attribute given a certain order
         *
         * @param attribute the attribute used to create a position (ascending ordered)
         * @return the builder
         */
        public PageRequestBuilder<E> firstPage( final Order order,
                final SingularAttribute<E, ? extends Comparable<?>> attribute ) {
            return addPosition( Position.create( b -> b.attribute( Attribute.of( attribute ) ).order( order ) ) );
        }

        private PageRequestBuilder<E> addPosition( final Position pos ) {
            if ( this.positions == null ) {
                this.positions = new ArrayList<>( 3 );
            }
            this.positions.add( pos );
            return this;
        }

        public PageRequestBuilder<E> asc( final SingularAttribute<E, ? extends Comparable<?>> attribute ) {
            return addPosition( Position.create( b -> b.attribute( Attribute.of( attribute ) ).order( Order.ASC ) ) );
        }

        public PageRequestBuilder<E> desc( final SingularAttribute<E, ? extends Comparable<?>> attribute ) {
            return addPosition( Position.create( b -> b.attribute( Attribute.of( attribute ) ).order( Order.DESC ) ) );
        }

        public PageRequestBuilder<E> asc( final Attribute attribute ) {
            return addPosition( Position.create( b -> b.attribute( attribute ).order( Order.ASC ) ) );
        }

        public PageRequestBuilder<E> desc( final Attribute attribute ) {
            return addPosition( Position.create( b -> b.attribute( attribute ).order( Order.DESC ) ) );
        }
    }

    /**
     * Create a new page-request with a builder
     *
     * @param creator the customizer for the builder
     * @param <E>     Entity type
     * @return the created page request
     */
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
     * @return A new page request, for the first page
     */
    public static <E, V extends Comparable<? super V>> PageRequest<E> firstAsc(
            final SingularAttribute<E, V> attribute ) {
        return create( b -> b.asc( attribute ) );
    }

    /**
     * Shortcut to create a page-request with a single position, sorting descending
     *
     * @param attribute the attribute to define a position
     * @param <E>       Entity type
     * @param <V>       Value type of attribute
     * @return A new page request, for the first page
     */
    public static <E, V extends Comparable<? super V>> PageRequest<E> firstDesc(
            final SingularAttribute<E, V> attribute ) {
        return create( b -> b.desc( attribute ) );
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
