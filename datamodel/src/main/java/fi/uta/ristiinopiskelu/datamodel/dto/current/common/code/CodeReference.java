package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code;

import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeReferenceEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * fi: Lähettäessä tarvitsee täytää vain avain-tieto, koska koodien arvot on
 * tallennettu jo valmiiksi ristiinopiskelupalveluun
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on
 *         https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
@Schema(name = "CodeReferenceV9", title = "CodeReferenceV9")
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

    public static CodeReference from(CodeReferenceEntity source) {
        if (source == null) {
            return null;
        }
        return new CodeReference(source.getKey(), source.getCodeSetKey());
    }

    public static List<CodeReference> from(List<CodeReferenceEntity> source) {
        if (source == null) {
            return null;
        }
        return source.stream()
            .map(CodeReference::from)
            .toList();
    }
}
