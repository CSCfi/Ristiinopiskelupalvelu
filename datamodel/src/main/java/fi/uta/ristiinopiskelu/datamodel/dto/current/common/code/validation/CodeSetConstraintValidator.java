package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.validation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeReference;

import jakarta.validation.ConstraintValidator;

public interface CodeSetConstraintValidator extends ConstraintValidator<CodeSetConstraint, CodeReference> {
}
