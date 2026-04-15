package io.vigier.cursorpaging.jpa.impl;

import io.vigier.cursorpaging.jpa.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith( MockitoExtension.class )
class JpaMetamodelAttributeResolverTest {

    // -- test model classes --

    static class TestEntity {
        String name;
        Integer age;
        AuditInfo auditInfo;
    }

    static class AuditInfo {
        Instant createdAt;
        Instant modifiedAt;
    }

    private JpaMetamodelAttributeResolver resolver;

    @SuppressWarnings( "unchecked" )
    @BeforeEach
    void setUp() {
        final Metamodel metamodel = mock( Metamodel.class );
        final ManagedType<TestEntity> entityType = mock( ManagedType.class );

        // Root entity attributes
        mockAttribute( entityType, "name", String.class );
        mockAttribute( entityType, "age", Integer.class );
        mockAttribute( entityType, "auditInfo", AuditInfo.class );

        // Embedded AuditInfo attributes
        final ManagedType<AuditInfo> auditInfoType = mock( ManagedType.class );
        lenient().when( metamodel.managedType( AuditInfo.class ) )
                .thenReturn( auditInfoType );
        mockAttribute( auditInfoType, "createdAt", Instant.class );
        mockAttribute( auditInfoType, "modifiedAt", Instant.class );

        lenient().when( metamodel.managedType( TestEntity.class ) )
                .thenReturn( entityType );

        resolver = JpaMetamodelAttributeResolver.of( metamodel, entityType );
    }

    @Test
    void shouldResolveSimpleAttribute() {
        final Attribute attribute = resolver.resolve( "name" );

        assertThat( attribute.name() ).isEqualTo( "name" );
        assertThat( attribute.type() ).isEqualTo( String.class );
        assertThat( attribute.attributes() ).hasSize( 1 );
    }

    @Test
    void shouldResolveIntegerAttribute() {
        final Attribute attribute = resolver.resolve( "age" );

        assertThat( attribute.name() ).isEqualTo( "age" );
        assertThat( attribute.type() ).isEqualTo( Integer.class );
    }

    @Test
    void shouldResolveDottedEmbeddedAttribute() {
        final Attribute attribute = resolver.resolve( "auditInfo.createdAt" );

        assertThat( attribute.name() ).isEqualTo( "auditInfo.createdAt" );
        assertThat( attribute.type() ).isEqualTo( Instant.class );
        assertThat( attribute.attributes() ).hasSize( 2 );
        assertThat( attribute.attributes()
                .get( 0 )
                .name() ).isEqualTo( "auditInfo" );
        assertThat( attribute.attributes()
                .get( 0 )
                .type() ).isEqualTo( AuditInfo.class );
        assertThat( attribute.attributes()
                .get( 1 )
                .name() ).isEqualTo( "createdAt" );
        assertThat( attribute.attributes()
                .get( 1 )
                .type() ).isEqualTo( Instant.class );
    }

    @Test
    void shouldResolveDottedModifiedAtAttribute() {
        final Attribute attribute = resolver.resolve( "auditInfo.modifiedAt" );

        assertThat( attribute.name() ).isEqualTo( "auditInfo.modifiedAt" );
        assertThat( attribute.type() ).isEqualTo( Instant.class );
    }

    @Test
    void shouldThrowForUnknownAttribute() {
        // ManagedType.getAttribute() throws IllegalArgumentException for unknown attributes.
        // In this mock setup, getAttribute("nonExistent") is not configured, so it returns null,
        // which causes a NullPointerException in SingleAttribute.of(). In a real JPA Metamodel,
        // the IllegalArgumentException would be thrown by getAttribute() itself.
        assertThatThrownBy( () -> resolver.resolve( "nonExistent" ) ).isInstanceOfAny( IllegalArgumentException.class,
                NullPointerException.class );
    }

    @Test
    void shouldCreateResolverFromEntityClass() {
        final Metamodel metamodel = mock( Metamodel.class );
        @SuppressWarnings( "unchecked" ) final ManagedType<TestEntity> entityType = mock( ManagedType.class );
        lenient().when( metamodel.managedType( TestEntity.class ) )
                .thenReturn( entityType );
        mockAttribute( entityType, "name", String.class );

        final var resolverFromClass = JpaMetamodelAttributeResolver.of( metamodel, TestEntity.class );
        final Attribute attribute = resolverFromClass.resolve( "name" );

        assertThat( attribute.name() ).isEqualTo( "name" );
        assertThat( attribute.type() ).isEqualTo( String.class );
    }

    // -- helper --
    
    private static <X, T> void mockAttribute( final ManagedType<X> managedType, final String name,
            final Class<T> javaType ) {
        final SingularAttribute<X, T> attr = mock( SingularAttribute.class );
        lenient().when( attr.getName() )
                .thenReturn( name );
        lenient().when( attr.getJavaType() )
                .thenReturn( javaType );
        lenient().doReturn( attr )
                .when( managedType )
                .getAttribute( name );
    }
}


