package fi.uta.ristiinopiskelu.datamodel.dto.v8.code.virta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtaCodeSet {
    private String koodistoUri;
    private String organisaatioOid;
    private Integer koodistoVersio;

    public Integer getKoodistoVersio() {
        return koodistoVersio;
    }

    public String getKoodistoUri() {
        return koodistoUri;
    }

    public String getOrganisaatioOid() {
        return organisaatioOid;
    }

    public void setKoodistoUri(String koodistoUri) {
        this.koodistoUri = koodistoUri;
    }

    public void setKoodistoVersio(Integer koodistoVersio) {
        this.koodistoVersio = koodistoVersio;
    }

    public void setOrganisaatioOid(String organisaatioOid) {
        this.organisaatioOid = organisaatioOid;
    }
}
