package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.code;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.Code;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.PageableSearchParameters;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CodeSearchParametersV8", title = "CodeSearchParametersV8")
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
