package io.vigier.cursorpaging.example.webapp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static io.vigier.cursorpaging.example.webapp.api.controller.DataRecordController.PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest( properties = "spring.autoconfigure.exclude=org.springdoc.core.configuration.SpringDocHateoasConfiguration" )
@Slf4j
class ExampleWebApplicationTests {

    private static final String PATH_RSQL_QUERY = PATH + "/rsql";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgresqlContainer = postgresContainer();

    @Autowired
    private MockMvc mockMvc;

    @SneakyThrows
    private static PostgreSQLContainer<?> postgresContainer() {
        final PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse( "postgres:latest" ) );
        container.start();
        log.info( "PostgreSQL exposed ports: {}", container.getExposedPorts() );
        waitForConnectionReady( container );

        return container;
    }

    @Retryable( includes = SQLException.class, maxRetries = 5, delay = 1000 )
    private static void waitForConnectionReady( final PostgreSQLContainer<?> container ) throws SQLException {
        final String jdbcUrl = container.getJdbcUrl();
        try ( final Connection con = container.createConnection( jdbcUrl.substring( jdbcUrl.indexOf( '?' ) ) ) ) {
            if ( !con.isValid( 1000 ) ) {
                throw new IllegalStateException( "Cannot connect to: " + jdbcUrl );
            }
            log.info( "Connection successfully to JDBC URL {}", jdbcUrl );
        } catch ( final SQLException e ) {
            log.error( "Error connecting to: " + jdbcUrl, e );
            throw e;
        }
    }

    @Test
    void contextLoads() {
        // Fails when Spring's ApplicationContext cannot be created
    }

    @Test
    void shouldAcceptEmptyCursor() throws Exception {
        mockMvc.perform( get( "/api/v1/datarecord" ) ).andExpect( status().isOk() );
    }

    /**
     * TestDataService generates 500 records. Names cycle through 26 NATO phonetic alphabet names, so:
     * <ul>
     *   <li>"Alpha" (index 0) appears 20 times (500/26 = 19 remainder 6, indices 0-5 get 20)</li>
     *   <li>"Zulu" (index 25) appears 19 times</li>
     *   <li>createdAt starts at 1999-01-03 and increments by 1 day per record</li>
     * </ul>
     */
    static Stream<Arguments> queryDataRecordsArguments() {
        return Stream.of(
                // rsqlQuery, sort, pageSize, expectedCount, hasNextPage, description
                Arguments.of( "name==Alpha", List.of( "NAME:ASC", "ID:ASC" ), 5, 5, true,
                        "eq filter with ASC sort, page smaller than total matches -> has next page" ),
                Arguments.of( "name==Zulu", List.of( "NAME:ASC", "ID:ASC" ), 20, 19, false,
                        "eq filter for Zulu (19 matches), page size 20 -> no next page" ),
                Arguments.of( "name==Alpha", List.of( "NAME:DESC", "ID:DESC" ), 20, 20, false,
                        "eq filter with DESC sort, exact page size match -> no next page" ),
                Arguments.of( "name==Alpha,name==Bravo", List.of( "NAME:ASC", "ID:ASC" ), 10, 10, true,
                        "OR filter (Alpha or Bravo = 40 matches), page 10 -> has next page" ),
                Arguments.of( "name==Alpha;auditInfo.createdAt=gt=1999-07-01T00:00:00Z",
                        List.of( "CREATED_AT:ASC", "ID:ASC" ), 20, 13, false,
                        "AND filter with date range, expecting Alpha records after 1999-07-01 -> no next page" ),
                Arguments.of( "name==NonExistent", List.of( "NAME:ASC" ), 10, 0, false,
                        "eq filter with no matches -> empty, no next page" ),
                Arguments.of( "name==Alpha", null, 5, 5, true,
                        "eq filter with no sort (defaults to MODIFIED_AT DESC) -> has next page" ),
                Arguments.of( "name=in=(Alpha,Bravo,Charlie)", List.of( "NAME:ASC", "ID:ASC" ), 20, 20, true,
                        "in-list filter (60 matches), page 20 -> has next page" ) );
    }

    @ParameterizedTest( name = "{5}" )
    @MethodSource( "queryDataRecordsArguments" )
    void shouldQueryDataRecordsWithRsql( final String rsqlQuery, final List<String> sort, final int pageSize,
            final int expectedCount, final boolean hasNextPage, final String description ) throws Exception {

        final var requestBuilder = get( PATH_RSQL_QUERY ).param( "q", rsqlQuery )
                .param( "pageSize", String.valueOf( pageSize ) );

        if ( sort != null && !sort.isEmpty() ) {
            requestBuilder.param( "sort", sort.toArray( new String[0] ) );
        }

        final var resultActions = mockMvc.perform( requestBuilder )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$._links.self.href" ).exists() );

        if ( expectedCount > 0 ) {
            resultActions.andExpect( jsonPath( "$._embedded.dtoDataRecordList" ).isArray() )
                    .andExpect( jsonPath( "$._embedded.dtoDataRecordList.length()" ).value( expectedCount ) );
        } else {
            resultActions.andExpect( jsonPath( "$._embedded" ).doesNotExist() );
        }

        if ( hasNextPage ) {
            resultActions.andExpect( jsonPath( "$._links.next.href" ).exists() )
                    .andExpect( jsonPath( "$._links.next.href" ).value( Matchers.containsString( "cursor=" ) ) );
        } else {
            resultActions.andExpect( jsonPath( "$._links.next" ).doesNotExist() );
        }
    }
}
