package io.vigier.cursorpaging.jpa.serializer;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.vigier.cursorpaging.jpa.PageRequest.create;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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

    @Data
    static class TestEntity {
        private Long id;
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

    @Test
    void shouldNotThrowExceptionWhenAttributeIsSerializedWithNullEntityManager() {
        final var requestSerializerFactory = RequestSerializerFactory.create( f -> f.entityManager( null ) );
        // When
        final var serializer = requestSerializerFactory.forEntity( TestEntity.class );
        final PageRequest<TestEntity> request = create( r -> r.desc( Attribute.of( "id", Long.class ) ) );
        final var deserialized = serializer.toPageRequest( serializer.toBase64( request ) );
        // Then
        assertNotNull( deserialized );
    }

    @Test
    void shouldThrowExceptionWhenAttributeIsUnknownAndEntityManagerIsNull() {
        final Encrypter encrypter = Encrypter.getInstance();
        final var serializer = getRequestSerializerFactory( encrypter ).forEntity( TestEntity.class );
        final PageRequest<TestEntity> request = create( r -> r.desc( Attribute.of( "id", Long.class ) ) );
        final var base64 = serializer.toBase64( request );

        // We need a new serializer to simulate deserialization in a different context where the factory does not know about the attribute
        final var newSerializer = getRequestSerializerFactory( encrypter ).forEntity( TestEntity.class );

        // Assert
        assertThatThrownBy( () -> newSerializer.toPageRequest( base64 ) ).isInstanceOf( SerializerException.class )
                .hasMessageContaining(
                        "Attribute 'id' not present in cache and no entity manager configured to resolve attributes" );
    }

    private static RequestSerializerFactory getRequestSerializerFactory( final Encrypter encrypter ) {
        return RequestSerializerFactory.create( f -> f.entityManager( null )
                .encrypter( encrypter ) );
    }
}