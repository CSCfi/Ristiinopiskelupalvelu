package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Description;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "rekisteroinnit", createIndex = false)
public class RegistrationEntity extends GenericEntity implements Serializable {

    private String sendingOrganisationTkCode;
    private String receivingOrganisationTkCode;
    private String networkIdentifier;
    private String networkDescription;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime sendDateTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrolmentDateTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime receivingDateTime;
    
    private RegistrationStatus status;
    private String statusInfo;
    private RegistrationStudentEntity student;
    private List<RegistrationSelection> selections;
    private List<RegistrationSelection> selectionsReplies;
    private List<Description> descriptions;
    private LocalisedString rejectionReason;
}
