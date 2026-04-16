package io.vigier.cursorpaging.jpa.serializer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class RequestSerializerFactoryTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Metamodel metamodel;
    @Mock
    private ManagedType<TestEntity> managedType;

    static class TestEntity {

    }

    @Test
    void testCreate() {
        // Arrange
        // Act
        final RequestSerializerFactory result = RequestSerializerFactory.create( b -> b.serializer(
                RequestSerializer.create( TestEntity.class )
                        .apply( _ -> {} ) ) );
        // Assert
        assertNotNull( result );
    }

    @Test
    void testForEntity() {
        when( entityManager.getMetamodel() ).thenReturn( metamodel );
        when( metamodel.managedType( TestEntity.class ) ).thenReturn( managedType );
        final var requestSerializerFactory = RequestSerializerFactory.create( f -> f.entityManager( entityManager ) );
        // When
        final var result = requestSerializerFactory.forEntity( TestEntity.class );
        // Then
        assertNotNull( result );
        verify( metamodel ).managedType( TestEntity.class );
    }

}