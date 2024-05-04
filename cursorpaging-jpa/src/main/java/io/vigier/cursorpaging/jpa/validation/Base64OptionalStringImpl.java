package io.vigier.cursorpaging.jpa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;

public class Base64OptionalStringImpl implements ConstraintValidator<Base64, Optional<String>> {

    @Override
    public boolean isValid( final Optional<String> cursor,
            final ConstraintValidatorContext constraintValidatorContext ) {
        return cursor.map( this::isValidBase64 ).orElse( true );
    }

    private boolean isValidBase64( final String cursor ) {
        try {
            java.util.Base64.getUrlDecoder().decode( cursor );
            return true;
        } catch ( final IllegalArgumentException e ) {
            return false;
        }
    }

}
