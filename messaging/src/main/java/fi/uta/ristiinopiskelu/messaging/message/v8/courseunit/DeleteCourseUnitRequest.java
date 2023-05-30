package fi.uta.ristiinopiskelu.messaging.message.v8.courseunit;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;

public class DeleteCourseUnitRequest extends AbstractRequest {

    private String studyElementId;
    private boolean deleteRealisations = false;

    public String getStudyElementId() {
        return studyElementId;
    }

    public void setStudyElementId(String studyElementId) {
        this.studyElementId = studyElementId;
    }

    public boolean isDeleteRealisations() {
        return deleteRealisations;
    }

    public void setDeleteRealisations(boolean deleteRealisations) {
        this.deleteRealisations = deleteRealisations;
    }
}
