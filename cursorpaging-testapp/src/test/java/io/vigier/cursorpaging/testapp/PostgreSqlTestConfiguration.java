package io.vigier.cursorpaging.testapp;

import java.sql.Connection;
import java.sql.SQLException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration( proxyBeanMethods = false )
@Testcontainers
@EnableRetry
@Slf4j
public class PostgreSqlTestConfiguration {

    public static void main( final String[] args ) {
        SpringApplication.from( TestApplication::main ).with( PostgreSqlTestConfiguration.class ).run( args );
    }

    @Bean
    @ServiceConnection
    @SneakyThrows
    PostgreSQLContainer<?> postgresContainer() {
        final PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse( "postgres:latest" ) );
        container.start();

        waitForConnectionReady( container );

        return container;
    }

    @Retryable( retryFor = SQLException.class,
            maxAttempts = 5,
            backoff = @org.springframework.retry.annotation.Backoff( delay = 1000 ) )
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
