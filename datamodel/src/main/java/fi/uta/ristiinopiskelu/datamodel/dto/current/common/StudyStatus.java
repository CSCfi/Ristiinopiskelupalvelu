package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyStatusEntity;

public enum StudyStatus {
    ACTIVE, CANCELLED, ARCHIVED;

    public static StudyStatus from(StudyStatusEntity source) {
        return source == null ? null : StudyStatus.valueOf(source.name());
    }
}
