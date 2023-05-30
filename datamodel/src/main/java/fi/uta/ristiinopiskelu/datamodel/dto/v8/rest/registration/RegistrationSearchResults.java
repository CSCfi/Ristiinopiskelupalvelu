package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;

import java.util.List;

public class RegistrationSearchResults extends ListSearchResults<RegistrationEntity> {

    public RegistrationSearchResults() {
        super();
    }

    public RegistrationSearchResults(List<RegistrationEntity> results) {
        super(results);
    }
}
