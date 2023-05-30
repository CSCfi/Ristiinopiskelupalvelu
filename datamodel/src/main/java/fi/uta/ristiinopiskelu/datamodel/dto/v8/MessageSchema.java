package fi.uta.ristiinopiskelu.datamodel.dto.v8;

public class MessageSchema {

    private int schemaVersion;

    public MessageSchema() {

    }

    public MessageSchema(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
