package io.vigier.cursorpaging.jpa;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A page of entities.
 *
 * @param <E> the entity type
 */
@Getter
@Builder
@Accessors( fluent = true )
public class Page<E> implements Iterable<E> {

    /**
     * The content of this page.
     */
    private final List<E> content;
    
    /**
     * The request used to fetching this page.
     */
    private final PageRequest<E> self;

    /**
     * The next page request. Empty when there is no further page to fetch.
     */
    private final PageRequest<E> next;

    /**
     * Creates a new page.
     *
     * @param <E>     the entity type
     * @param creator the page builder consumer
     * @return the created page
     */
    public static <E> Page<E> create( final Consumer<PageBuilder<E>> creator ) {
        final var builder = Page.<E>builder();
        creator.accept( builder );
        return builder.build();
    }

    /**
     * Get an iterator over the content of this page.
     *
     * @return the iterator
     * @see Iterable#iterator()
     */
    @Override
    public @NonNull Iterator<E> iterator() {
        return content.iterator();
    }

    /**
     * Loop over the content of the page
     *
     * @param action the action to perform on each element
     * @see Iterable#forEach(Consumer)
     */
    @Override
    public void forEach( final Consumer<? super E> action ) {
        content.forEach( action );
    }

    /**
     * The content of this page.
     *
     * @return the content
     */
    public List<E> getContent() {
        return Collections.unmodifiableList( content );
    }

    /**
     * The request which can be used to fetching the next page.
     *
     * @return the request or an empty {@link Optional} if there is no further page
     */
    public Optional<PageRequest<E>> next() {
        return Optional.ofNullable( next );
    }

    /**
     * Get the next page request with the given page size.
     *
     * @param pageSize desired page size
     * @return the next page request or an empty {@link Optional} if there is no further page
     */
    public Optional<PageRequest<E>> next( final int pageSize ) {
        return next().map( pageRequest -> pageRequest.withPageSize( pageSize ) );
    }

    /**
     * Map the content with the given function and get the result
     *
     * @param mapper the function to be applied on each element of the page
     * @param <T>    the result type of the mapping function
     * @return the mapped content
     */
    public <T> List<T> content( final Function<E, T> mapper ) {
        return content.stream().map( mapper ).toList();
    }

    /**
     * Get the number of elements in this page.
     *
     * @return the number of elements
     */
    public int size() {
        return content.size();
    }
}
