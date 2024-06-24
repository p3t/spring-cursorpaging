package io.vigier.cursorpaging.example.webapp;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest
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
//		waitForConnectionReady( container );

		return container;
	}

	@Test
	void contextLoads() {

	}

	@Test
	void shouldAcceptEmptyCursor() throws Exception {
		mockMvc.perform( get( "/api/v1/datarecord" ) ).andExpect( status().isOk() );
	}

}
