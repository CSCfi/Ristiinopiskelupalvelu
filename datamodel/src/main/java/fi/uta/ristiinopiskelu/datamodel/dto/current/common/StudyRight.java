package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.StudyRightType;

import java.io.Serializable;
import java.util.List;

public class StudyRight implements Serializable {

    private StudyRightIdentifier identifiers;
    private StudyRightStatus studyRightStatus;
    private StudyRightType studyRightType;
    private List<Keyword> keywords;

    public StudyRightIdentifier getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(StudyRightIdentifier identifiers) {
        this.identifiers = identifiers;
    }

    public StudyRightStatus getStudyRightStatus() {
        return studyRightStatus;
    }

    public void setStudyRightStatus(StudyRightStatus studyRightStatus) {
        this.studyRightStatus = studyRightStatus;
    }

    public StudyRightType getStudyRightType() {
        return studyRightType;
    }

    public void setStudyRightType(StudyRightType studyRightType) {
        this.studyRightType = studyRightType;
    }

    public List<Keyword> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }
}
