package ru.shvets.blog.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = IsPostValidator.class)
@Documented
public @interface IsPost {
        String message() default "{IsPost.invalid}";
        Class<?>[] groups() default { };
        Class<? extends Payload>[] payload() default { };
}
