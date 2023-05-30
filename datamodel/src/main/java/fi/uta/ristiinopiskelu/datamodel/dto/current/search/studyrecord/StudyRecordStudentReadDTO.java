package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StudyRecordStudent")
public class StudyRecordStudentReadDTO {

    private String oid;
    private String firstNames;
    private String surName;
    private String givenName;

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public void setFirstNames(String firstNames) {
        this.firstNames = firstNames;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }
}
