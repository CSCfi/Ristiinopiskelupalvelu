package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.validation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeReference;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DelegatingCodeSetConstraintValidator implements ConstraintValidator<CodeSetConstraint, CodeReference> {

    @Autowired
    private CodeSetConstraintValidator validator;

    @Override
    public void initialize(CodeSetConstraint constraintAnnotation) {
        this.validator.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(CodeReference value, ConstraintValidatorContext context) {
        return this.validator.isValid(value, context);
    }
}
