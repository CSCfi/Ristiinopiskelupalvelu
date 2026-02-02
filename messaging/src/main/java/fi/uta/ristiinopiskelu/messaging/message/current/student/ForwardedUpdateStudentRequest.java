package fi.uta.ristiinopiskelu.messaging.message.current.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Country;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentWarning;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ForwardedUpdateStudentRequest extends AbstractForwardedStudentMessage {
    private String studentRequestId;
    private String oid;
    private String homeEppn;
    private String hostEppn;
    private String firstNames;
    private String surName;
    private String givenName;

    private List<Address> address;
    private List<Country> countryOfCitizenship;
    private String municipalityOfResidence;
    private String motherTongue;
    private String preferredLanguage;
    private List<String> email;
    private List<Phone> phone;

    @Deprecated(since = "9.0.28")
    private List<StudentWarning> warnings;
    
    private List<StudyRightIdentifier> hostStudyRightIdentifiers;

    @Override
    public String getStudentRequestId() {
        return studentRequestId;
    }

    @Override
    public void setStudentRequestId(String studentRequestId) {
        this.studentRequestId = studentRequestId;
    }

    @Override
    public List<StudyRightIdentifier> getHostStudyRightIdentifiers() {
        return hostStudyRightIdentifiers;
    }

    @Override
    public void setHostStudyRightIdentifiers(List<StudyRightIdentifier> hostStudyRightIdentifiers) {
        this.hostStudyRightIdentifiers = hostStudyRightIdentifiers;
    }
}
