package io.vigier.cursorpaging.jpa.serializer;

public class SerializerException extends RuntimeException {
    public SerializerException( final String message ) {
        super( message );
    }

    public SerializerException( final String message, final Throwable cause ) {
        super( message, cause );
    }
}
