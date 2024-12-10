package io.vigier.cursorpaging.jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource( strings = { "  ", "\t", "\n" } )
    void shouldIgnoreEmptyFilter( final String value ) {
        final var pageRequest = PageRequest.create( b -> b.asc( Attribute.of( "id", Long.class ) )
                .filter( Filter.create( f -> f.attribute( Attribute.of( "test", String.class ) ).equalTo( value ) ) ) );

        assertThat( pageRequest.filters() ).isEmpty();
    }

    @Test
    void shouldAddPositionAndFilterIfValuePresent() {
        final var pageRequest = PageRequest.create( b -> b.asc( Attribute.of( "id", Long.class ) )
                .filter( Filters.attribute( "test", String.class ).equalTo( "value" ) ) );

        assertThat( pageRequest.filters() ).hasSize( 1 );
        assertThat( pageRequest.positions() ).hasSize( 1 ).first().satisfies( p -> {
            assertThat( p.attribute().name() ).isEqualTo( "id" );
            assertThat( p.order() ).isEqualTo( Order.ASC );
        } );
    }

    @Test
    void shouldFailWhenRequestWithoutOrder() {
        assertThatThrownBy( () -> PageRequest.create(
                b -> b.filter( Filters.attribute( "test", String.class ).equalTo( "value" ) ) ) ).isInstanceOf(
                        IllegalArgumentException.class )
                .hasMessageContaining(
                        "at least one order-attribute (asc/desc) for determine the position of the page start is required" );
    }
}