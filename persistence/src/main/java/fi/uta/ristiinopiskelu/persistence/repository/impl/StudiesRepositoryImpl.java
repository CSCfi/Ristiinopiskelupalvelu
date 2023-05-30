package fi.uta.ristiinopiskelu.persistence.repository.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchRealisationQueries;
import fi.uta.ristiinopiskelu.persistence.querybuilder.StudiesNativeSearchQueryBuilder;
import fi.uta.ristiinopiskelu.persistence.repository.StudiesRepository;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class StudiesRepositoryImpl implements StudiesRepository {

    private static final Logger logger = LoggerFactory.getLogger(StudiesRepositoryImpl.class);

    @Autowired
    protected ElasticsearchRestTemplate elasticsearchTemplate;

    @Override
    public SearchResponse findAllStudiesByParentReferences(String referenceIdentifier, String referenceOrganizer) {
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.nestedQuery("parents", QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("parents.referenceIdentifier", referenceIdentifier))
                        .must(QueryBuilders.matchQuery("parents.referenceOrganizer", referenceOrganizer)), ScoreMode.None));

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .build();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(builder.getQuery());
        SearchRequest searchRequest = new SearchRequest("opintojaksot", "opintokokonaisuudet", "tutkinnot");
        searchRequest.source(searchSourceBuilder);

        return elasticsearchTemplate.execute(client -> client.search(searchRequest, RequestOptions.DEFAULT));
    }

    @Override
    public SearchResponse findAllStudies(BoolQueryBuilder query, StudiesSearchRealisationQueries realisationQueriesWithTeachingLanguage,
                                                     StudiesSearchRealisationQueries realisationQueriesWithoutTeachingLanguage,
                                                     List<String> indices, PageRequest paging, StudiesSearchParameters searchParams) {
        SearchSourceBuilder searchSourceBuilder = new StudiesNativeSearchQueryBuilder().get(query, realisationQueriesWithTeachingLanguage,
            realisationQueriesWithoutTeachingLanguage, paging, searchParams);

        SearchRequest searchRequest = new SearchRequest(indices.toArray(new String[0]), searchSourceBuilder);

        return elasticsearchTemplate.execute(client -> client.search(searchRequest, RequestOptions.DEFAULT));
    }

    @Override
    public String findIndexNameByAlias(String alias) throws IOException {
        IndicesClient ic = elasticsearchTemplate.execute(RestHighLevelClient::indices);
        Map<String, Set<AliasMetadata>> map = ic.getAlias(new GetAliasesRequest(alias), RequestOptions.DEFAULT).getAliases();
        return map.keySet().iterator().next();
    }
}
