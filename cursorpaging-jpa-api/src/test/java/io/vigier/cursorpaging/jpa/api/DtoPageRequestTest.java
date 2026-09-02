package io.vigier.cursorpaging.jpa.api;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.QueryElement;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoAndFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoEqFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoFilterElement;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoFilterList;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoGeFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoGtFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoLeFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoLikeFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoLtFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoNeFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoNotLikeFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoOrFilter;
import io.vigier.cursorpaging.jpa.filter.FilterType;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class DtoPageRequestTest {

    @Test
    void shouldDeserializeFromJson() {
        final String json = """
                {
                    "orderBy": {
                        "id": "ASC"
                    },
                    "filterBy": {
                        "AND": [
                            { "EQ": { "super": [ "true" ] } },
                            { "NE": { "id": [ "667", "668" ] } },
                            { "GT": { "id": [ "666" ] } },
                            { "GE": { "id": [ "665" ] } },
                            { "LT": { "id": [ "778" ] } },
                            { "LE": { "id": [ "777" ] } },
                            { "LIKE": { "name": [ "471*" ] } },
                            { "NLIKE": { "name": [ "*815" ] } }
                        ]
                    },
                    "pageSize": 10,
                    "withTotalCount": false
                }
                """;

        final var mapper = JsonMapper.builder()
                .build();
        final DtoPageRequest request = mapper.readValue( json, DtoPageRequest.class );
        assertThat( request.getOrderBy() ).containsExactly( Map.entry( "id", Order.ASC ) );
        assertThat( request.getFilterBy() ).isNotNull()
                .isInstanceOf( DtoAndFilter.class );
        assertThat( request.getFilterBy()
                .getFilters() ).satisfiesExactly( //
                f -> filterIs( f, DtoEqFilter.class, "super", "true" ), //
                f -> filterIs( f, DtoNeFilter.class, "id", "667", "668" ), //
                f -> filterIs( f, DtoGtFilter.class, "id", "666" ), //
                f -> filterIs( f, DtoGeFilter.class, "id", "665" ), //
                f -> filterIs( f, DtoLtFilter.class, "id", "778" ), //
                f -> filterIs( f, DtoLeFilter.class, "id", "777" ), //
                f -> filterIs( f, DtoLikeFilter.class, "name", "471*" ), //
                f -> filterIs( f, DtoNotLikeFilter.class, "name", "*815" ) );
        assertThat( request.getPageSize() ).isEqualTo( 10 );
    }

    private static void filterIs( final DtoFilterElement element, final Class<? extends DtoFilter> type,
            final String attribute, final String... values ) {
        assertThat( element ).isInstanceOf( type );
        assertThat( ((DtoFilter) element).getAttribute() ).isEqualTo( attribute );
        assertThat( ((DtoFilter) element).getValues() ).containsExactly( values );
    }

    @Test
    void shouldDeserializeWithOrRootList() {
        final String json = """
                {
                    "orderBy": {
                        "id": "ASC"
                    },
                    "filterBy": {
                        "OR": [
                            { "GT": { "id": [ 666 ] } },
                            { "GE": { "id": [ 666 ] } }
                        ]
                    },
                    "pageSize": 10,
                    "withTotalCount": false
                }
                """;
        final var mapper = JsonMapper.builder()
                .build();
        final DtoPageRequest request = mapper.readValue( json, DtoPageRequest.class );
        assertThat( request.getOrderBy() ).containsExactly( Map.entry( "id", Order.ASC ) );
        assertThat( request.getFilterBy() ).isNotNull()
                .satisfies( fl -> {
                    assertThat( fl ).isInstanceOf( DtoOrFilter.class );
                    assertThat( ((DtoFilterList) fl).getFilters() ).hasSize( 2 );
                } );

        final var pageRequest = request.toPageRequest( DtoPageRequestTest::getAttribute );
        assertThat( pageRequest.filters() ).hasSize( 2 )
                .isInstanceOf( OrFilter.class )
                .satisfies( orFilter -> {
                    final var iterator = orFilter.iterator();
                    assertThat( iterator.next() ).satisfies( f -> operationIs( f, FilterType.GREATER_THAN ) );
                    assertThat( iterator.next() ).satisfies(
                            f -> operationIs( f, FilterType.GREATER_THAN_OR_EQUAL_TO ) );
                } );
    }

    private static void operationIs( final QueryElement f, final FilterType type ) {
        assertThat( ((Filter) f).operation() ).isEqualTo( type );
    }

    @Test
    void shouldSerializeDtoPageRequestsToJson() {
        final var request = DtoPageRequest.builder()
                .pageSize( 10 )
                .orderBy( Map.of( "id", Order.ASC ) )
                .filterBy( DtoAndFilter.builder()
                        .filter( DtoEqFilter.builder()
                                .attribute( "super" )
                                .value( "true" )
                                .build() )
                        .filter( DtoNeFilter.builder()
                                .attribute( "id" )
                                .value( "667" )
                                .value( "668" )
                                .build() )
                        .filter( DtoGtFilter.builder()
                                .attribute( "id" )
                                .value( "666" )
                                .build() )
                        .filter( DtoGeFilter.builder()
                                .attribute( "id" )
                                .value( "665" )
                                .build() )
                        .filter( DtoOrFilter.builder()
                                .filter( DtoLtFilter.builder()
                                        .attribute( "id" )
                                        .value( "778" )
                                        .build() )
                                .filter( DtoLeFilter.builder()
                                        .attribute( "id" )
                                        .value( "777" )
                                        .build() )
                                .filter( DtoLikeFilter.builder()
                                        .attribute( "name" )
                                        .value( "4711*" )
                                        .build() )
                                .filter( DtoNotLikeFilter.builder()
                                        .attribute( "name" )
                                        .value( "*0815" )
                                        .build() )
                                .build() )
                        .build() )
                .build();

        final var jsonMapper = JsonMapper.builder()
                .build();
        final var json = jsonMapper.writeValueAsString( request );
        log.info( json );
        final var nodes = jsonMapper.readTree( json );
        assertThat( nodes.get( "pageSize" )
                .intValue() ).isEqualTo( 10 );
        assertThat( nodes.get( "orderBy" )
                .get( "id" )
                .stringValue() ).isEqualTo( "ASC" );
        final var filterBy = nodes.get( "filterBy" );
        assertThat( filterBy.get( "AND" ) ).isNotNull();
        final var andArray = filterBy.get( "AND" );
        assertThat( andArray ).hasSize( 5 );
        filterNodeIs( andArray.get( 0 ), "EQ", "super", "true" );
        filterNodeIs( andArray.get( 1 ), "NE", "id", "667", "668" );
        filterNodeIs( andArray.get( 2 ), "GT", "id", "666" );
        filterNodeIs( andArray.get( 3 ), "GE", "id", "665" );

        final var orArray = andArray.get( 4 )
                .get( "OR" );
        assertThat( orArray ).isNotNull()
                .hasSize( 4 );
        filterNodeIs( orArray.get( 0 ), "LT", "id", "778" );
        filterNodeIs( orArray.get( 1 ), "LE", "id", "777" );
        filterNodeIs( orArray.get( 2 ), "LIKE", "name", "4711*" );
        filterNodeIs( orArray.get( 3 ), "NLIKE", "name", "*0815" );

        assertThat( jsonMapper.readValue( json, DtoPageRequest.class ) ).isEqualTo( request );
    }

    private static void filterNodeIs( final JsonNode node, final String operation, final String attribute,
            final String... values ) {
        assertThat( node.propertyNames() ).containsExactly( operation );
        final var filterNode = node.get( operation );
        assertThat( filterNode.propertyNames() ).containsExactly( attribute );
        assertThat( filterNode.get( attribute )
                .valueStream()
                .map( JsonNode::stringValue ) ).containsExactly( values );
    }

    @Test
    void shouldGenerateValidPageRequests() {
        final var request = DtoPageRequest.builder()
                .pageSize( 10 )
                .orderBy( Map.of( "id", Order.ASC ) )
                .filterBy( DtoAndFilter.builder()
                        .filter( DtoGtFilter.builder()
                                .attribute( "id" )
                                .value( "666" )
                                .build() )
                        .filter( DtoOrFilter.builder()
                                .filter( DtoEqFilter.builder()
                                        .attribute( "super" )
                                        .value( "true" )
                                        .build() )
                                .filter( DtoLikeFilter.builder()
                                        .attribute( "name" )
                                        .value( "4711" )
                                        .build() )
                                .filter( DtoLtFilter.builder()
                                        .attribute( "priority" )
                                        .value( "0815" )
                                        .build() )
                                .build() )
                        .build() )
                .build();

        final var pageRequest = request.toPageRequest( DtoPageRequestTest::getAttribute );

        assertThat( pageRequest.pageSize() ).isEqualTo( 10 );
        assertThat( pageRequest.filters() ).hasSize( 2 );
        assertThat( pageRequest.filters()
                .filters()
                .get( 0 ) ).satisfies( f -> operationIs( f, FilterType.GREATER_THAN ) );
        assertThat( pageRequest.filters()
                .filters()
                .get( 1 ) ).isInstanceOf( OrFilter.class )
                .satisfies( of -> {
                    assertThat( ((OrFilter) of).filters() ).hasSize( 3 );
                    assertThat( ((OrFilter) of).filters()
                            .get( 0 ) ).satisfies( f -> operationIs( f, FilterType.EQUAL_TO ) );
                    assertThat( ((OrFilter) of).filters()
                            .get( 1 ) ).satisfies( f -> operationIs( f, FilterType.LIKE ) );
                    assertThat( ((OrFilter) of).filters()
                            .get( 2 ) ).satisfies( f -> operationIs( f, FilterType.LESS_THAN ) );
                } );
    }

    @Test
    void shouldCreateRequestsWithTheBuilderConsumer() {
        final var request = DtoPageRequest.create( b -> b.pageSize( 42 )
                .orderBy( Map.of( "id", Order.DESC ) )
                .withTotalCount( true ) );

        assertThat( request.getPageSize() ).isEqualTo( 42 );
        assertThat( request.isWithTotalCount() ).isTrue();
        assertThat( request.getOrderBy() ).containsExactly( Map.entry( "id", Order.DESC ) );
        assertThat( request.getFilterBy() ).isInstanceOf( DtoAndFilter.class );
    }

    @Test
    void shouldAddOrderByOnlyWhenNotAlreadyPresent() {
        final var request = DtoPageRequest.create( b -> b.orderBy( new HashMap<>( Map.of( "id", Order.ASC ) ) ) );

        request.addOrderByIfAbsent( "id", Order.DESC );
        request.addOrderByIfAbsent( "name", Order.DESC );

        assertThat( request.getOrderBy() ).containsExactlyInAnyOrderEntriesOf(
                Map.of( "id", Order.ASC, "name", Order.DESC ) );
    }

    @Test
    void shouldGeneratePageRequestsForTheNegatingFilters() {
        final var request = DtoPageRequest.builder()
                .orderBy( Map.of( "id", Order.ASC ) )
                .filterBy( DtoAndFilter.builder()
                        .filter( DtoNeFilter.builder()
                                .attribute( "id" )
                                .value( "667" )
                                .build() )
                        .filter( DtoLeFilter.builder()
                                .attribute( "id" )
                                .value( "777" )
                                .build() )
                        .filter( DtoNotLikeFilter.builder()
                                .attribute( "name" )
                                .value( "*0815" )
                                .build() )
                        .build() )
                .build();

        final var pageRequest = request.toPageRequest( DtoPageRequestTest::getAttribute );

        assertThat( pageRequest.filters()
                .filters() ).satisfiesExactly( //
                f -> operationIs( f, FilterType.NOT_EQUAL_TO ), //
                f -> operationIs( f, FilterType.LESS_THAN_OR_EQUAL_TO ), //
                f -> operationIs( f, FilterType.NOT_LIKE ) );
    }

    @Test
    void shouldRejectAMissingFilterList() {
        final var request = DtoPageRequest.builder()
                .orderBy( Map.of() )
                .filterBy( null )
                .build();

        assertThatThrownBy( () -> request.toPageRequest( DtoPageRequestTest::getAttribute ) ) //
                .isInstanceOf( IllegalStateException.class )
                .hasMessage( "Unknown filter element: null" );
    }

    @Test
    void shouldRejectUnknownFilterElements() {
        final DtoFilterElement unknown = new DtoFilterElement() {
        };
        final var request = DtoPageRequest.builder()
                .orderBy( Map.of() )
                .filterBy( DtoAndFilter.builder()
                        .filter( unknown )
                        .build() )
                .build();

        assertThatThrownBy( () -> request.toPageRequest( DtoPageRequestTest::getAttribute ) ) //
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "Unknown filter element: " + unknown.getClass()
                        .getName() );
    }

    @Test
    void shouldProvideTheContentOfAFilterList() {
        final DtoFilterElement filter = DtoEqFilter.builder()
                .attribute( "id" )
                .value( "666" )
                .build();
        final var filterList = new DtoAndFilter();
        filterList.setContent( Map.of( "AND", List.of( filter ) ) );

        assertThat( filterList.getFilters() ).containsExactly( filter );
        assertThat( filterList.size() ).isEqualTo( 1 );
        assertThat( filterList.iterator() ).toIterable()
                .containsExactly( filter );

        final var visited = new ArrayList<DtoFilterElement>();
        filterList.forEach( visited::add );
        assertThat( visited ).containsExactly( filter );
    }

    @Test
    void shouldIgnoreNonListValuesWhenSettingAnAttribute() {
        final var filter = new DtoEqFilter();
        filter.setAttribute( "id", "666" );

        assertThat( filter.getAttribute() ).isEqualTo( "id" );
        assertThat( filter.getValues() ).isEmpty();
    }

    private static Attribute getAttribute( final String s ) {
        return switch ( s ) {
            case "id" -> Attribute.of( "id", Long.class );
            case "super" -> Attribute.of( "super", Boolean.class );
            case "name" -> Attribute.of( "name", String.class );
            case "priority" -> Attribute.of( "priority", Integer.class );
            default -> throw new IllegalArgumentException( "Unknown attribute: " + s );
        };
    }
}