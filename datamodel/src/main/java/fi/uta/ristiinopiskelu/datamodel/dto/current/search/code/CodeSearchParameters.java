package fi.uta.ristiinopiskelu.datamodel.dto.current.search.code;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.PageableSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;

public class CodeSearchParameters extends PageableSearchParameters<CodeEntity> {

    private String codeKey;
    private String codeSetKey;

    public String getCodeKey() {
        return codeKey;
    }

    public void setCodeKey(String codeKey) {
        this.codeKey = codeKey;
    }

    public String getCodeSetKey() {
        return codeSetKey;
    }

    public void setCodeSetKey(String codeSetKey) {
        this.codeSetKey = codeSetKey;
    }
}
