package fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration;

import java.util.HashMap;
import java.util.Map;

public class RegistrationAmountSearchResult {

    private RegistrationSelectionIdentifier identifier;
    private Map<String, Integer> amounts = new HashMap();

    public RegistrationAmountSearchResult() {
        
    }

    public RegistrationAmountSearchResult(RegistrationSelectionIdentifier identifier, Map<String, Integer> amounts) {
        this.identifier = identifier;
        this.amounts = amounts;
    }

    public RegistrationSelectionIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(RegistrationSelectionIdentifier identifier) {
        this.identifier = identifier;
    }

    public Map<String, Integer> getAmounts() {
        return amounts;
    }

    public void setAmounts(Map<String, Integer> amounts) {
        this.amounts = amounts;
    }
}
