package io.vigier.cursorpaging.jpa.serializer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestSerializerFactoryTest {

    static class TestEntity {

    }

    @Test
    void testCreate() {
        // Arrange
        // Act
        final RequestSerializerFactory result = RequestSerializerFactory.create( b -> {
            b.serialalizer( RequestSerializer.create( TestEntity.class ).apply( r -> {} ) );
        } );
        // Assert
        assertNotNull( result );
    }

    @Test
    void testForEntity() {
        // Arrange
        final RequestSerializerFactory requestSerializerFactory = RequestSerializerFactory.builder()
                .build();
        // Act
        final RequestSerializer<TestEntity> result = requestSerializerFactory.forEntity( TestEntity.class );
        // Assert
        assertNotNull( result );
    }

}