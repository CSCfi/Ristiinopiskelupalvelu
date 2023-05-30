package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;

import java.util.List;

public abstract class StudyElementSearchResults<T extends AbstractStudyElementReadDTO> extends ListSearchResults<T> {

    public StudyElementSearchResults() {
        super();
    }

    public StudyElementSearchResults(List<T> results) {
        super(results);
    }
}
