package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.code;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;

import java.util.List;

public class CodeSearchResults extends ListSearchResults<CodeEntity> {

    public CodeSearchResults() {
        super();
    }

    public CodeSearchResults(List<CodeEntity> results) {
        super(results);
    }
}
