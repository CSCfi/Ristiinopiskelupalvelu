package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;

import java.util.List;

public class StudyRecordSearchResults extends ListSearchResults<StudyRecordEntity> {

    public StudyRecordSearchResults() {
        super();
    }

    public StudyRecordSearchResults(List<StudyRecordEntity> results) {
        super(results);
    }
}
