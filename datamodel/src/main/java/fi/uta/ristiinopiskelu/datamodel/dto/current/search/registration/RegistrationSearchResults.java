package fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.registration.RegistrationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;

import java.util.List;

public class RegistrationSearchResults extends ListSearchResults<RegistrationReadDTO> {

    public RegistrationSearchResults() {
        super();
    }

    public RegistrationSearchResults(List<RegistrationReadDTO> results) {
        super(results);
    }
}
