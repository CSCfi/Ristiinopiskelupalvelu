package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationSelectionItemTypeEntity;

import java.util.Arrays;

public enum CompletedCreditTargetType {

    ASSESSMENT_ITEM,
    COURSE_UNIT,
    DEGREE,
    STUDY_MODULE,
    REALISATION;

    public static CompletedCreditTargetType from(RegistrationSelectionItemTypeEntity selectionItemType) {
        if(selectionItemType == null) {
            return null;
        }

        return Arrays.stream(CompletedCreditTargetType.values())
            .filter(cctt -> cctt.name().equals(selectionItemType.name()))
            .findFirst()
            .orElse(null);
    }
}
