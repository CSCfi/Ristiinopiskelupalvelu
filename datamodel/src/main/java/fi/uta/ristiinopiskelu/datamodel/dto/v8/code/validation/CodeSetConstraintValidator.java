package fi.uta.ristiinopiskelu.datamodel.dto.v8.code.validation;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeReference;

import javax.validation.ConstraintValidator;

public interface CodeSetConstraintValidator extends ConstraintValidator<CodeSetConstraint, CodeReference> {
}
