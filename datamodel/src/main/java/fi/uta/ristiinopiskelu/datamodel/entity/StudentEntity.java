package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentMessageType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.UpdateStatus;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "opiskelijat", createIndex = false)
public class StudentEntity extends GenericEntity {

    private StudentStudyRightEntity homeStudyRight;
    private List<UpdateStatus> statuses;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime timestamp;

    private String homeOrganisationTkCode;
    private StudentMessageType messageType;
}
