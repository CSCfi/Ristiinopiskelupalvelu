package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;

import java.util.Arrays;

public enum StudyStatusEntity {
    ACTIVE, CANCELLED, ARCHIVED;

    public static StudyStatusEntity from(StudyStatus status) {
        if(status == null) {
            return null;
        }

        return Arrays.stream(StudyStatusEntity.values())
            .filter(s -> s.name().equals(status.name()))
            .findFirst()
            .orElse(null);
    }
}
