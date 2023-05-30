package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.search;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;

import java.util.List;

public class RealisationSearchResults extends ListSearchResults<RealisationEntity> {

    public RealisationSearchResults() {
        super();
    }

    public RealisationSearchResults(List<RealisationEntity> results) {
        super(results);
    }
}
