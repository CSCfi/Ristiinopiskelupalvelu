package fi.uta.ristiinopiskelu.datamodel.dto.current.search.code;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.code.CodeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;

import java.util.List;

public class CodeSearchResults extends ListSearchResults<CodeReadDTO> {

    public CodeSearchResults() {
        super();
    }

    public CodeSearchResults(List<CodeReadDTO> results) {
        super(results);
    }
}
