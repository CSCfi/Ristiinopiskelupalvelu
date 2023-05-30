package fi.uta.ristiinopiskelu.messaging.message.v8.student;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRight;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.student.StudentStudyRight;
import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractPersonIdentifiableRequest;
import org.springframework.util.StringUtils;

public class UpdateStudentStudyRightRequest extends AbstractPersonIdentifiableRequest {

    private String oid;
    private String personId;
    private StudentStudyRight homeStudyRight;

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public StudentStudyRight getHomeStudyRight() {
        return homeStudyRight;
    }

    public void setHomeStudyRight(StudentStudyRight homeStudyRight) {
        this.homeStudyRight = homeStudyRight;
    }

    @JsonIgnore
    @Override
    public String getPersonIdentifier() {
        return !StringUtils.isEmpty(personId) ? personId : oid;
    }
}
