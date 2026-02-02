package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CodeSetV9", title = "CodeSetV9")
public class CodeSet {

    private String key;
    private String organisationOid;
    private Integer version;

    public CodeSet() {}

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public Integer getVersion() {
        return version;
    }

    public String getOrganisationOid() {
        return organisationOid;
    }

    public void setOrganisationOid(String organisationOid) {
        this.organisationOid = organisationOid;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
