package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.virta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtaCodeValue {
    private String kieli;
    private String nimi;
    private String kuvaus;
    private String lyhytNimi;

    public String getKieli() {
        return kieli;
    }

    public String getKuvaus() {
        return kuvaus;
    }

    public String getLyhytNimi() {
        return lyhytNimi;
    }

    public String getNimi() {
        return nimi;
    }

    public void setKieli(String kieli) {
        this.kieli = kieli;
    }

    public void setKuvaus(String kuvaus) {
        this.kuvaus = kuvaus;
    }

    public void setLyhytNimi(String lyhytNimi) {
        this.lyhytNimi = lyhytNimi;
    }

    public void setNimi(String nimi) {
        this.nimi = nimi;
    }
}
