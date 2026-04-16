package io.vigier.cursorpaging.jpa.impl;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.AttributeResolver;
import io.vigier.cursorpaging.jpa.SingleAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

/**
 * An {@link AttributeResolver} that uses the JPA {@link Metamodel} to look up attribute types.
 * <p>
 * Dotted selectors (e.g. {@code "auditInfo.modifiedAt"}) are resolved by walking through the metamodel, following
 * embedded and relational attributes to determine the correct Java type for each path segment.
 * <p>
 * Example:
 * <pre>{@code
 *   var resolver = JpaMetamodelAttributeResolver.of( entityManager.getMetamodel(), DataRecord.class );
 *   Attribute attr = resolver.resolve( "auditInfo.createdAt" );
 * }</pre>
 */
public class JpaMetamodelAttributeResolver implements AttributeResolver {

    private final ManagedType<?> rootType;
    private final Metamodel metamodel;

    public JpaMetamodelAttributeResolver( final Metamodel metamodel, final ManagedType<?> rootType ) {
        this.metamodel = metamodel;
        this.rootType = rootType;
    }

    /**
     * Creates a new resolver for the given entity class.
     *
     * @param metamodel   the JPA metamodel
     * @param entityClass the root entity class
     * @return a new resolver instance
     */
    public static JpaMetamodelAttributeResolver of( final Metamodel metamodel, final Class<?> entityClass ) {
        return new JpaMetamodelAttributeResolver( metamodel, metamodel.managedType( entityClass ) );
    }

    /**
     * Creates a new resolver for the given managed type.
     *
     * @param metamodel the JPA metamodel
     * @param rootType  the root managed type
     * @return a new resolver instance
     */
    public static JpaMetamodelAttributeResolver of( final Metamodel metamodel, final ManagedType<?> rootType ) {
        return new JpaMetamodelAttributeResolver( metamodel, rootType );
    }

    @Override
    public Attribute resolve( final String name ) {
        final var segments = name.split( "\\." );
        final var path = new SingleAttribute[segments.length];
        var currentType = rootType;

        for ( int i = 0; i < segments.length; i++ ) {
            final var jpaAttribute = currentType.getAttribute( segments[i] );
            path[i] = SingleAttribute.of( jpaAttribute );

            if ( i < segments.length - 1 ) {
                currentType = metamodel.managedType( jpaAttribute.getJavaType() );
            }
        }
        return Attribute.of( path );
    }
}

