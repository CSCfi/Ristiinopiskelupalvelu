package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code;

/**
 * fi: Lähettäessä tarvitsee täytää vain avain-tieto, koska koodien arvot on
 * tallennettu jo valmiiksi ristiinopiskelupalveluun
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on
 *         https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class CodeReference {

    private String key;
    private String codeSetKey;

    public CodeReference(String key, String codeSetKey) {
        this.key = key;
        this.codeSetKey = codeSetKey;
    }

    public CodeReference() {}

    /**
     * @return String return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return String return the codeSetKey
     */
    public String getCodeSetKey() {
        return codeSetKey;
    }

    /**
     * @param codeSetKey the codeSetKey to set
     */
    public void setCodeSetKey(String codeSetKey) {
        this.codeSetKey = codeSetKey;
    }

}
