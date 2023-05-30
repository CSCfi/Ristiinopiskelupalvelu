package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyElementRestDTO;

import java.util.List;

public class DefaultStudyElementSearchResults<R extends StudyElementRestDTO> extends StudyElementSearchResults<R> {

    public DefaultStudyElementSearchResults() {
        super();
    }

    public DefaultStudyElementSearchResults(List<R> results) {
        super(results);
    }
}
