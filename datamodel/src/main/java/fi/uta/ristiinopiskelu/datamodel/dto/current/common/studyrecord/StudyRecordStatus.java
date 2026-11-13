package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.StudyRecordStatusEntity;

public enum StudyRecordStatus {

    RECORDED,
    RECORD_REJECTED;

    public static StudyRecordStatus from(StudyRecordStatusEntity source) {
        return source == null ? null : StudyRecordStatus.valueOf(source.name());
    }
}
