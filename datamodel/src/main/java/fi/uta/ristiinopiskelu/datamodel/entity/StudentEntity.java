package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.entity.common.student.StudentMessageTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.student.UpdateStatusEntity;
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
    private List<UpdateStatusEntity> statuses;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime timestamp;

    private String homeOrganisationTkCode;
    private StudentMessageTypeEntity messageType;
}
