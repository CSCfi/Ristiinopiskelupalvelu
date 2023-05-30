package fi.uta.ristiinopiskelu.datamodel.entity;

import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Document(indexName = "viestiskeemat", createIndex = false)
public class MessageSchemaEntity extends GenericEntity implements Serializable {

    private int schemaVersion;

    public MessageSchemaEntity() {

    }

    public MessageSchemaEntity(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
