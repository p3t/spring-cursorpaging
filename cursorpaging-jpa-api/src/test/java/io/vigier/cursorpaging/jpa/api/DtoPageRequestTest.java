package io.vigier.cursorpaging.jpa.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoAndFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoEqFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoFilterList;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoGtFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoLikeFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoLtFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoOrFilter;
import io.vigier.cursorpaging.jpa.filter.EqualFilter;
import io.vigier.cursorpaging.jpa.filter.GreaterThanFilter;
import io.vigier.cursorpaging.jpa.filter.LessThanFilter;
import io.vigier.cursorpaging.jpa.filter.LikeFilter;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class DtoPageRequestTest {

    @Test
    void toPageRequest() {
    }

    @Test
    void shouldDesrerializeFromJson() throws Exception {
        String json = """
                {
                    "orderBy": {
                        "id": "ASC"
                    },
                    "filterBy": {
                        "AND": [
                            {
                                "GT": {
                                    "id": [
                                        "666"
                                    ]
                                }
                            }
                        ]
                    },
                    "pageSize": 10,
                    "withTotalCount": false
                }
                """;

        DtoPageRequest request = new ObjectMapper().readValue( json, DtoPageRequest.class );
        assertThat( request.getOrderBy() ).containsExactly( Map.entry( "id", Order.ASC ) );
        assertThat( request.getFilterBy() ).isNotNull().satisfies( fl -> {
            assertThat( fl ).isInstanceOf( DtoAndFilter.class );
            assertThat( ((DtoFilterList) fl).getFilters() ).hasSize( 1 ).first().satisfies( gtf -> {
                assertThat( gtf ).isInstanceOf( DtoGtFilter.class );
                assertThat( ((DtoGtFilter) gtf).getAttribute() ).isEqualTo( "id" );
                assertThat( ((DtoGtFilter) gtf).getValues() ).contains( "666" );
            } );
        } );
        assertThat( request.getPageSize() ).isEqualTo( 10 );

    }

    @Test
    void shouldSerializeDtoPageRequestsToJson() throws JsonProcessingException {
        var request = DtoPageRequest.builder()
                .pageSize( 10 )
                .orderBy( Map.of( "id", Order.ASC ) )
                .filterBy( DtoAndFilter.builder()
                        .filter( DtoGtFilter.builder().attribute( "id" ).value( "666" )
                                .build() )
                        .filter( DtoOrFilter.builder()
                                .filter( DtoEqFilter.builder().attribute( "super" ).value( "true" )
                                        .build() )
                                .filter( DtoLikeFilter.builder().attribute( "name" ).value( "4711" )
                                        .build() )
                                .filter( DtoLtFilter.builder().attribute( "priority" ).value( "0815" )
                                        .build() )
                                .build() )
                        .build() )
                .build();

        log.info( new ObjectMapper().writeValueAsString( request ) );
    }

    @Test
    void shouldGenerateValidPageRequests() {
        var request = DtoPageRequest.builder()
                .pageSize( 10 )
                .orderBy( Map.of( "id", Order.ASC ) )
                .filterBy( DtoAndFilter.builder()
                        .filter( DtoGtFilter.builder().attribute( "id" ).value( "666" )
                                .build() )
                        .filter( DtoOrFilter.builder()
                                .filter( DtoEqFilter.builder().attribute( "super" ).value( "true" )
                                        .build() )
                                .filter( DtoLikeFilter.builder().attribute( "name" ).value( "4711" )
                                        .build() )
                                .filter( DtoLtFilter.builder().attribute( "priority" ).value( "0815" )
                                        .build() )
                                .build() )
                        .build() )
                .build();

        var pageRequest = request.toPageRequest( s -> switch ( s ) {
            case "id" -> Attribute.of( "id", Long.class );
            case "super" -> Attribute.of( "super", Boolean.class );
            case "name" -> Attribute.of( "name", String.class );
            case "priority" -> Attribute.of( "priority", Integer.class );
            default -> throw new IllegalArgumentException( "Unknown attribute: " + s );
        } );

        assertThat( pageRequest.pageSize() ).isEqualTo( 10 );
        assertThat( pageRequest.filters() ).hasSize( 2 );
        assertThat( pageRequest.filters().filters().get( 0 ) ).isInstanceOf( GreaterThanFilter.class );
        assertThat( pageRequest.filters().filters().get( 1 ) ).isInstanceOf( OrFilter.class ).satisfies( of -> {
            assertThat( ((OrFilter) of).filters() ).hasSize( 3 );
            assertThat( ((OrFilter) of).filters().get( 0 ) ).isInstanceOf( EqualFilter.class );
            assertThat( ((OrFilter) of).filters().get( 1 ) ).isInstanceOf( LikeFilter.class );
            assertThat( ((OrFilter) of).filters().get( 2 ) ).isInstanceOf( LessThanFilter.class );
        } );
    }
}