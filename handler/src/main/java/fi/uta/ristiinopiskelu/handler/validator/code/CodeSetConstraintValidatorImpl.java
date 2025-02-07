package fi.uta.ristiinopiskelu.handler.validator.code;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.validation.CodeSetConstraint;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.validation.CodeSetConstraintValidator;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;
import fi.uta.ristiinopiskelu.persistence.repository.CodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.util.List;

@Component
public class CodeSetConstraintValidatorImpl implements CodeSetConstraintValidator {

    @Autowired
    private CodeRepository codeRepository;

    private boolean required;
    private String codeSetKey;

    @Override
    public void initialize(CodeSetConstraint constraintAnnotation) {
        this.required = constraintAnnotation.required();
        this.codeSetKey = constraintAnnotation.codeSetKey();
    }

    @Override
    public boolean isValid(CodeReference value, ConstraintValidatorContext context) {
        if(required) {
            if(value == null) {
                return false;
            }
        } else {
            if (value == null) {
                return true;
            }
        }

        if(StringUtils.isEmpty(value.getCodeSetKey()) || StringUtils.isEmpty(value.getKey())) {
            return false;
        }

        if (!this.codeSetKey.equals(value.getCodeSetKey())) {
            return false;
        }

        List<CodeEntity> codes = codeRepository.findAllByKeyAndCodeSetKeyOrderByCodeUri(value.getKey(), value.getCodeSetKey());

        LocalDate now = LocalDate.now();
        return !CollectionUtils.isEmpty(codes) && codes.stream()
                .anyMatch(code -> ((code.getValidityStartDate().isBefore(now) || code.getValidityStartDate().isEqual(now))
                        && (code.getValidityEndDate().isAfter(now) || code.getValidityEndDate().isEqual(now))));
    }
}
