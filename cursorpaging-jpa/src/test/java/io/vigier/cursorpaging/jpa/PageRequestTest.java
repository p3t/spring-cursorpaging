package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.filter.FilterType;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class PageRequestTest {

    private static class Entity {
        @SuppressWarnings( { "unused", "java:S1068" } )
        private final String name;
        @SuppressWarnings( { "unused", "java:S1068" } )
        private final Long id;

        Entity( final String name, final Long id ) {
            this.name = name;
            this.id = id;
        }
    }

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
                IllegalArgumentException.class ).hasMessageContaining(
                "at least one order-attribute (asc/desc) for determine the position of the page start is required" );
    }

    @Test
    void shouldFindFilterByAttribute() {
        final var test1Attr = Attribute.of( "test", String.class );
        final var test2Attr = Attribute.of( "test2", String.class );
        final var test3Attr = Attribute.of( "test3", String.class );

        final var pageRequest = PageRequest.create( b -> b.asc( Attribute.of( "id", Long.class ) )
                .filter( Filters.attribute( test1Attr ).equalTo( "value" ) )
                .filter( Filters.or( Filters.attribute( test2Attr ).equalTo( "value2" ),  //
                        Filters.attribute( test3Attr ).equalTo( "value3" ) ) ) );

        assertThat( pageRequest.firstFilterWith( test1Attr ) ).isPresent();
        assertThat( pageRequest.firstFilterWith( Attribute.of( "test", Long.class ) ) ).isEmpty();

        assertThat( pageRequest.firstFilterWith( test3Attr ) ).isPresent().get().asInstanceOf( type( Filter.class ) )
                .satisfies( f -> {
                    assertThat( f.attribute() ).isEqualTo( test3Attr );
                    assertThat( f.operation() ).isEqualTo( FilterType.EQUAL_TO );
                } );

        assertThat( pageRequest.firstFilterListWith( test3Attr ) ).isPresent().get()
                .asInstanceOf( type( OrFilter.class ) )
                .satisfies( f -> assertThat( f.attributes() ).contains( test2Attr, test3Attr ) );
    }

    @Test
    void shouldAcceptNullAsFilter() {
        final var pageRequest = PageRequest.create( b -> b.asc( Attribute.of( "id", Long.class ) ).filter( null ) );

        assertThat( pageRequest.filters() ).isEmpty();
    }

    @Test
    void shouldBeFirstPageWithoutPositionValues() {
        final PageRequest<Entity> request = PageRequest.create(
                b -> b.asc( Attribute.of( "name", String.class ) ).asc( Attribute.of( "id", Long.class ) ) );

        assertThat( request.isFirstPage() ).isTrue();
        assertThat( request.withPageSize( 42 ).isFirstPage() ).isTrue();
    }

    @Test
    void shouldNotBeFirstPageForCursorWithNullValue() {
        final PageRequest<Entity> request = PageRequest.<Entity>create(
                        b -> b.asc( Attribute.of( "name", String.class ) ).asc( Attribute.of( "id", Long.class ) ) )
                .positionOf( new Entity( null, 1L ), new Entity( null, 2L ) );

        assertThat( request.isFirstPage() ).isFalse();
        assertThat( request.positions() ).first().satisfies( p -> assertThat( p.value() ).isNull() );
        assertThat( request.toReversed().isFirstPage() ).isFalse();
        assertThat( request.withPageSize( 42 ).isFirstPage() ).isFalse();
    }

    @Test
    void shouldFailForCursorWhereAllValuesAreNull() {
        final PageRequest<Entity> request = PageRequest.create( b -> b.asc( Attribute.of( "name", String.class ) ) );
        final var last = new Entity( null, 1L );
        final var next = new Entity( null, 2L );

        assertThatThrownBy( () -> request.positionOf( last, next ) ).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "[name] have null values" );
    }

    @Test
    void shouldFailForFollowingPageWithoutValues() {
        assertThatThrownBy( () -> PageRequest.create(
                b -> b.asc( Attribute.of( "id", Long.class ) ).firstPage( false ) ) ).isInstanceOf(
                IllegalArgumentException.class ).hasMessageContaining( "do not address a unique record" );
    }

    @Test
    void shouldFailForFirstPageWithValues() {
        assertThatThrownBy( () -> PageRequest.create( b -> b.position( Position.create(
                        p -> p.attribute( Attribute.of( "id", Long.class ) ).order( Order.ASC ).value( 4711L ) ) )
                .firstPage( true ) ) ).isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Cannot create page-request for the first page with position values" );
    }
}
