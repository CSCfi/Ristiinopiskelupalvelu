package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.virta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtaCode  {
    private String koodiUri;
    private String resourceUri;
    private VirtaCodeSet koodisto;
    private String koodiArvo;
    private Integer versio;
    private Date paivitysPvm;
    private Date voimassaAlkuPvm;
    private Date voimassaLoppuPvm;
    private String tila;
    private List<VirtaCodeValue> metadata;

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public Date getPaivitysPvm() {
        return paivitysPvm;
    }

    public Date getVoimassaAlkuPvm() {
        return voimassaAlkuPvm;
    }

    public Date getVoimassaLoppuPvm() {
        return voimassaLoppuPvm;
    }

    public Integer getVersio() {
        return versio;
    }

    public VirtaCodeSet getKoodisto() {
        return koodisto;
    }

    public List<VirtaCodeValue> getMetadata() {
        return metadata;
    }

    public String getKoodiArvo() {
        return koodiArvo;
    }

    public String getKoodiUri() {
        return koodiUri;
    }

    public String getTila() {
        return tila;
    }

    public void setKoodiArvo(String koodiArvo) {
        this.koodiArvo = koodiArvo;
    }

    public void setKoodisto(VirtaCodeSet koodisto) {
        this.koodisto = koodisto;
    }

    public void setKoodiUri(String koodiUri) {
        this.koodiUri = koodiUri;
    }

    public void setMetadata(List<VirtaCodeValue> metadata) {
        this.metadata = metadata;
    }

    public void setPaivitysPvm(Date paivitysPvm) {
        this.paivitysPvm = paivitysPvm;
    }

    public void setTila(String tila) {
        this.tila = tila;
    }

    public void setVersio(Integer versio) {
        this.versio = versio;
    }

    public void setVoimassaAlkuPvm(Date voimassaAlkuPvm) {
        this.voimassaAlkuPvm = voimassaAlkuPvm;
    }

    public void setVoimassaLoppuPvm(Date voimassaLoppuPvm) {
        this.voimassaLoppuPvm = voimassaLoppuPvm;
    }

    public VirtaCode() {
    }

    public String toString() {
        return "VirtaCode(koodiUri=" + this.getKoodiUri() + ")";
    }

}
