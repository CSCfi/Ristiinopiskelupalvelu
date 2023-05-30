package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

public class CourseUnitReference {

    private String courseUnitId;
    private String organizingOrganisationId;

    public CourseUnitReference() {
    }

    public CourseUnitReference(String courseUnitId, String organizingOrganisationId) {
        this.courseUnitId = courseUnitId;
        this.organizingOrganisationId = organizingOrganisationId;
    }

    public String getCourseUnitId() {
        return courseUnitId;
    }

    public void setCourseUnitId(String courseUnitId) {
        this.courseUnitId = courseUnitId;
    }

    public String getOrganizingOrganisationId() {
        return organizingOrganisationId;
    }

    public void setOrganizingOrganisationId(String organizingOrganisationId) {
        this.organizingOrganisationId = organizingOrganisationId;
    }
}
