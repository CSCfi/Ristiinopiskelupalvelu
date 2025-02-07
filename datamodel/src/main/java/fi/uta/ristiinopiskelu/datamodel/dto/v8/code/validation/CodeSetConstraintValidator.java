package fi.uta.ristiinopiskelu.datamodel.dto.v8.code.validation;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeReference;

import jakarta.validation.ConstraintValidator;

public interface CodeSetConstraintValidator extends ConstraintValidator<CodeSetConstraint, CodeReference> {
}
