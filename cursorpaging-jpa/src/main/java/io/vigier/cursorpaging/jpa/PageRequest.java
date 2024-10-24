package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.filter.AndFilter;
import io.vigier.cursorpaging.jpa.filter.FilterList;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import jakarta.persistence.Transient;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
@Builder
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
    @Builder.Default
    private final FilterList filters = AndFilter.of();

    /**
     * The filter rules to apply to the query (removing results). Note that filter rules are <i>not</i> passed to the
     * client in serialized form, and must be added every-time the cursor is deserialized.
     */
    @Transient
    @Singular
    private final List<FilterRule> rules;

    /**
     * The size of the page to fetch
     */
    @With
    @Builder.Default
    private final int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Control if the total element count should be calculated if missing in the request
     */
    private final boolean enableTotalCount;

    private final Long totalCount;

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
                final SingularAttribute<? super E, ? extends Comparable<?>> attribute ) {
            return addPosition( Position.create( b -> b.attribute( Attribute.of( attribute ) ).order( order ) ) );
        }

        /**
         * Shortcut for adding a position spec of an attribute in ascending  order
         *
         * @param attribute the attribute used to create a position (ascending ordered)
         * @return the builder
         */
        public PageRequestBuilder<E> asc( final SingularAttribute<? super E, ? extends Comparable<?>> attribute ) {
            return firstPage( Order.ASC, attribute );
        }

        /**
         * Shortcut for adding a position spec of an attribute in descending  order
         *
         * @param attribute the attribute used to create a position (descending ordered)
         * @return the builder
         */
        public PageRequestBuilder<E> desc( final SingularAttribute<? super E, ? extends Comparable<?>> attribute ) {
            return firstPage( Order.DESC, attribute );
        }

        /**
         * Shortcut for adding a position spec of an attribute in ascending order
         *
         * @param attribute the attribute used to create a position (ascending ordered)
         * @return the builder
         */
        public PageRequestBuilder<E> asc( final Attribute attribute ) {
            return addPosition( Position.create( b -> b.attribute( attribute ).order( Order.ASC ) ) );
        }

        /**
         * Shortcut for adding a position spec of an attribute in descending order
         *
         * @param attribute the attribute used to create a position (descending ordered)
         * @return the builder
         */
        public PageRequestBuilder<E> desc( final Attribute attribute ) {
            return addPosition( Position.create( b -> b.attribute( attribute ).order( Order.DESC ) ) );
        }

        /**
         * Shortcut for adding a position spec of an attribute in ascending order
         *
         * @param name the name of the attribute
         * @param type the type of the attribute
         * @return the builder
         */
        public PageRequestBuilder<E> asc( final String name, final Class<? extends Comparable<?>> type ) {
            return addPosition( Position.create( b -> b.attribute( Attribute.of( name, type ) ).order( Order.ASC ) ) );
        }

        /**
         * Shortcut for adding a position spec of an attribute in descending order
         *
         * @param name the name of the attribute
         * @param type the type of the attribute
         * @return the builder
         */
        public PageRequestBuilder<E> desc( final String name, final Class<? extends Comparable<?>> type ) {
            return addPosition( Position.create( b -> b.attribute( Attribute.of( name, type ) ).order( Order.DESC ) ) );
        }

        /**
         * Add a filter to the request. Filter which do not contain a filter value or empty char-sequences as values are
         * silently ignored for convenience reasons when creating page requests out of query parameters.
         *
         * @param filter A new filter definition
         * @return the builder
         */
        public PageRequestBuilder<E> filter( final QueryElement filter ) {
            List<QueryElement> filters = new LinkedList<>();
            if ( this.filters$value != null ) {
                filters.addAll( this.filters$value.filters() );
            }
            if ( filter != null && !filter.isEmpty() ) {
                filters.add( filter );
            }
            this.filters$value = (filters$value instanceof OrFilter ? OrFilter.of( filters ) : AndFilter.of( filters ));
            this.filters$set = true;
            return this;
        }

        /**
         * Add a list of filters to the page request. Filter which do not contain a filter value or empty char-sequences
         * as values are silently ignored for convenience reasons when creating page requests out of query parameters.
         *
         * @param filters the list of filters to be added
         * @return the builder
         */
        public PageRequestBuilder<E> filters( final FilterList filters ) {
            if ( filters != null ) {
                this.filters$value = filters;
                this.filters$set = true;
            }
            return this;
        }

        private PageRequestBuilder<E> addPosition( final Position pos ) {
            if ( this.positions == null ) {
                this.positions = new ArrayList<>( 3 );
            }
            this.positions.add( pos );
            return this;
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
     * Create a new page-request from the current one, but with the provided customizer applied
     *
     * @param c customizer for the copy
     * @return A new page-request with existing and customized attributes
     */
    public PageRequest<E> copy( Consumer<PageRequestBuilder<E>> c ) {
        PageRequestBuilder<E> builder = PageRequest.<E>builder()
                .totalCount( totalCount )
                .enableTotalCount( enableTotalCount )
                .pageSize( pageSize );
        c.accept( builder );
        if ( !builder.filters$set && !filters.isEmpty() ) {
            builder.filters( filters );
        }
        if ( (builder.rules == null || builder.rules.isEmpty()) && !rules.isEmpty() ) {
            builder.rules( rules );
        }
        if ( (builder.positions == null || builder.positions.isEmpty()) && !positions.isEmpty() ) {
            builder.positions( positions );
        }
        return builder.build();
    }

    /**
     * Enable the total count calculation for the request.<br> Setting {@code enable = true} forces also the
     * re-calculation of the total count for a page-request where the total-count is already present.
     *
     * @return A copy of the page-request where the total-count is removed and the enable flag is set accordingly
     */
    public PageRequest<E> withEnableTotalCount( boolean enable ) {
        return copy( b -> b.enableTotalCount( enable ).totalCount( null ) );
    }

    /**
     * Get the total count if present
     *
     * @return the total count if present
     */
    public Optional<Long> totalCount() {
        return Optional.ofNullable( totalCount );
    }

    /**
     * Create a new {@linkplain PageRequest} pointing to the position defined through the attributes of the provided
     * entity.
     *
     * @param entity The entity used as source for the position
     * @return A new PageRequest with the positions set to the values of the provided entity
     */
    public PageRequest<E> positionOf( final E entity ) {
        return create( b -> b.positions( positions.stream().map( p -> p.positionOf( entity ) ).toList() )
                .pageSize( this.pageSize )
                .totalCount( this.totalCount )
                .enableTotalCount( this.enableTotalCount ) );
    }

    public PageRequest<E> toReversed() {
        return copy( b -> b.positions( positions.stream().map( Position::toReversed ).toList() ) );
    }

    public boolean isFirstPage() {
        return positions.get( 0 ).isFirst();
    }

    public boolean isReversed() {
        return positions.get( 0 ).reversed();
    }
}
