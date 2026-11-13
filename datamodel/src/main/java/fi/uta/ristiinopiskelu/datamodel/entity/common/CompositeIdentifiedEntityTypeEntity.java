package fi.uta.ristiinopiskelu.datamodel.entity.common;

public enum CompositeIdentifiedEntityTypeEntity {

    STUDY_MODULE,
    COURSE_UNIT,
    DEGREE,
    REALISATION;

    public static CompositeIdentifiedEntityTypeEntity from(StudyElementTypeEntity studyElementType) {
        for(CompositeIdentifiedEntityTypeEntity type : CompositeIdentifiedEntityTypeEntity.values()) {
            if(type.name().equals(studyElementType.name())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Cannot match StudyElementTypeEntity '" + studyElementType.name() + "' to CompositeIdentifiedEntityTypeEntity");
    }
}
