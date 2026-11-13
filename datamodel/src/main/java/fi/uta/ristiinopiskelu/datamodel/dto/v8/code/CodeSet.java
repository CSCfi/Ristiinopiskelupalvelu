package fi.uta.ristiinopiskelu.datamodel.dto.v8.code;

import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeSetEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CodeSetV8", title = "CodeSetV8")
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

    public static CodeSet from(fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeSet source) {
        if (source == null) {
            return null;
        }

        fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeSet target = new fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeSet();
        target.setKey(source.getKey());
        target.setOrganisationOid(source.getOrganisationOid());
        target.setVersion(source.getVersion());
        return target;
    }

    public static CodeSet from(CodeSetEntity source) {
        if (source == null) {
            return null;
        }

        fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeSet target = new fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeSet();
        target.setKey(source.getKey());
        target.setOrganisationOid(source.getOrganisationOid());
        target.setVersion(source.getVersion());
        return target;
    }
}
