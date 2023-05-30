package fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;

import java.util.List;

public class RegistrationAmountSearchResults extends ListSearchResults<RegistrationAmountSearchResult> {

    public RegistrationAmountSearchResults() {
        super();
    }

    public RegistrationAmountSearchResults(List<RegistrationAmountSearchResult> results) {
        super(results);
    }
}
