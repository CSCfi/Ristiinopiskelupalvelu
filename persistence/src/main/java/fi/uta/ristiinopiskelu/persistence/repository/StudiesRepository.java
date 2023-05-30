package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchRealisationQueries;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.IOException;
import java.util.List;

@NoRepositoryBean
public interface StudiesRepository {

    SearchResponse findAllStudiesByParentReferences(String referenceIdentifier, String referenceOrganizer);

    SearchResponse findAllStudies(BoolQueryBuilder searchQueryBuilder, StudiesSearchRealisationQueries realisationQueriesWithTeachingLanguage,
                                              StudiesSearchRealisationQueries realisationQueriesWithoutTeachingLanguage,
                                              List<String> indices, PageRequest paging, StudiesSearchParameters searchParams);

    String findIndexNameByAlias(String alias) throws IOException;
}
