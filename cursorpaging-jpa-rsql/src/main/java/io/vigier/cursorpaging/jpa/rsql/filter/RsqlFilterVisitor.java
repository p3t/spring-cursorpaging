package io.vigier.cursorpaging.jpa.rsql.filter;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.Filters;
import io.vigier.cursorpaging.jpa.QueryElement;
import io.vigier.cursorpaging.jpa.SingleAttribute;
import io.vigier.cursorpaging.jpa.filter.FilterType;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Visitor that converts an RSQL AST into a {@link QueryElement} tree (using {@link Filters}).
 * <p>
 * Values are converted to the attribute's target type via {@link DefaultConversionService} (extended with common
 * temporal converters).
 */
class RsqlFilterVisitor implements RSQLVisitor<QueryElement, Void> {

    /**
     * Default resolver: splits dotted selectors into path segments, all typed as {@link String}.
     */
    static final AttributeResolver DEFAULT_RESOLVER = RsqlFilterVisitor::toAttribute;

    private static final DefaultConversionService CONVERSION_SERVICE = createConversionService();

    private static final Map<ComparisonOperator, FilterType> OPERATOR_MAP = Map.of( //
            RSQLOperators.EQUAL, FilterType.EQUAL_TO, //
            RSQLOperators.IN, FilterType.EQUAL_TO, //
            RSQLOperators.GREATER_THAN, FilterType.GREATER_THAN, //
            RSQLOperators.GREATER_THAN_OR_EQUAL, FilterType.GREATER_THAN_OR_EQUAL_TO, //
            RSQLOperators.LESS_THAN, FilterType.LESS_THAN, //
            RSQLOperators.LESS_THAN_OR_EQUAL, FilterType.LESS_THAN_OR_EQUAL_TO );

    private final AttributeResolver resolver;

    RsqlFilterVisitor( final AttributeResolver resolver ) {
        this.resolver = resolver;
    }

    @Override
    public QueryElement visit( final AndNode node, final Void unused ) {
        final var elements = node.getChildren().stream().map( n -> n.accept( this ) ).toList();
        return Filters.and( elements );
    }

    @Override
    public QueryElement visit( final OrNode node, final Void unused ) {
        final var elements = node.getChildren().stream().map( n -> n.accept( this ) ).toList();
        return Filters.or( elements );
    }

    @Override
    public QueryElement visit( final ComparisonNode node, final Void unused ) {
        final var filterType = OPERATOR_MAP.get( node.getOperator() );
        if ( filterType == null ) {
            throw new UnsupportedOperationException( "Operator not supported: " + node.getOperator().getSymbol() );
        }
        final var attribute = resolver.resolve( node.getSelector() );
        final var values = convertValues( node.getArguments(), attribute.type() );
        return Filter.create( b -> b.attribute( attribute ).type( filterType ).values( values ) );
    }

    private static List<? extends Comparable<?>> convertValues( final List<String> arguments,
            final Class<? extends Comparable<?>> targetType ) {
        if ( targetType == String.class ) {
            return arguments;
        }
        return arguments.stream()
                .map( v -> (Comparable<?>) CONVERSION_SERVICE.convert( v, (Class<?>) targetType ) )
                .toList();
    }

    /**
     * Converts a (possibly dotted) selector like "securityClass.level" into an {@link Attribute} path.
     */
    private static Attribute toAttribute( final String selector ) {
        final var segments = selector.split( "\\." );
        if ( segments.length == 1 ) {
            return Attribute.of( selector, String.class );
        }
        final var path = Arrays.stream( segments )
                .map( name -> SingleAttribute.of( name, String.class ) )
                .toArray( SingleAttribute[]::new );
        return Attribute.of( path );
    }

    private static DefaultConversionService createConversionService() {
        final var service = new DefaultConversionService();
        service.addConverter( String.class, Instant.class, (Converter<String, Instant>) Instant::parse );
        return service;
    }
}
