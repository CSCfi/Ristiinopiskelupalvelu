package fi.uta.ristiinopiskelu.datamodel.dto.v8;

public enum StudyElementType {

    STUDY_MODULE,
    COURSE_UNIT,
    EDUCATION,
    DEGREE,
    ASSESSMENT_ITEM;

    public static <D extends StudyElement> StudyElementType of(Class<D> dtoClass) {
        if(CourseUnit.class.isAssignableFrom(dtoClass)) {
            return COURSE_UNIT;
        } else if(StudyModule.class.isAssignableFrom(dtoClass)) {
            return STUDY_MODULE;
        } else if(Degree.class.isAssignableFrom(dtoClass)) {
            return DEGREE;
        }
        
        throw new IllegalArgumentException("Unknown study element: " + dtoClass.getSimpleName());
    }
}
