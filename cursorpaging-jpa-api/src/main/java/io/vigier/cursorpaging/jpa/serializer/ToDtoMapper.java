package io.vigier.cursorpaging.jpa.serializer;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor( staticName = "of" )
class ToDtoMapper<E> {

    private final PageRequest<E> pageRequest;

    public Cursor.PageRequest map() {
        final var builder = Cursor.PageRequest.newBuilder()
                .addAllPositions( positions() )
                .setPageSize( pageRequest.pageSize() )
                .addAllFilters( filters() );
        pageRequest.totalCount().ifPresent( builder::setTotalCount );
        return builder.build();
    }


    private Iterable<Cursor.Filter> filters() {
        return pageRequest.filters().stream()
                .map( f -> Cursor.Filter.newBuilder().setAttribute( attributeOf( f.attribute() ) )
                        .addAllValues( f.values().stream().map( this::valueOf ).toList() )
                        .build() ).toList();
    }

    private Iterable<Cursor.Position> positions() {
        return pageRequest.positions().stream()
                .map( p -> Cursor.Position.newBuilder().setAttribute( attributeOf( p.attribute() ) )
                        .setValue( valueOf( p.value() ) ).setOrder( switch ( p.order() ) {
                            case ASC -> Cursor.Order.ASC;
                            case DESC -> Cursor.Order.DESC;
                        } ).setReversed( p.reversed() )
                        .build() ).toList();
    }


    private static Cursor.Attribute attributeOf( final Attribute attribute ) {
        return Cursor.Attribute.newBuilder().setName( attribute.name() )
                .build();
    }

    private Cursor.Value valueOf( final Comparable<?> value ) {
        if ( value == null ) {
            return Cursor.Value.newBuilder().setValue( "" )
                    .build();
        }
        if ( value instanceof final Instant i ) {
            return Cursor.Value.newBuilder().setValue( "" + i.toEpochMilli() )
                    .build();
        }
        return Cursor.Value.newBuilder().setValue( value.toString() )
                .build();

    }
}
