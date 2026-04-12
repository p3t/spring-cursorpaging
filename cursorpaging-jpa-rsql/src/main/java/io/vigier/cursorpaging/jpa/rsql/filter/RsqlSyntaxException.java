package io.vigier.cursorpaging.jpa.rsql.filter;

/**
 * Thrown when an RSQL expression cannot be parsed due to a syntax error.
 */
public class RsqlSyntaxException extends RuntimeException {

    public RsqlSyntaxException( final String message, final Throwable cause ) {
        super( message, cause );
    }

    public RsqlSyntaxException( final String message ) {
        super( message );
    }
}

