package io.vigier.cursorpaging.jpa.api;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.PageRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Data;

/**
 * {@link PageRequest} representation, which can be used in a REST API as request body.
 */
@Data
public class DtoPageRequest {

    private final Map<String, Order> orderBy = new LinkedHashMap<>();

    private final Map<String, List<String>> filterBy = new LinkedHashMap<>();

    @Min( 1 )
    @Max( 100 )
    private int pageSize = 100; // Must not be final!

    private boolean withTotalCount;

    public DtoPageRequest withPageSize( final int pageSize ) {
        this.pageSize = pageSize;
        return this;
    }

    public DtoPageRequest withOrderBy( final String name, final Order order ) {
        orderBy.put( name, order );
        return this;
    }

    public DtoPageRequest withFilterBy( final String name, final String... values ) {
        filterBy.put( name, List.of( values ) );
        return this;
    }

    public <T> PageRequest<T> toPageRequest( final Function<String, Attribute> attributeProvider ) {
        return PageRequest.create( b -> {
            orderBy.forEach( ( name, order ) -> {
                final Attribute attribute = attributeProvider.apply( name );
                switch ( order ) {
                    case ASC -> b.asc( attribute );
                    case DESC -> b.desc( attribute );
                }
            } );
            filterBy.forEach( ( name, values ) -> {
                final Attribute attribute = attributeProvider.apply( name );
                b.filter( Filter.create( fb -> fb.attribute( attribute ).values( values ) ) );
            } );
            b.pageSize( pageSize ).enableTotalCount( withTotalCount );
        } );
    }

    public void addOrderByIfAbsent( final String name, final Order order ) {
        if ( !orderBy.containsKey( name ) ) {
            orderBy.put( name, order );
        }
    }
}
