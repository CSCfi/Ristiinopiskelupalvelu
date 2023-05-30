package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.code;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.Code;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.PageableSearchParameters;

public class CodeSearchParameters extends PageableSearchParameters<Code> {

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
