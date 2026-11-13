package fi.uta.ristiinopiskelu.datamodel.entity.common.student;

import fi.uta.ristiinopiskelu.datamodel.entity.common.AddressEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CountryEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.PhoneEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightDataEntity;
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
public class ExtendedStudentEntity extends StudentDataEntity {

    private StudyRightDataEntity hostStudyRight;
    private StudyRightDataEntity homeStudyRight;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate dateOfBirth;

    private int gender; // 1 = male, 2 = female
    private List<AddressEntity> addresses;
    private CountryEntity countryOfCitizenship;
    private String municipalityOfResidence;
    private String motherTongue;
    private String preferredLanguage;
    
    private List<String> email;
    private List<PhoneEntity> phone;
    private Boolean safetyProhibition;

    @Builder
    public ExtendedStudentEntity(String oid, String personId, String homeStudentNumber, String hostStudentNumber, StudyRightDataEntity hostStudyRight,
                           StudyRightDataEntity homeStudyRight, LocalDate dateOfBirth, int gender, List<AddressEntity> addresses, CountryEntity countryOfCitizenship,
                           String municipalityOfResidence, String motherTongue, String preferredLanguage, List<String> email,
                           List<PhoneEntity> phone, Boolean safetyProhibition) {
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

    public static ExtendedStudentEntity from(RegistrationStudentEntity student) {
        return ExtendedStudentEntity.builder()
            .homeStudentNumber(student.getHomeStudentNumber())
            .hostStudentNumber(student.getHostStudentNumber())
            .hostStudyRight(StudyRightDataEntity.from(student.getHostStudyRight()))
            .homeStudyRight(StudyRightDataEntity.from(student.getHomeStudyRight()))
            .build();
    }
}
