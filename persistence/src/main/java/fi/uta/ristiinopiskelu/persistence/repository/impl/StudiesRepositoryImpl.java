package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchRealisationQueries;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.persistence.querybuilder.StudiesSearchRequestBuilder;
import fi.uta.ristiinopiskelu.persistence.repository.StudiesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudiesRepositoryImpl implements StudiesRepository {

    private static final Logger logger = LoggerFactory.getLogger(StudiesRepositoryImpl.class);

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public SearchResponse<StudyElementEntity> findAllStudiesByParentReferences(String referenceIdentifier, String referenceOrganizer) {
        Query query = new Query.Builder().bool(q -> q
                        .must(q2 -> q2
                                .nested(nq -> nq
                                        .path("parents")
                                        .scoreMode(ChildScoreMode.None)
                                        .query(q3 -> q3
                                                .bool(bq -> bq
                                                        .must(q4 -> q4
                                                                .term(tq -> tq
                                                                        .field("parents.referenceIdentifier")
                                                                        .value(referenceIdentifier)))
                                                        .must(q4 -> q4
                                                                .term(tq -> tq
                                                                        .field("parents.referenceOrganizer")
                                                                        .value(referenceOrganizer))))))))
                .build();
        
        SearchRequest searchRequest = SearchRequest.of(q -> q.index("opintojaksot", "opintokokonaisuudet", "tutkinnot").query(query));
        return elasticsearchTemplate.execute(client -> client.search(searchRequest, StudyElementEntity.class));
    }

    @Override
    public SearchResponse<StudyElementEntity> findAllStudies(BoolQuery.Builder query, StudiesSearchRealisationQueries realisationQueriesWithTeachingLanguage,
                                                     StudiesSearchRealisationQueries realisationQueriesWithoutTeachingLanguage,
                                                     List<String> indices, PageRequest paging, StudiesSearchParameters searchParams) {

        SearchRequest searchRequest = new StudiesSearchRequestBuilder().build(indices, query, realisationQueriesWithTeachingLanguage,
            realisationQueriesWithoutTeachingLanguage, paging, searchParams);

        return elasticsearchTemplate.execute(client -> client.search(searchRequest, StudyElementEntity.class));
    }
}
