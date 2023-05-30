package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD, TYPE_USE })
@Retention(RUNTIME)
@Constraint(validatedBy = { DelegatingCodeSetConstraintValidator.class })
@Documented
public @interface CodeSetConstraint {
    String message() default "Invalid codeSetKey '${validatedValue.codeSetKey}' or invalid code key '${validatedValue.key}' specified";

    boolean required() default false;

    String codeSetKey();

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
