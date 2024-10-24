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

    private final String encoded;

    public Base64String( final String base64String ) {
        this.encoded = validate( base64String );
    }

    public Base64String( final byte[] bytes ) {
        this.encoded = new String( bytes, StandardCharsets.UTF_8 );
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
        return decode( encoded );
    }

    private static byte[] decode( final String base64String ) {
        return Base64.getUrlDecoder().decode( base64String );
    }

    @Override
    public int length() {
        return encoded.length();
    }

    @Override
    public char charAt( final int index ) {
        return encoded.charAt( index );
    }

    @Override
    public boolean isEmpty() {
        return encoded.isEmpty();
    }

    @Override
    public @NotNull CharSequence subSequence( final int start, final int end ) {
        return encoded.subSequence( start, end );
    }

    @Override
    public @NotNull IntStream chars() {
        return encoded.chars();
    }

    @Override
    public @NotNull IntStream codePoints() {
        return encoded.codePoints();
    }

    @Override
    public @NotNull String toString() {
        return encoded;
    }

    /**
     * Modify the encoded string and get a new {@link Base64String} instance.
     *
     * @param target      the sequence to be replaced
     * @param replacement the replacement sequence
     * @return a new Base64String with the replaced sequence
     */
    public Base64String replace( final String target, final String replacement ) {
        return new Base64String( encoded.replace( target, replacement ) );
    }
}
