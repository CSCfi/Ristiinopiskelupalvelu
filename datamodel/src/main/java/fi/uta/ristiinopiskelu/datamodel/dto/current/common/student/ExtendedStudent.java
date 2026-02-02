package fi.uta.ristiinopiskelu.datamodel.dto.current.common.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Country;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationStudentEntity;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ExtendedStudent extends Student {

    private StudyRight hostStudyRight;
    private StudyRight homeStudyRight;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate dateOfBirth;

    private int gender; // 1 = male, 2 = female
    private List<Address> addresses;
    private Country countryOfCitizenship;
    private String municipalityOfResidence;
    private String motherTongue;
    private String preferredLanguage;
    
    private List<String> email;
    private List<Phone> phone;
    private Boolean safetyProhibition;

    @Builder
    public ExtendedStudent(String oid, String personId, String homeStudentNumber, String hostStudentNumber, StudyRight hostStudyRight,
                           StudyRight homeStudyRight, LocalDate dateOfBirth, int gender, List<Address> addresses, Country countryOfCitizenship,
                           String municipalityOfResidence, String motherTongue, String preferredLanguage, List<String> email,
                           List<Phone> phone, Boolean safetyProhibition) {
        super(oid, personId, homeStudentNumber, hostStudentNumber);
        this.hostStudyRight = hostStudyRight;
        this.homeStudyRight = homeStudyRight;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.addresses = addresses;
        this.countryOfCitizenship = countryOfCitizenship;
        this.municipalityOfResidence = municipalityOfResidence;
        this.motherTongue = motherTongue;
        this.preferredLanguage = preferredLanguage;
        this.email = email;
        this.phone = phone;
        this.safetyProhibition = safetyProhibition;
    }

    public static ExtendedStudent from(RegistrationStudentEntity student) {
        return ExtendedStudent.builder()
            .homeStudentNumber(student.getHomeStudentNumber())
            .hostStudentNumber(student.getHostStudentNumber())
            .hostStudyRight(StudyRight.from(student.getHostStudyRight()))
            .homeStudyRight(StudyRight.from(student.getHomeStudyRight()))
            .build();
    }
}
