package fi.uta.ristiinopiskelu.persistence.repository.impl;

import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepositoryExtended;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NetworkRepositoryExtendedImpl implements NetworkRepositoryExtended {

    @Autowired
    protected ElasticsearchRestTemplate elasticsearchTemplate;

    @Override
    public Optional<NetworkEntity> findValidNetworkById(String networkId) {
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("id", networkId))
                .must(QueryBuilders.boolQuery()
                        .should(QueryBuilders.rangeQuery("validity.start").lte("now/d")))
                .must(QueryBuilders.boolQuery()
                        .should(QueryBuilders.rangeQuery("validity.end").gte("now/d"))
                        .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("validity.end"))))
                .mustNot(QueryBuilders.termQuery("deleted", true));

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .build();

        return elasticsearchTemplate.search(builder, NetworkEntity.class).get().map(SearchHit::getContent).findFirst();
    }

    @Override
    public List<NetworkEntity> findAllNetworksByOrganisationIdAndNetworkNameByLanguage(String orgId, String name, String lang, Pageable pageable) {
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.nestedQuery("organisations", QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("organisations.organisationTkCode", orgId)), ScoreMode.None))
                .filter(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name.values." + lang, name)))
                .mustNot(QueryBuilders.termQuery("deleted", true));

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .withPageable(pageable)
                .build();

        return elasticsearchTemplate.search(builder, NetworkEntity.class).get().map(SearchHit::getContent).collect(Collectors.toList());
    }

    @Override
    public Optional<NetworkEntity> findNetworkByOrganisationIdAndNetworkId(String orgId, String id) {
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.nestedQuery("organisations", QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("organisations.organisationTkCode", orgId)), ScoreMode.None))
                .filter(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("_id", id)))
                .mustNot(QueryBuilders.termQuery("deleted", true));

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .build();

        return elasticsearchTemplate.search(builder, NetworkEntity.class).get().map(SearchHit::getContent).findFirst();
    }

    @Override
    public List<NetworkEntity> findAllNetworksByOrganisationId(String orgId, Pageable pageable) {
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.nestedQuery("organisations", QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("organisations.organisationTkCode", orgId)), ScoreMode.None))
                .mustNot(QueryBuilders.termQuery("deleted", true));

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .withSort(SortBuilders.fieldSort("abbreviation"))
                .withPageable(pageable)
                .build();

        return elasticsearchTemplate.search(builder, NetworkEntity.class).get().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
