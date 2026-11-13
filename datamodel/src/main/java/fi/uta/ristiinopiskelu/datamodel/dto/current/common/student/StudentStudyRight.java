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

    public static StudentStudyRight from(StudyRight source) {
        if (source == null) {
            return null;
        }

        StudentStudyRight target = new StudentStudyRight();
        target.setIdentifiers(source.getIdentifiers());
        target.setStudyRightStatus(source.getStudyRightStatus());
        target.setStudyRightType(source.getStudyRightType());
        target.setKeywords(source.getKeywords());
        return target;
    }
}
