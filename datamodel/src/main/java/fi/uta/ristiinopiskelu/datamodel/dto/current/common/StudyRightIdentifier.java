package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import java.util.Objects;

public class StudyRightIdentifier {

    private String organisationTkCodeReference;
    private String studyRightId;

    public String getOrganisationTkCodeReference() {
        return organisationTkCodeReference;
    }

    public void setOrganisationTkCodeReference(String organisationTkCodeReference) {
        this.organisationTkCodeReference = organisationTkCodeReference;
    }

    public String getStudyRightId() {
        return studyRightId;
    }

    public void setStudyRightId(String studyRightId) {
        this.studyRightId = studyRightId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudyRightIdentifier)) return false;
        StudyRightIdentifier that = (StudyRightIdentifier) o;
        return Objects.equals(getOrganisationTkCodeReference(), that.getOrganisationTkCodeReference()) && Objects.equals(getStudyRightId(), that.getStudyRightId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrganisationTkCodeReference(), getStudyRightId());
    }
}
