package io.vigier.cursorpaging.jpa.validation;

import io.vigier.cursorpaging.jpa.PageRequest;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenience validation annotation replacing:
 * <pre>{@code final Optional<@Max( {{max-value.here}} ) @Min( 1 ) Integer> pageSize}</pre>
 * with:
 * <pre>{@code final @MaxSize( {{max-value.here}} ) Optional<Integer> pageSize}</pre>
 * The max value defaults to {@link PageRequest#DEFAULT_PAGE_SIZE}
 */
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
