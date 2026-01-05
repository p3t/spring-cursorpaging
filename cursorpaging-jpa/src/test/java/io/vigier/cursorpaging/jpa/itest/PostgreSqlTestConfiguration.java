package io.vigier.cursorpaging.jpa.itest;

import java.sql.Connection;
import java.sql.SQLException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.Retryable;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Configuration
@TestConfiguration( proxyBeanMethods = false )
@Testcontainers
@Slf4j
public class PostgreSqlTestConfiguration {

    @Bean
    @ServiceConnection
    @SneakyThrows
    PostgreSQLContainer<?> postgresContainer() {
        final PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse( "postgres:latest" ) ).withPassword( "secret" ).withUsername( "admin" );
        container.start();

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

}
