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

    public static fi.uta.ristiinopiskelu.datamodel.dto.v8.student.StudentStudyRight from(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRight source) {
        if (source == null) {
            return null;
        }

        fi.uta.ristiinopiskelu.datamodel.dto.v8.student.StudentStudyRight target =
            new fi.uta.ristiinopiskelu.datamodel.dto.v8.student.StudentStudyRight();
        target.setIdentifiers(source.getIdentifiers());
        target.setStudyRightStatus(source.getStudyRightStatus());
        target.setStudyRightType(source.getStudyRightType());
        target.setKeywords(source.getKeywords());
        return target;
    }
}
