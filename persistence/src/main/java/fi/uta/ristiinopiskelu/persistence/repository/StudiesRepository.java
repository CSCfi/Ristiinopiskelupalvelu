package fi.uta.ristiinopiskelu.persistence.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchRealisationQueries;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface StudiesRepository {

    SearchResponse<StudyElementEntity> findAllStudiesByParentReferences(String referenceIdentifier, String referenceOrganizer);

    SearchResponse<StudyElementEntity> findAllStudies(BoolQuery.Builder searchQueryBuilder, StudiesSearchRealisationQueries realisationQueriesWithTeachingLanguage,
                                                      StudiesSearchRealisationQueries realisationQueriesWithoutTeachingLanguage,
                                                      List<String> indices, PageRequest paging, StudiesSearchParameters searchParams);
}
