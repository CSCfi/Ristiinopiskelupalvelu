package fi.uta.ristiinopiskelu.datamodel.dto.current.search.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;

import java.util.List;

public class StudentSearchResults extends ListSearchResults<StudentSearchResult> {

    public StudentSearchResults() {
        super();
    }

    public StudentSearchResults(List<StudentSearchResult> students) {
        super(students);
    }
}
