package fi.uta.ristiinopiskelu.datamodel.dto.current.write.messageschema;

public class MessageSchemaWriteDTO {

    private int schemaVersion;

    public MessageSchemaWriteDTO() {

    }

    public MessageSchemaWriteDTO(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
