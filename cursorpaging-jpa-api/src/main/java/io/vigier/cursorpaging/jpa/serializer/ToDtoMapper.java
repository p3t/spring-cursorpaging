package io.vigier.cursorpaging.jpa.serializer;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.FilterRule;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.filter.AndFilter;
import io.vigier.cursorpaging.jpa.filter.EqualFilter;
import io.vigier.cursorpaging.jpa.filter.FilterList;
import io.vigier.cursorpaging.jpa.filter.GreaterThanFilter;
import io.vigier.cursorpaging.jpa.filter.LessThanFilter;
import io.vigier.cursorpaging.jpa.filter.LikeFilter;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor.FilterList.FilterListType;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor.FilterType;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor.Rule.Parameter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor( staticName = "of" )
class ToDtoMapper<E> {

    private static final Map<Class<? extends Filter>, FilterType> TYPE_MAP = Map.of( //
            EqualFilter.class, FilterType.EQ, //
            GreaterThanFilter.class, FilterType.GT, //
            LessThanFilter.class, FilterType.LT, //
            LikeFilter.class, FilterType.LIKE //
    );
    private static final Map<Class<? extends FilterList>, FilterListType> LISTTYPE_MAP = Map.of( //
            AndFilter.class, FilterListType.AND, //
            OrFilter.class, FilterListType.OR );

    private final PageRequest<E> pageRequest;

    public static <E> ToDtoMapper<E> create( final Consumer<ToDtoMapperBuilder<E>> c ) {
        final var builder = ToDtoMapper.<E>builder();
        c.accept( builder );
        return builder.build();
    }

    public Cursor.PageRequest map() {
        final var builder = Cursor.PageRequest.newBuilder()
                .addAllPositions( positions() )
                .setPageSize( pageRequest.pageSize() )
                .setFilters( filters() )
                .addAllFilterRules( filterRules() );
        pageRequest.totalCount().ifPresent( builder::setTotalCount );
        return builder.build();
    }

    private Iterable<Cursor.Rule> filterRules() {
        return pageRequest.rules()
                .stream()
                .map( r -> Cursor.Rule.newBuilder().setName( r.name() ).addAllParameters( toDtoParameters( r ) )
                        .build() )
                .toList();
    }

    private List<Parameter> toDtoParameters( final FilterRule r ) {
        return r.parameters()
                .entrySet()
                .stream()
                .map( e -> Parameter.newBuilder()
                        .setName( e.getKey() )
                        .addAllValues( e.getValue().stream().map( this::valueOf ).toList() )
                        .build() )
                .toList();
    }


    private Cursor.FilterList filters() {
        return toDto( pageRequest.filters() );
    }

    private Cursor.FilterList toDto( FilterList list ) {
        Cursor.FilterList.Builder b = Cursor.FilterList.newBuilder().setType( typeOf( list ) );
        list.forEach( f -> {
            if ( f instanceof Filter ff ) {
                b.addFilters( Cursor.Filter.newBuilder()
                        .setAttribute( attributeOf( ff.attribute() ) )
                        .addAllValues( ff.values().stream().map( this::valueOf ).toList() )
                        .setType( typeOf( ff ) )
                        .build() );
            } else if ( f instanceof FilterList fl ) {
                b.addFilterLists( toDto( fl ) );
            }
        } );
        return b.build();
    }

    FilterType typeOf( Filter f ) {
        return TYPE_MAP.get( f.getClass() );
    }

    FilterListType typeOf( FilterList f ) {
        return LISTTYPE_MAP.get( f.getClass() );
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
