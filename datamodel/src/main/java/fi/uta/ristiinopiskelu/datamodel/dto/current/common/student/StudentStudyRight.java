package fi.uta.ristiinopiskelu.datamodel.dto.current.common.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight;

public class StudentStudyRight extends StudyRight {

    private Boolean eligibleForNetworkStudies;
    
    public Boolean getEligibleForNetworkStudies() {
        return eligibleForNetworkStudies;
    }

    public void setEligibleForNetworkStudies(Boolean eligibleForNetworkStudies) {
        this.eligibleForNetworkStudies = eligibleForNetworkStudies;
    }
}
