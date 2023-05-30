package fi.uta.ristiinopiskelu.persistence.repository.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSetKeyWithCodeCount;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;
import fi.uta.ristiinopiskelu.persistence.repository.CodeRepositoryExtended;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class CodeRepositoryExtendedImpl implements CodeRepositoryExtended {

    @Autowired
    protected ElasticsearchRestTemplate elasticsearchTemplate;

    @Override
    public List<CodeSetKeyWithCodeCount> findCodeSetKeysWithCodeCount() {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(matchAllQuery())
            .addAggregation(AggregationBuilders.terms("codeSets").field("codeSet.key"))
            .build();

        SearchHits<CodeEntity> searchHits = elasticsearchTemplate.search(searchQuery, CodeEntity.class);

        ElasticsearchAggregations aggregationContainer = (ElasticsearchAggregations) searchHits.getAggregations();
        if(aggregationContainer != null) {
            Aggregations aggregations = aggregationContainer.aggregations();

            Terms codeSets = aggregations.get("codeSets");

            if (codeSets != null && !CollectionUtils.isEmpty(codeSets.getBuckets())) {
                return codeSets.getBuckets().stream()
                    .map(cs -> new CodeSetKeyWithCodeCount(cs.getKeyAsString(), cs.getDocCount()))
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
