package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.filter.AndFilter;
import io.vigier.cursorpaging.jpa.filter.FilterList;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * A request, which can be used to query a database for a page of entities.
 * <p>
 * The request uses a list of `positions` (one for each attribute which should be used to address the start of the
 * page). The combination of all positions must uniquely address a certain record in the table. This can be achieved by
 * adding the primary id of the entity as secondary position, if the first position is not unique (like a `name`, or
 * date), but should be the primary order.
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
     * The positions used to address the start of a page. It is essential, that at least one position (order-by,
     * asc/dsc) is provided, and that it cannot happen, that all positions can contain null-values (are nullable columns
     * in the DB)!
     */
    private final List<Position> positions;

    /**
     * The filters to apply to the query (removing results)
     */
    @Builder.Default
    private final FilterList filters = AndFilter.of();

    /**
     * The size of the page to fetch
     */
    @Builder.Default
    private final int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Control if the total element count should be calculated if missing in the request
     */
    private final boolean enableTotalCount;

    private final Long totalCount;

    /**
     * Marks the request for the first page (default). A request for a following page (a cursor) is created with
     * {@code false}, as its position values can be {@code null}.
     */
    @Getter( AccessLevel.NONE )
    @Builder.Default
    private final boolean firstPage = true;

    /**
     * Adding some short-cut builder methods to create a request
     *
     * @param <E> the entity type
     */
    public static class PageRequestBuilder<E> {

        /**
         * Shortcut for adding a position spec of an attribute in ascending order
         *
         * @param attribute the attribute used to create a position (ascending ordered)
         * @return the builder
         */
        public PageRequestBuilder<E> asc( final SingularAttribute<? super E, ? extends Comparable<?>> attribute ) {
            return sort( attribute, Order.ASC );
        }

        /**
         * Shortcut for adding a position spec of an attribute in descending order
         *
         * @param attribute the attribute used to create a position (descending ordered)
         * @return the builder
         */
        public PageRequestBuilder<E> desc( final SingularAttribute<? super E, ? extends Comparable<?>> attribute ) {
            return sort( attribute, Order.DESC );
        }

        /**
         * Shortcut for adding a position spec of an attribute in ascending order
         *
         * @param attribute the attribute used to create a position (ascending ordered)
         * @return the builder
         */
        public PageRequestBuilder<E> asc( final Attribute attribute ) {
            return sort( attribute, Order.ASC );
        }

        /**
         * Shortcut for adding a position spec of an attribute in descending order
         *
         * @param attribute the attribute used to create a position (descending ordered)
         * @return the builder
         */
        public PageRequestBuilder<E> desc( final Attribute attribute ) {
            return sort( attribute, Order.DESC );
        }

        /**
         * Shortcut for adding a position spec of an attribute in ascending order
         *
         * @param name the name of the attribute
         * @param type the type of the attribute
         * @return the builder
         */
        public PageRequestBuilder<E> asc( final String name, final Class<? extends Comparable<?>> type ) {
            return sort( Attribute.of( name, type ), Order.ASC );
        }

        /**
         * Shortcut for adding a position spec of an attribute in descending order
         *
         * @param name the name of the attribute
         * @param type the type of the attribute
         * @return the builder
         */
        public PageRequestBuilder<E> desc( final String name, final Class<? extends Comparable<?>> type ) {
            return sort( Attribute.of( name, type ), Order.DESC );
        }

        public PageRequestBuilder<E> sort( final SingularAttribute<? super E, ? extends Comparable<?>> attribute,
                final Order order ) {
            return sort( Attribute.of( attribute ), order );
        }

        public PageRequestBuilder<E> sort( final Attribute attribute, final Order order ) {
            return addPosition( Position.create( b -> b.attribute( attribute ).order( order ) ) );
        }

        /**
         * Add a filter to the request. Filter which do not contain a filter value or empty char-sequences as values are
         * silently ignored for convenience reasons when creating page requests out of query parameters.
         *
         * @param filter A new filter definition
         * @return the builder
         */
        public PageRequestBuilder<E> filter( @Nullable final QueryElement filter ) {
            final List<QueryElement> filters = new LinkedList<>();
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
        public PageRequestBuilder<E> filters( @Nullable final FilterList filters ) {
            if ( filters != null ) {
                this.filters$value = filters;
                this.filters$set = true;
            }
            return this;
        }

        public PageRequestBuilder<E> apply( final Consumer<PageRequestBuilder<E>> consumer ) {
            consumer.accept( this );
            return this;
        }

        public PageRequestBuilder<E> position( final Position pos ) {
            return addPosition( pos );
        }

        public PageRequestBuilder<E> positions( final Collection<Position> positions ) {
            positions.forEach( this::addPosition );
            return this;
        }


        private PageRequestBuilder<E> addPosition( final Position pos ) {
            if ( this.positions == null ) {
                this.positions = new ArrayList<>( 3 );
            }
            if ( pos.hasValue() ) {
                this.firstPage$set = true;
                this.firstPage$value = false;
            }
            this.positions.add( pos );
            return this;
        }
    }

    public PageRequest( final List<Position> positions, final FilterList filters, final int pageSize,
            final boolean enableTotalCount, final Long totalCount, final boolean firstPage ) {
        if ( positions == null || positions.isEmpty() ) {
            throw new IllegalArgumentException(
                    "Cannot create page-request, at least one order-attribute (asc/desc) for determine the position of the page start is required" );
        }
        final boolean hasNoPosition = positions.stream().noneMatch( Position::hasValue );
        if ( firstPage && !hasNoPosition ) {
            throw new IllegalArgumentException( "Cannot create page-request for the first page with position values" );
        }
        if ( !firstPage && hasNoPosition ) {
            throw new IllegalArgumentException(
                    "Cannot create page-request for a following page, all positions " + positions.stream()
                            .map( p -> p.attribute().name() ).toList()
                            + " have null values: the positions do not address a unique record. Add a unique, not nullable attribute (e.g. the id) as last position" );
        }
        this.positions = positions;
        this.filters = filters;
        this.pageSize = pageSize;
        this.enableTotalCount = enableTotalCount;
        this.totalCount = totalCount;
        this.firstPage = firstPage;
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
    public PageRequest<E> copy( final Consumer<PageRequestBuilder<E>> c ) {
        final PageRequestBuilder<E> builder = PageRequest.<E>builder().totalCount( totalCount )
                .enableTotalCount( enableTotalCount ).pageSize( pageSize ).firstPage( firstPage );
        c.accept( builder );
        if ( !builder.filters$set && !filters.isEmpty() ) {
            builder.filters( filters );
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
    public PageRequest<E> withEnableTotalCount( final boolean enable ) {
        return copy( b -> b.enableTotalCount( enable ).totalCount( null ) );
    }

    /**
     * creates a new page request with the given size, or returns the current one if size is {@code null} or the same as
     * the actual size.
     *
     * @param size the requested max page size. {@code null} is accepted and will return the current request.
     * @return Page request with the provided size
     */
    public PageRequest<E> withPageSize( @Nullable final Integer size ) {
        if ( size == null || pageSize() == size ) {
            return this;
        }
        return copy( b -> b.pageSize( size ) );
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
     * @param entity The entity used as a source for the position
     * @return A new {@code PageRequest} with the positions set to the values of the provided entity
     */
    public PageRequest<E> positionOf( @Nonnull final E entity, @Nonnull final E nextEntity ) {
        return create( b -> b.positions( positions.stream().map( p -> p.positionOf( entity, nextEntity ) ).toList() )
                .pageSize( this.pageSize ).totalCount( this.totalCount ).filters( this.filters )
                .enableTotalCount( this.enableTotalCount ).firstPage( false ) );
    }

    public PageRequest<E> toReversed() {
        return copy( b -> b.positions( positions.stream().map( Position::toReversed ).toList() ) );
    }

    /**
     * Checks if this is a request for the first page. Requests created for a following page (see
     * {@link #positionOf(Object, Object)}) are never a first page, even if position values are {@code null}.
     *
     * @return {@code true} if the request is for the first page, {@code false} otherwise
     */
    public boolean isFirstPage() {
        return firstPage;
    }

    public boolean isReversed() {
        return positions.getFirst().reversed();
    }

    /**
     * Returns the <b>first</b> filter found given the attribute. Probably useful for tests to verify that the request
     * is as expected.
     *
     * @param condition predicate to select the filter
     * @return a present query-element (filter) containing the attribute or an empty optional if no filter is found
     */
    public Optional<QueryElement> firstFilterWith( final Predicate<QueryElement> condition ) {
        return findQueryElement( condition, filters );
    }

    private Optional<QueryElement> findQueryElement( final Predicate<QueryElement> condition, final QueryElement ele ) {
        if ( condition.test( ele ) ) {
            return Optional.of( ele );
        }
        if ( ele instanceof final FilterList fl ) {
            for ( final var f : fl.filters() ) {
                final var found = findQueryElement( condition, f );
                if ( found.isPresent() ) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the <b>first</b> filter found given the attribute. Probably useful for tests to verify that the request
     * is as expected. The search iterates through all filter-lists and will return the first {@code Filter} found (not
     * and/or lists).
     *
     * @param attribute Attribute which should be used by the filter
     * @return a present {@linkplain Filter} containing the attribute or an empty optional if no filter is found
     */
    public Optional<Filter> firstFilterWith( final Attribute attribute ) {
        return firstFilterWith( ele -> ele instanceof final Filter f && f.attributes().stream()
                .anyMatch( a -> a.equals( attribute ) ) ).map( Filter.class::cast );
    }

    /**
     * Returns the <b>first</b> filter-list (and/or filter-lists) found containing directly a {@linkplain Filter} with
     * the given attribute. Probably useful for tests.
     *
     * @param attribute Attribute which should be used by the filter (which is in the filter-list)
     * @return a present filter-list containing a filter with the attribute or an empty optional if no filter-list is
     * found
     */
    public Optional<FilterList> firstFilterListWith( final Attribute attribute ) {
        return firstFilterWith( ele -> ele instanceof final FilterList fl && fl.filters().stream()
                .anyMatch( a -> a instanceof final Filter f && f.attribute().equals( attribute ) ) ).map(
                FilterList.class::cast );
    }

}
