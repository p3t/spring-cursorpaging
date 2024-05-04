package io.vigier.cursorpaging.jpa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint( validatedBy = { Base64OptionalStringImpl.class } )
@Target( { ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface Base64 {

    String message() default "Cursor must be an base64 encoded string";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
