package io.vigier.cursorpaging.jpa.serializer;

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.IntStream;

/**
 * Value class for base64 encoded strings, making sure that the given string can be decoded.
 */
public class Base64String implements CharSequence {

    private final String base64String;

    public Base64String( final String base64String ) {
        this.base64String = validate( base64String );
    }

    public Base64String( final byte[] bytes ) {
        this.base64String = new String( bytes, StandardCharsets.UTF_8 );
    }

    public static Base64String encode( final byte[] content ) {
        return new Base64String( Base64.getUrlEncoder().encode( content ) );
    }

    private String validate( final String base64String ) {
        if ( base64String.isEmpty() ) {
            throw new ValidationException( "Base64 string must not be empty" );
        }
        try {
            decode( base64String );
        } catch ( final IllegalArgumentException e ) {
            throw new ValidationException(
                    "Argument string must be a valid Base64 string : '%s'".formatted( base64String ), e );
        }
        return base64String;
    }

    /**
     * @return the decoded byte array of the base64 string
     */
    public byte[] decoded() {
        return decode( base64String );
    }

    private static byte[] decode( final String base64String ) {
        return Base64.getUrlDecoder().decode( base64String );
    }

    @Override
    public int length() {
        return base64String.length();
    }

    @Override
    public char charAt( final int index ) {
        return base64String.charAt( index );
    }

    @Override
    public boolean isEmpty() {
        return base64String.isEmpty();
    }

    @Override
    public @NotNull CharSequence subSequence( final int start, final int end ) {
        return base64String.subSequence( start, end );
    }

    @Override
    public @NotNull IntStream chars() {
        return base64String.chars();
    }

    @Override
    public @NotNull IntStream codePoints() {
        return base64String.codePoints();
    }

    @Override
    public @NotNull String toString() {
        return base64String;
    }

    /**
     * Modify the encoded string and get a new {@link Base64String} instance.
     *
     * @param target      the sequence to be replaced
     * @param replacement the replacement sequence
     * @return a new Base64String with the replaced sequence
     */
    public Base64String replace( final String target, final String replacement ) {
        return new Base64String( base64String.replace( target, replacement ) );
    }
}
