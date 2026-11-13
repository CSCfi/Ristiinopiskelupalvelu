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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class StudiesRepositoryImpl implements StudiesRepository {

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Override
    public List<StudyElementEntity> findAllStudiesByParentReferences(String referenceIdentifier, String referenceOrganizer) {
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

        NativeQuery nativeQuery = new NativeQueryBuilder()
                .withQuery(query)
                .withPageable(Pageable.unpaged())
                .withReactiveBatchSize(ElasticsearchQueryUtils.SEARCH_BATCH_SIZE)
                .build();

        try (SearchHitsIterator<Map> searchHits = elasticsearchTemplate.searchForStream(nativeQuery, Map.class,
                IndexCoordinates.of("opintojaksot", "opintokokonaisuudet", "tutkinnot"))) {
            return searchHits.stream()
                    .map(SearchHit::getContent)
                    .map(source -> jsonMapper.convertValue(source, StudyElementEntity.class))
                    .toList();
        }
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
