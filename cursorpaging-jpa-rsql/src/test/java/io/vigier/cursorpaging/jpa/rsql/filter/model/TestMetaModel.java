package io.vigier.cursorpaging.jpa.rsql.filter.model;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.Instant;

/**
 * Sets up a mocked JPA Metamodel for {@link TestEntity} (with embedded {@link AuditInfo}) that can be used in unit
 * tests.
 */
public class TestMetaModel {

    private final EntityManager entityManager;
    private final Metamodel metamodel;
    private final ManagedType<TestEntity> entityType;

    @SuppressWarnings( "unchecked" )
    private TestMetaModel() {
        this.entityManager = mock( EntityManager.class );
        this.metamodel = mock( Metamodel.class );
        this.entityType = mock( ManagedType.class );

        lenient().when( entityManager.getMetamodel() ).thenReturn( metamodel );

        // Root entity attributes
        mockAttribute( entityType, "name", String.class );
        mockAttribute( entityType, "age", Integer.class );

        // Embedded 'auditInfo' and its nested attributes
        mockAttribute( entityType, "auditInfo", AuditInfo.class );
        final ManagedType<AuditInfo> auditInfoType = mock( ManagedType.class );
        lenient().when( metamodel.managedType( AuditInfo.class ) ).thenReturn( auditInfoType );
        mockAttribute( auditInfoType, "createdAt", Instant.class );
        mockAttribute( auditInfoType, "modifiedAt", Instant.class );
    }

    /**
     * Initialises and returns a new {@link TestMetaModel} with a fully mocked {@link EntityManager} and JPA
     * {@link Metamodel}.
     *
     * @return the initialised meta-model
     */
    public static TestMetaModel init() {
        return new TestMetaModel();
    }

    public EntityManager entityManager() {
        return entityManager;
    }

    public ManagedType<TestEntity> entityType() {
        return entityType;
    }

    public Metamodel metamodel() {
        return metamodel;
    }

    // -- internal helper --

    @SuppressWarnings( "unchecked" )
    private static <X> void mockAttribute( final ManagedType<X> managedType, final String name,
            final Class<?> javaType ) {
        final SingularAttribute<X, ?> attr = mock( SingularAttribute.class );
        lenient().when( attr.getName() ).thenReturn( name );
        lenient().when( attr.getJavaType() ).thenReturn( (Class) javaType );
        lenient().when( managedType.getAttribute( name ) ).thenReturn( (SingularAttribute) attr );
    }
}

