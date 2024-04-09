package io.vigier.cursor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
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

    private final List<E> content;
    
    /**
     * The request used to fetching this page.
     */
    private final PageRequest<E> self;

    /**
     * The next page request. Empty when there is no further page to fetch.
     */
    private final PageRequest<E> next;

    public static <E> Page<E> create( final Consumer<PageBuilder<E>> creator ) {
        final var builder = Page.<E>builder();
        creator.accept( builder );
        return builder.build();
    }

    @Override
    public @NonNull Iterator<E> iterator() {
        return content.iterator();
    }

    @Override
    public void forEach( final Consumer<? super E> action ) {
        content.forEach( action );
    }

    public List<E> getContent() {
        return Collections.unmodifiableList( content );
    }

    public Optional<PageRequest<E>> next() {
        return Optional.ofNullable( next );
    }

}
