package fi.uta.ristiinopiskelu.datamodel.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "viestiskeemat", createIndex = false)
public class MessageSchemaEntity extends GenericEntity implements Serializable {

    private int schemaVersion;
}
