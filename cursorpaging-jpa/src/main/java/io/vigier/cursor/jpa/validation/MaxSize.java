package io.vigier.cursor.jpa.validation;

import io.vigier.cursor.jpa.PageRequest;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint( validatedBy = { MaxSizeOptionalIntImpl.class } )
@Target( { ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface MaxSize {

    String message() default "Size must be greater than 0 and less than or equal to {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int value() default PageRequest.DEFAULT_PAGE_SIZE;
}
