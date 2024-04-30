package io.vigier.cursor.jpa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;

public class MaxSizeOptionalIntImpl implements ConstraintValidator<MaxSize, Optional<Integer>> {

    private MaxSize optMax;

    @Override
    public void initialize( final MaxSize constraintAnnotation ) {
        this.optMax = constraintAnnotation;
    }

    @Override
    public boolean isValid( final Optional<Integer> size,
            final ConstraintValidatorContext constraintValidatorContext ) {
        return size.map( s -> s > 0 && s <= optMax.value() ).orElse( true );
    }
}
