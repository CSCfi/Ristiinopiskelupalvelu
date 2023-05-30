package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;

import java.util.List;

public class DefaultStudyElementSearchResults<T extends AbstractStudyElementReadDTO> extends StudyElementSearchResults<T> {

    public DefaultStudyElementSearchResults() {
        super();
    }

    public DefaultStudyElementSearchResults(List<T> results) {
        super(results);
    }
}
