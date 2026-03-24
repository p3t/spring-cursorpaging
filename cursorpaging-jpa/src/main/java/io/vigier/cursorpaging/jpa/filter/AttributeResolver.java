package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;

/**
 * Resolves an RSQL selector (e.g. {@code "name"} or {@code "auditInfo.modifiedAt"}) to a typed {@link Attribute}.
 * <p>
 * Implement this interface to provide proper type information for non-string attributes so that value conversion
 * (via {@link Attribute#verify}) works correctly.
 */
@FunctionalInterface
public interface AttributeResolver {

    /**
     * Resolve the given RSQL selector to an {@link Attribute}.
     *
     * @param selector the RSQL selector string
     * @return the corresponding attribute
     */
    Attribute resolve( String selector );
}

