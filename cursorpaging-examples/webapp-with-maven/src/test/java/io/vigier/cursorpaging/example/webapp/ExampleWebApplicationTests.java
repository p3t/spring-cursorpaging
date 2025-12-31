package io.vigier.cursorpaging.example.webapp;

import java.sql.Connection;
import java.sql.SQLException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest( properties = "spring.autoconfigure.exclude=org.springdoc.core.configuration.SpringDocHateoasConfiguration" )
@Slf4j
class ExampleWebApplicationTests {

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

}
