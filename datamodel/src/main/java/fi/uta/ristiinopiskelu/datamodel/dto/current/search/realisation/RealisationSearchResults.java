package fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.realisation.RealisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;

import java.util.List;

public class RealisationSearchResults extends ListSearchResults<RealisationReadDTO> {

    public RealisationSearchResults() {
        super();
    }

    public RealisationSearchResults(List<RealisationReadDTO> results) {
        super(results);
    }
}
