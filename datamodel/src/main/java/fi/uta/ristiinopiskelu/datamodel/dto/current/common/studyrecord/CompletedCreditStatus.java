package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.CompletedCreditStatusEntity;

public enum CompletedCreditStatus {

    NOT_PARTICIPATED,
    ATTEMPT_ABANDONED,
    ACCEPTED,
    UNFINISHED;

    public static CompletedCreditStatus from(CompletedCreditStatusEntity source) {
        return source == null ? null : CompletedCreditStatus.valueOf(source.name());
    }
}
