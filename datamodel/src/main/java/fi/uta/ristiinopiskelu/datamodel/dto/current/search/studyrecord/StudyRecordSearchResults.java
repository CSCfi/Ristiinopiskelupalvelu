package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyrecord.StudyRecordReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;

import java.util.List;

public class StudyRecordSearchResults extends ListSearchResults<StudyRecordReadDTO> {

    public StudyRecordSearchResults() {
        super();
    }

    public StudyRecordSearchResults(List<StudyRecordReadDTO> results) {
        super(results);
    }
}
