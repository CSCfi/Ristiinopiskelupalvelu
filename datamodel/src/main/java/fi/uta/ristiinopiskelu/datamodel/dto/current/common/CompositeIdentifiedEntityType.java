package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.CompositeIdentifiedEntityTypeEntity;

public enum CompositeIdentifiedEntityType {

    STUDY_MODULE,
    COURSE_UNIT,
    DEGREE,
    REALISATION;

    public static CompositeIdentifiedEntityType from(StudyElementType studyElementType) {
        for(CompositeIdentifiedEntityType type : CompositeIdentifiedEntityType.values()) {
            if(type.name().equals(studyElementType.name())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Cannot match StudyElementType '" + studyElementType.name() + "' to CompositeIdentifiedEntityType");
    }

    public static CompositeIdentifiedEntityType from(CompositeIdentifiedEntityTypeEntity typeEntity) {
        for(CompositeIdentifiedEntityType type : CompositeIdentifiedEntityType.values()) {
            if(type.name().equals(typeEntity.name())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Cannot match CompositeIdentifiedEntityTypeEntity '" + typeEntity.name() + "' to CompositeIdentifiedEntityType");
    }
}
