package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeSetEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeValueEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "koodisto", createIndex = false)
public class CodeEntity extends GenericEntity implements Serializable {
    private String codeUri;
    private String resourceUri;
    private CodeSetEntity codeSet;
    private String key;
    private Integer codeVersion;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate updateDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityStartDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityEndDate;
    
    private String status;
    private List<CodeValueEntity> codeValues;
}
