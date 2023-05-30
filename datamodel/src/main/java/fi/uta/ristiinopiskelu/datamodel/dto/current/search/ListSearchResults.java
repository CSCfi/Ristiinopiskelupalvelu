package fi.uta.ristiinopiskelu.datamodel.dto.current.search;

import java.util.ArrayList;
import java.util.List;

public class ListSearchResults<T> implements SearchResults<T> {

    private List<T> results = new ArrayList<>();

    public ListSearchResults() {
    }

    public ListSearchResults(List<T> results) {
        this.results = results;
    }

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }
}
