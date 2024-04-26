package io.vigier.cursorpaging.jpa.serial;

import io.vigier.cursor.Attribute;
import io.vigier.cursor.Filter;
import io.vigier.cursor.Order;
import io.vigier.cursor.PageRequest;
import io.vigier.cursor.Position;
import io.vigier.cursorpaging.jpa.serial.dto.Cursor;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor( staticName = "of" )
public class FromDtoMapper<E> {

    private final Cursor.PageRequest request;
    private final Map<String, Attribute> attributesByName = new HashMap<>();

    public PageRequest<E> map() {
        return PageRequest.<E>builder().positions( positions() ).filters( filters() ).pageSize( request.getPageSize() )
                .build();
    }

    public FromDtoMapper<E> using( final Map<String, Attribute> attributes ) {
        attributesByName.putAll( attributes );
        return this;
    }

    private Collection<Position> positions() {
        return request.getPositionsList().stream().map( this::positionOf ).toList();
    }

    private Collection<Filter> filters() {
        return request.getFiltersList().stream().map( this::filterOf ).toList();
    }

    private Filter filterOf( final Cursor.Filter filter ) {
        final var attribute = attributeOf( filter.getAttribute() );
        return Filter.create(
                b -> b.attribute( attribute ).values( valueListOf( attribute, filter.getValuesList() ) ) );
    }

    private List<? extends Comparable<?>> valueListOf( final Attribute attribute,
            final List<Cursor.Value> valuesList ) {
        return valuesList.stream().map( v -> valueOf( attribute, v ) ).toList();
    }

    private Comparable<?> valueOf( final Attribute attribute, final Cursor.Value value ) {
        if ( value.getValue().isEmpty() ) {
            return null;
        }
        if ( attribute.type().equals( Boolean.class ) ) {
            return "true".equals( value.getValue() );
        }
        if ( attribute.type().equals( Double.class ) ) {
            return Double.parseDouble( value.getValue() );
        }
        if ( attribute.type().equals( Integer.class ) ) {
            return Integer.parseInt( value.getValue() );
        }
        if ( attribute.type().equals( Long.class ) ) {
            return Long.parseLong( value.getValue() );
        }
        if ( attribute.type().equals( String.class ) ) {
            return value.getValue();
        }
        if ( attribute.type().equals( Instant.class ) ) {
            return Instant.ofEpochMilli( Long.parseLong( value.getValue() ) );
        }
        throw new IllegalArgumentException( "Unsupported attribute type: " + attribute.type().getName() );
    }

    private Position positionOf( final Cursor.Position position ) {
        final var attribute = attributeOf( position.getAttribute() );

        return Position.create( b -> b.attribute( attribute ).value( valueOf( attribute, position.getValue() ) )
                .order( switch ( position.getOrder() ) {
                    case ASC -> Order.ASC;
                    case DESC -> Order.DESC;
                    case UNRECOGNIZED -> throw new IllegalArgumentException( "Unrecognized order" );
                } ) );
    }

    private Attribute attributeOf( final Cursor.Attribute attribute ) {
        final var cursorAttribute = attributesByName.get( attribute.getName() );
        if ( cursorAttribute == null ) {
            throw new IllegalArgumentException( "No attribute found for name: " + attribute.getName() );
        }
        return cursorAttribute;
    }

}
