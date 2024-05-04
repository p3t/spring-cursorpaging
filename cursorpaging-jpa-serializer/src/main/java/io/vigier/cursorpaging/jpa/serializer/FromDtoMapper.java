package io.vigier.cursorpaging.jpa.serializer;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.Position;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;

@Builder
@RequiredArgsConstructor( staticName = "of" )
public class FromDtoMapper<E> {

    private final Cursor.PageRequest request;
    @Builder.Default
    private final Map<String, Attribute> attributesByName = new HashMap<>();
    private final ConversionService conversionService;

    public  static <T> FromDtoMapper<T> create( final Consumer<FromDtoMapperBuilder<T>> c ) {
        final var builder = FromDtoMapper.<T>builder();
        c.accept( builder );
        return builder.build();
    }

    public PageRequest<E> map() {
        return PageRequest.<E>builder().positions( positions() ).filters( filters() ).pageSize( request.getPageSize() )
                .build();
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

    private <T extends Comparable<? super T>> List<T> valueListOf( final Attribute attribute,
            final List<Cursor.Value> valuesList ) {
        return valuesList.stream().map( v -> this.<T>valueOf( attribute, v ) ).toList();
    }

    private <T extends Comparable<? super T>> T valueOf( final Attribute attribute, final Cursor.Value value ) {
        if ( value.getValue().isEmpty() ) {
            return null;
        }
        return conversionService.<T>convert( value.getValue(), attribute.type() );
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
