package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.filter.AndFilter;
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

    @Test
    void shouldFindFilterByAttribute() {
        final var pageRequest = PageRequest.create( b -> b.asc( Attribute.of( "id", Long.class ) )
                .filter( Filters.attribute( "test", String.class ).equalTo( "value" ) )
                .filter( Filters.and( Filters.attribute( "test", String.class ).equalTo( "value2" ),
                        Filters.attribute( "test2", String.class ).equalTo( "value3" ) ) ) );

        assertThat( pageRequest.firstFilterWith( Attribute.of( "test", String.class ) ) ).isPresent();
        assertThat( pageRequest.firstFilterWith( Attribute.of( "test", Long.class ) ) ).isEmpty();
        assertThat( pageRequest.firstFilterWith( Attribute.of( "test2", String.class ) ) ).isPresent()
                .get()
                .isInstanceOf( AndFilter.class );
    }

    @Test
    void shouldAcceptNullAsFilter() {
        final var pageRequest = PageRequest.create( b -> b.asc( Attribute.of( "id", Long.class ) ).filter( null ) );

        assertThat( pageRequest.filters() ).isEmpty();
    }
}