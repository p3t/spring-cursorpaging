package io.vigier.cursorpaging.jpa.rsql.filter;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.AttributeResolver;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.Filters;
import io.vigier.cursorpaging.jpa.QueryElement;
import io.vigier.cursorpaging.jpa.filter.FilterType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Visitor that converts an RSQL AST into a {@link QueryElement} tree (using {@link Filters}).
 * <p>
 * All comparison operators of the default RSQL/FIQL operator set are supported: {@code ==}, {@code !=}, {@code =gt=} /
 * {@code >}, {@code =ge=} / {@code >=}, {@code =lt=} / {@code <}, {@code =le=} / {@code <=}, {@code =in=} and
 * {@code =out=}, combined with {@code ;} (and), {@code ,} (or) and parentheses for grouping.
 * <p>
 * Arguments of {@code ==} and {@code !=} on string attributes may contain {@code *} wildcards, which are translated
 * into a {@link FilterType#LIKE} / {@link FilterType#NOT_LIKE} condition (see {@link #toLikePattern(String)}).
 * <p>
 * Values are converted to the attribute's target type via {@link DefaultConversionService} (extended with common
 * temporal converters).
 */
class RsqlFilterVisitor implements RSQLVisitor<QueryElement, Void> {

    private static final DefaultConversionService CONVERSION_SERVICE = createConversionService();

    private static final char WILDCARD = '*';
    private static final char ESCAPE = '\\';
    private static final char SQL_WILDCARD = '%';
    private static final String ESCAPED_WILDCARD = "" + ESCAPE + WILDCARD;

    private static final Map<ComparisonOperator, FilterType> OPERATOR_MAP = Map.of( //
            RSQLOperators.EQUAL, FilterType.EQUAL_TO, //
            RSQLOperators.NOT_EQUAL, FilterType.NOT_EQUAL_TO, //
            RSQLOperators.IN, FilterType.EQUAL_TO, //
            RSQLOperators.NOT_IN, FilterType.NOT_EQUAL_TO, //
            RSQLOperators.GREATER_THAN, FilterType.GREATER_THAN, //
            RSQLOperators.GREATER_THAN_OR_EQUAL, FilterType.GREATER_THAN_OR_EQUAL_TO, //
            RSQLOperators.LESS_THAN, FilterType.LESS_THAN, //
            RSQLOperators.LESS_THAN_OR_EQUAL, FilterType.LESS_THAN_OR_EQUAL_TO );

    /**
     * Operators for which a {@code *} in the argument is interpreted as a wildcard.
     */
    private static final Map<ComparisonOperator, FilterType> WILDCARD_OPERATOR_MAP = Map.of( //
            RSQLOperators.EQUAL, FilterType.LIKE, //
            RSQLOperators.NOT_EQUAL, FilterType.NOT_LIKE );

    private final AttributeResolver resolver;

    RsqlFilterVisitor( final AttributeResolver resolver ) {
        this.resolver = resolver;
    }

    @Override
    public QueryElement visit( final AndNode node, final Void unused ) {
        final var elements = node.getChildren()
                .stream()
                .map( n -> n.accept( this ) )
                .toList();
        return Filters.and( elements );
    }

    @Override
    public QueryElement visit( final OrNode node, final Void unused ) {
        final var elements = node.getChildren()
                .stream()
                .map( n -> n.accept( this ) )
                .toList();
        return Filters.or( elements );
    }

    @Override
    public QueryElement visit( final ComparisonNode node, final Void unused ) {
        final var filterType = OPERATOR_MAP.get( node.getOperator() );
        if ( filterType == null ) {
            throw new UnsupportedOperationException( "Operator not supported: " + node.getOperator()
                    .getSymbol() );
        }
        final var attribute = resolver.resolve( node.getSelector() );

        if ( attribute.type() == String.class && WILDCARD_OPERATOR_MAP.containsKey( node.getOperator() ) ) {
            return wildcardAwareFilter( node, attribute, filterType );
        }

        final var values = convertValues( node.getArguments(), attribute.type() );
        return Filter.create( b -> b.attribute( attribute )
                .type( filterType )
                .values( values ) );
    }

    /**
     * Creates the filter for an {@code ==} / {@code !=} comparison on a string attribute: if any argument contains an
     * unescaped {@code *}, a {@code LIKE} / {@code NOT LIKE} filter is created, otherwise the plain comparison is kept
     * and only the {@code \*} escape sequences are resolved.
     */
    private QueryElement wildcardAwareFilter( final ComparisonNode node, final Attribute attribute,
            final FilterType filterType ) {
        final var arguments = node.getArguments();
        if ( arguments.stream()
                .anyMatch( RsqlFilterVisitor::containsWildcard ) ) {
            final var patterns = arguments.stream()
                    .map( RsqlFilterVisitor::toLikePattern )
                    .toList();
            return Filter.create( b -> b.attribute( attribute )
                    .type( WILDCARD_OPERATOR_MAP.get( node.getOperator() ) )
                    .values( patterns ) );
        }
        final var values = arguments.stream()
                .map( RsqlFilterVisitor::unescapeWildcard )
                .toList();
        return Filter.create( b -> b.attribute( attribute )
                .type( filterType )
                .values( values ) );
    }

    /**
     * Checks whether the argument contains at least one unescaped {@code *}.
     */
    @SuppressWarnings( "java:S127" ) // increment in loop unavoidable
    private static boolean containsWildcard( final String argument ) {
        for ( int i = 0; i < argument.length(); i++ ) {
            final var c = argument.charAt( i );
            if ( c == ESCAPE && i + 1 < argument.length() && argument.charAt( i + 1 ) == WILDCARD ) {
                i++; // skip the escaped wildcard
            } else if ( c == WILDCARD ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces the escape sequence {@code \*} by a literal {@code *}. All other backslashes are left untouched.
     *
     * @param argument the RSQL argument
     * @return the argument without wildcard escapes
     */
    private static String unescapeWildcard( final String argument ) {
        return argument.contains( ESCAPED_WILDCARD ) ? argument.replace( ESCAPED_WILDCARD, String.valueOf( WILDCARD ) )
                                                     : argument;
    }

    /**
     * Translates an RSQL argument into an SQL {@code LIKE} pattern: an unescaped {@code *} becomes {@code %}, a
     * {@code \*} becomes a literal {@code *}. Note that {@code %} and {@code _} are passed through unchanged and
     * therefore keep their SQL wildcard meaning.
     *
     * @param argument the RSQL argument
     * @return the LIKE pattern
     */
    @SuppressWarnings( "java:S127" ) // increment in loop unavoidable
    private static String toLikePattern( final String argument ) {
        final var pattern = new StringBuilder( argument.length() );
        for ( int i = 0; i < argument.length(); i++ ) {
            final var c = argument.charAt( i );
            if ( c == ESCAPE && i + 1 < argument.length() && argument.charAt( i + 1 ) == WILDCARD ) {
                pattern.append( argument.charAt( ++i ) );
            } else if ( c == WILDCARD ) {
                pattern.append( SQL_WILDCARD );
            } else {
                pattern.append( c );
            }
        }
        return pattern.toString();
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

    private static DefaultConversionService createConversionService() {
        final var service = new DefaultConversionService();
        service.addConverter( String.class, Instant.class, Instant::parse );
        return service;
    }
}
