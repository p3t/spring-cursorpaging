package io.vigier.cursorpaging.jpa;

/**
 * Resolves a dot-separated attribute path (e.g. {@code "auditInfo.createdAt"}) to a fully typed {@link Attribute}.
 * <p>
 * Implement this interface to provide proper type information for attributes so that value conversion works correctly
 * during deserialization.
 */
@FunctionalInterface
public interface AttributeResolver {

    /**
     * Resolve the given dot-separated attribute path to an {@link Attribute}.
     *
     * @param name the dot-separated attribute path
     * @return the resolved attribute
     * @throws IllegalArgumentException if the name cannot be resolved
     */
    Attribute resolve( String name );
}

