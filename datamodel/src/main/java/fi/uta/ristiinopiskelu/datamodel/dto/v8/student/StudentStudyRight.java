package fi.uta.ristiinopiskelu.datamodel.dto.v8.student;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRight;

public class StudentStudyRight extends StudyRight {

    private Boolean eligibleForNetworkStudies;
    
    public Boolean getEligibleForNetworkStudies() {
        return eligibleForNetworkStudies;
    }

    public void setEligibleForNetworkStudies(Boolean eligibleForNetworkStudies) {
        this.eligibleForNetworkStudies = eligibleForNetworkStudies;
    }
}
