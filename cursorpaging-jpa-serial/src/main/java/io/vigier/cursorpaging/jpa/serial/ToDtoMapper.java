package io.vigier.cursorpaging.jpa.serial;

import io.vigier.cursor.Attribute;
import io.vigier.cursor.PageRequest;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoAttribute;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoFilter;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoOrder;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoPageRequest;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoPosition;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoValue;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor( staticName = "of" )
public class ToDtoMapper<E> {

    private final PageRequest<E> pageRequest;

    public DtoPageRequest map() {

        return DtoPageRequest.newBuilder().addAllPositions( positions() ).setPageSize( pageRequest.pageSize() )
                .addAllFilters( filters() )
                .build();
    }


    private Iterable<DtoFilter> filters() {
        return pageRequest.filters().stream()
                .map( f -> DtoFilter.newBuilder().setAttribute( attributeOf( f.attribute() ) )
                        .addAllValues( f.values().stream().map( this::valueOf ).toList() )
                        .build() ).toList();
    }

    private Iterable<DtoPosition> positions() {
        return pageRequest.positions().stream()
                .map( p -> DtoPosition.newBuilder().setAttribute( attributeOf( p.attribute() ) )
                        .setValue( valueOf( p.value() ) ).setOrder( switch ( p.order() ) {
                            case ASC -> DtoOrder.ASC;
                            case DESC -> DtoOrder.DESC;
                        } )
                        .build() ).toList();
    }


    private static DtoAttribute attributeOf( final Attribute attribute ) {
        return DtoAttribute.newBuilder().setName( attribute.name() )
                .build();
    }

    private DtoValue valueOf( final Comparable<?> value ) {
        if ( value == null ) {
            return DtoValue.newBuilder().setValue( "" )
                    .build();
        }
        if ( value instanceof final Instant i ) {
            return DtoValue.newBuilder().setValue( "" + i.toEpochMilli() )
                    .build();
        }
        return DtoValue.newBuilder().setValue( value.toString() )
                .build();

    }
}
