package fi.uta.ristiinopiskelu.datamodel.dto.current.read.messageschema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MessageSchema")
public class MessageSchemaReadDTO {

    private int schemaVersion;

    public MessageSchemaReadDTO() {

    }

    public MessageSchemaReadDTO(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
