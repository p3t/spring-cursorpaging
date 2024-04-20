package io.vigier.cursorpaging.jpa.serial;

import io.vigier.cursor.Attribute;
import io.vigier.cursor.Filter;
import io.vigier.cursor.Order;
import io.vigier.cursor.PageRequest;
import io.vigier.cursor.Position;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoAttribute;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoFilter;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoPageRequest;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoPosition;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoValue;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor( staticName = "of" )
public class FromDtoMapper<E> {

    private final DtoPageRequest request;
    private final Map<String, SingularAttribute<E, ? extends Comparable<?>>> attributesByName = new HashMap<>();

    public PageRequest<E> map() {
        return PageRequest.<E>builder().positions( positions() ).filters( filters() )
                .build();
    }

    public FromDtoMapper<E> using( final Collection<SingularAttribute<E, ? extends Comparable<?>>> attributes ) {
        attributes.forEach( a -> attributesByName.put( a.getName(), a ) );
        return this;
    }

    private Collection<Position> positions() {
        return request.getPositionsList().stream().map( this::positionOf ).toList();
    }

    private Collection<Filter> filters() {
        return request.getFiltersList().stream().map( this::filterOf ).toList();
    }

    private Filter filterOf( final DtoFilter filter ) {
        final var attribute = attributeOf( filter.getAttribute() );
        return Filter.create(
                b -> b.attribute( attribute ).values( valueListOf( attribute, filter.getValuesList() ) ) );
    }

    private List<? extends Comparable<?>> valueListOf( final io.vigier.cursor.Attribute attribute,
            final List<DtoValue> valuesList ) {
        return valuesList.stream().map( v -> valueOf( attribute, v ) ).toList();
    }

    private Comparable<?> valueOf( final Attribute attribute, final DtoValue value ) {
        if ( "".equals( value.getValue() ) ) {
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

    private Position positionOf( final DtoPosition position ) {
        final var attribute = attributeOf( position.getAttribute() );

        return Position.create( b -> b.attribute( attribute ).value( valueOf( attribute, position.getValue() ) )
                .order( switch ( position.getOrder() ) {
                    case ASC -> Order.ASC;
                    case DESC -> Order.DESC;
                    case UNRECOGNIZED -> throw new IllegalArgumentException( "Unrecognized order" );
                } ) );
    }

    private io.vigier.cursor.Attribute attributeOf( final DtoAttribute attribute ) {
        final var singularAttribute = attributesByName.get( attribute.getName() );
        if ( singularAttribute == null ) {
            throw new IllegalArgumentException( "No attribute found for name: " + attribute.getName() );
        }
        return io.vigier.cursor.Attribute.of( singularAttribute );
    }

}
