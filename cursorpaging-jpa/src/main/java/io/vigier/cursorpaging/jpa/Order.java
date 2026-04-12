package io.vigier.cursorpaging.jpa;

/**
 * Enum defining the order to be used for the cursor (position) query
 */
public enum Order {
    /**
     * Ascending order
     */
    ASC,
    /**
     * Descending order
     */
    DESC;

    /**
     * Returns the order matching the given String by ignoring the case.
     *
     * @param order to match
     * @return the matching order
     * @throws IllegalArgumentException if no matching order is found
     */
    public static Order from( final String order ) {
        for ( final var v : values() ) {
            if ( v.name().equalsIgnoreCase( order ) ) {
                return v;
            }
        }
        throw new IllegalArgumentException( "Unknown order: " + order );
    }

}
