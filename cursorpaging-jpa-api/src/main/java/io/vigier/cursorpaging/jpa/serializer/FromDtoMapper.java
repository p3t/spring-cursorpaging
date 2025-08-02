package io.vigier.cursorpaging.jpa.serializer;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.FilterRule;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.Position;
import io.vigier.cursorpaging.jpa.QueryElement;
import io.vigier.cursorpaging.jpa.filter.AndFilter;
import io.vigier.cursorpaging.jpa.filter.FilterList;
import io.vigier.cursorpaging.jpa.filter.FilterType;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor;
import io.vigier.cursorpaging.jpa.serializer.dto.Cursor.Value;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;

@Slf4j
@Builder
@RequiredArgsConstructor( staticName = "of" )
class FromDtoMapper<E> {

    private static final Map<Cursor.FilterType, FilterType> FILTER_TYPE_MAP = Map.of( //
            Cursor.FilterType.EQ, FilterType.EQUAL_TO, //
            Cursor.FilterType.GT, FilterType.GREATER_THAN, //
            Cursor.FilterType.LT, FilterType.LESS_THAN, //
            Cursor.FilterType.LIKE, FilterType.LIKE, //
            Cursor.FilterType.GE, FilterType.GREATER_THAN_OR_EQUAL_TO, //
            Cursor.FilterType.LE, FilterType.LESS_THAN_OR_EQUAL_TO, //
            Cursor.FilterType.UNRECOGNIZED, FilterType.EQUAL_TO //
    );

    private final Cursor.PageRequest request;
    @Builder.Default
    private final Map<String, Attribute> attributesByName = new HashMap<>();
    private final ConversionService conversionService;
    private final Map<String, RuleFactory> ruleFactories;

    public static <T> FromDtoMapper<T> create( final Consumer<FromDtoMapperBuilder<T>> c ) {
        final var builder = FromDtoMapper.<T>builder();
        c.accept( builder );
        return builder.build();
    }

    public PageRequest<E> map() {
        return PageRequest.<E>builder()
                .positions( positions() )
                .filters( filters() ) //
                .pageSize( request.getPageSize() )
                .enableTotalCount( request.hasTotalCount() )
                .totalCount( request.hasTotalCount() ? request.getTotalCount() : null ) //
                .build();
    }

    private FilterRule filterRuleOf( final Cursor.Rule rule ) {
        final var factory = ruleFactories.get( rule.getName() );
        if ( factory != null ) {
            final Map<String, List<String>> parameters = new HashMap<>();
            rule.getParametersList()
                    .forEach( p -> parameters.put( p.getName(),
                            p.getValuesList().stream().map( Value::getValue ).toList() ) );
            return factory.apply( parameters );
        }
        return null;
    }

    private Collection<Position> positions() {
        return request.getPositionsList().stream().map( this::positionOf ).toList();
    }

    private FilterList filters() {
        return fromFilterListDto( request.getFilters() );
    }

    private FilterList fromFilterListDto( final Cursor.FilterList dto ) {
        final List<QueryElement> filters = new LinkedList<>();

        filters.addAll( dto.getFiltersList().stream().map( this::fromFilterDto ).toList() );
        filters.addAll( dto.getFilterListsList().stream().map( this::fromFilterListDto ).toList() );
        filters.addAll( dto.getRulesList().stream().map( this::filterRuleOf ).toList() );

        return switch ( dto.getType() ) {
            case AND, UNRECOGNIZED -> AndFilter.of( filters );
            case OR -> OrFilter.of( filters );
        };
    }

    private Filter fromFilterDto( final Cursor.Filter dto ) {
        final var attribute = attributeOf( dto.getAttribute() );
        final var values = valueListOf( attribute, dto.getValuesList() );
        return Filter.builder().attribute( attribute ).values( values ).type( getFilterType( dto ) )
                .build();
    }

    private FilterType getFilterType( final Cursor.Filter dto ) {
        return FILTER_TYPE_MAP.get( dto.getType() );
    }

    private List<? extends Comparable<?>> valueListOf( final Attribute attribute,
            final List<Cursor.Value> valuesList ) {
        return valuesList.stream().map( v -> {
            final Comparable<?> converted = valueOf( attribute, v );
            log.trace( "Converted: {} into {} (value={})", v.getClass().getSimpleName(),
                    (converted != null ? converted.getClass().getSimpleName() : null), converted );
            return converted;
        } ).toList();
    }

    private <T extends Comparable<? super T>> T valueOf( final Attribute attribute, final Cursor.Value value ) {
        if ( value.getValue().isEmpty() ) {
            return null;
        }
        try {
            return conversionService.<T>convert( value.getValue(), attribute.type() );
        } catch ( final ConverterNotFoundException e ) {
            throw new SerializerException(
                    "Cannot convert value: '%s' (type: %s) to type: '%s' for attribute: %s".formatted( value.getValue(),
                            value.getValue().getClass().getName(), attribute.type(), attribute.name() ) );
        }
    }

    private Position positionOf( final Cursor.Position position ) {
        final var attribute = attributeOf( position.getAttribute() );

        return Position.create( b -> b.attribute( attribute )
                .value( valueOf( attribute, position.getValue() ) )
                .nextValue( valueOf( attribute, position.getNextValue() ) )
                .order( switch ( position.getOrder() ) {
                    case ASC -> Order.ASC;
                    case DESC -> Order.DESC;
                    case UNRECOGNIZED -> throw new IllegalArgumentException( "Unrecognized order" );
                } )
                .reversed( position.getReversed() ) );
    }

    private Attribute attributeOf( final Cursor.Attribute attribute ) {
        final var cursorAttribute = attributesByName.get( attribute.getName() );
        if ( cursorAttribute == null ) {
            throw new IllegalArgumentException( "No attribute found for name: " + attribute.getName() );
        }
        return cursorAttribute;
    }

}
