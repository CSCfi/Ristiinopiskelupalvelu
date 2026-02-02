package fi.uta.ristiinopiskelu.messaging.message.current.student;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Country;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentWarning;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractPersonIdentifiableRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateStudentRequest extends AbstractPersonIdentifiableRequest {

    private String oid;
    private String personId;

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

    @JsonIgnore
    @Override
    public String getPersonOid() {
        return oid;
    }
}
