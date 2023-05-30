package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyElementRestDTO;

import java.util.List;

public abstract class StudyElementSearchResults<T extends StudyElementRestDTO> extends ListSearchResults<T> {

    public StudyElementSearchResults() {
        super();
    }

    public StudyElementSearchResults(List<T> results) {
        super(results);
    }
}
