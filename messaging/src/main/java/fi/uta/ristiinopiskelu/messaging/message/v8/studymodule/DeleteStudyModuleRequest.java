package fi.uta.ristiinopiskelu.messaging.message.v8.studymodule;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;

public class DeleteStudyModuleRequest extends AbstractRequest {

    private String studyElementId;
    private boolean deleteCourseUnits = false;

    public String getStudyElementId() {
        return studyElementId;
    }

    public void setStudyElementId(String studyElementId) {
        this.studyElementId = studyElementId;
    }

    public boolean isDeleteCourseUnits() {
        return deleteCourseUnits;
    }

    public void setDeleteCourseUnits(boolean deleteCourseUnits) {
        this.deleteCourseUnits = deleteCourseUnits;
    }
}
