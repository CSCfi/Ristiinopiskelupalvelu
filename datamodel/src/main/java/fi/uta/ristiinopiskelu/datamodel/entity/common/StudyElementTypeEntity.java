package fi.uta.ristiinopiskelu.datamodel.entity.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.DegreeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;

import java.util.Arrays;

public enum StudyElementTypeEntity {

    STUDY_MODULE(StudyModuleEntity.class),
    COURSE_UNIT(CourseUnitEntity.class),
    EDUCATION(null),
    DEGREE(DegreeEntity.class),
    ASSESSMENT_ITEM(null);

    private final Class<? extends StudyElementEntity> studyElementEntityClass;

    StudyElementTypeEntity(Class<? extends StudyElementEntity> studyElementEntityClass) {
        this.studyElementEntityClass = studyElementEntityClass;
    }

    @JsonIgnore
    public Class<? extends StudyElementEntity> getStudyElementEntityClass() {
        return studyElementEntityClass;
    }

    public static StudyElementTypeEntity from(StudyElementType type) {
        if(type == null) {
            return null;
        }

        return Arrays.stream(StudyElementTypeEntity.values())
            .filter(t -> t.name().equals(type.name()))
            .findFirst()
            .orElse(null);
    }
}
