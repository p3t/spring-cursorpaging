package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.QueryElement;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors( fluent = true )
@EqualsAndHashCode
public abstract class FilterList implements QueryElement, Iterable<QueryElement> {

    private final List<QueryElement> filters;

    protected FilterList( final List<QueryElement> filters ) {
        this.filters = List.copyOf( filters );
    }

    @Override
    public List<Attribute> attributes() {
        return filters.stream().flatMap( e -> e.attributes().stream() ).toList();
    }

    /**
     * Returns an iterator over elements within the filter-list.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<QueryElement> iterator() {
        return filters.iterator();
    }

    @Override
    public void forEach( final Consumer<? super QueryElement> action ) {
        filters.forEach( action );
    }

    public int size() {
        return filters.size();
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }
}
