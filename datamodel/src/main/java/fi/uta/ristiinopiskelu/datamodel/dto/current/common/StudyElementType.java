package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.DegreeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CompositeIdentifiedEntityTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyElementTypeEntity;

import java.util.Arrays;

public enum StudyElementType {

    STUDY_MODULE(StudyModuleEntity.class),
    COURSE_UNIT(CourseUnitEntity.class),
    EDUCATION(null),
    DEGREE(DegreeEntity.class),
    ASSESSMENT_ITEM(null);

    private final Class<? extends StudyElementEntity> studyElementEntityClass;

    StudyElementType(Class<? extends StudyElementEntity> studyElementEntityClass) {
        this.studyElementEntityClass = studyElementEntityClass;
    }

    @JsonIgnore
    public Class<? extends StudyElementEntity> getStudyElementEntityClass() {
        return studyElementEntityClass;
    }

    public static StudyElementType from(CompositeIdentifiedEntityType type) {
        if(type == null) {
            return null;
        }

        return Arrays.stream(StudyElementType.values())
            .filter(set -> set.name().equals(type.name()))
            .findFirst()
            .orElse(null);
    }

    public static StudyElementType from(CompositeIdentifiedEntityTypeEntity type) {
        if (type == null) {
            return null;
        }

        return Arrays.stream(StudyElementType.values())
            .filter(set -> set.name().equals(type.name()))
            .findFirst()
            .orElse(null);
    }

    public static StudyElementType from(StudyElementTypeEntity type) {
        if (type == null) {
            return null;
        }

        return Arrays.stream(StudyElementType.values())
            .filter(set -> set.name().equals(type.name()))
            .findFirst()
            .orElse(null);
    }
}
