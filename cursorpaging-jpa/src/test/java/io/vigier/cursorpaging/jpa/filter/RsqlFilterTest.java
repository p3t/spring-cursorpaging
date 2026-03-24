package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.Filters;
import io.vigier.cursorpaging.jpa.QueryElement;
import io.vigier.cursorpaging.jpa.SingleAttribute;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsqlFilterTest {

    // -- helpers to build the expected QueryElements -----------------------------------------------

    private static Filter eq( final String name, final String value ) {
        return Filters.attribute( Attribute.of( name, String.class ) ).equalTo( value );
    }

    private static Filter in( final String name, final String... values ) {
        return Filters.attribute( Attribute.of( name, String.class ) ).in( List.of( values ) );
    }

    private static Filter gt( final String name, final String value ) {
        return Filters.attribute( Attribute.of( name, String.class ) ).greaterThan( value );
    }

    private static Filter gte( final String name, final String value ) {
        return Filters.attribute( Attribute.of( name, String.class ) ).greaterThanOrEqualTo( value );
    }

    private static Filter lt( final String name, final String value ) {
        return Filters.attribute( Attribute.of( name, String.class ) ).lessThan( value );
    }

    private static Filter lte( final String name, final String value ) {
        return Filters.attribute( Attribute.of( name, String.class ) ).lessThanOrEqualTo( value );
    }

    // -- test data ---------------------------------------------------------------------------------

    static Stream<Arguments> rsqlExpressions() {
        return Stream.of( //
                // simple comparisons
                Arguments.of( "name==John", eq( "name", "John" ) ),  //
                Arguments.of( "age=gt=18", gt( "age", "18" ) ), //
                Arguments.of( "age=ge=19", gte( "age", "19" ) ), //
                Arguments.of( "sum=lt=42", lt( "sum", "42" ) ),//
                Arguments.of( "age=le=18", lte( "age", "18" ) ), //

                // in operator
                Arguments.of( "status=in=(active,pending)", in( "status", "active", "pending" ) ),

                // AND (;)
                Arguments.of( "name==John;age=gt=18", Filters.and( eq( "name", "John" ), gt( "age", "18" ) ) ),

                // OR (,)
                Arguments.of( "name==John,name==Jane", Filters.or( eq( "name", "John" ), eq( "name", "Jane" ) ) ),

                // combined AND & OR
                Arguments.of( "name==John;age=gt=18,status==active",
                        Filters.or( Filters.and( eq( "name", "John" ), gt( "age", "18" ) ),
                                eq( "status", "active" ) ) ),

                // dotted path (nested attribute)
                Arguments.of( "parent.child==value", Filters.attribute(
                        Attribute.of( SingleAttribute.of( "parent", String.class ),
                                SingleAttribute.of( "child", String.class ) ) ).equalTo( "value" ) ) );
    }

    // -- parameterized test ------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource( "rsqlExpressions" )
    void shouldParseRsqlExpression( final String rsql, final QueryElement expected ) {
        final var result = RsqlFilter.of( rsql );
        assertThat( result ).isEqualTo( expected );
    }

    // -- edge-case tests ---------------------------------------------------------------------------

    @Test
    void shouldRejectNotEqualOperator() {
        assertThatThrownBy( () -> RsqlFilter.of( "name!=John" ) ).isInstanceOf( UnsupportedOperationException.class )
                .hasMessageContaining( "not supported" );
    }

    @Test
    void shouldRejectNotInOperator() {
        assertThatThrownBy( () -> RsqlFilter.of( "name=out=(A,B)" ) ).isInstanceOf(
                UnsupportedOperationException.class ).hasMessageContaining( "not supported" );
    }

    @Test
    void shouldRejectInvalidRsql() {
        assertThatThrownBy( () -> RsqlFilter.of( "==invalid" ) ).isInstanceOf(
                cz.jirutka.rsql.parser.RSQLParserException.class );
    }
}

