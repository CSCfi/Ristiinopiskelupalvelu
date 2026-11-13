package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus;

import java.util.Arrays;

public enum StudyRecordStatusEntity {

    RECORDED,
    RECORD_REJECTED;

    public static StudyRecordStatusEntity from(StudyRecordStatus status) {
        if(status == null) {
            return null;
        }

        return Arrays.stream(StudyRecordStatusEntity.values())
            .filter(s -> s.name().equals(status.name()))
            .findFirst()
            .orElse(null);
    }
}
