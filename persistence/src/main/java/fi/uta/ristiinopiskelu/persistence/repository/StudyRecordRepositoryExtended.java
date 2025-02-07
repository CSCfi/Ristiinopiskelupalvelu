package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordAmountSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

public interface StudyRecordRepositoryExtended {

    List<StudyRecordEntity> findAllByParams(StudyRecordSearchParameters searchParameters);

    SearchHits<StudyRecordEntity> findAmounts(StudyRecordAmountSearchParameters searchParams);
}
