package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCreditStatus;

public enum CompletedCreditStatusEntity {

    NOT_PARTICIPATED,
    ATTEMPT_ABANDONED,
    ACCEPTED,
    UNFINISHED;

    public static CompletedCreditStatusEntity from(CompletedCreditStatus source) {
        return source == null ? null : CompletedCreditStatusEntity.valueOf(source.name());
    }
}
