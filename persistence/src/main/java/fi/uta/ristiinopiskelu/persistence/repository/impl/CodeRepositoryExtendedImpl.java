package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSetKeyWithCodeCount;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;
import fi.uta.ristiinopiskelu.persistence.repository.CodeRepositoryExtended;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.*;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CodeRepositoryExtendedImpl implements CodeRepositoryExtended {

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public List<CodeSetKeyWithCodeCount> findCodeSetKeysWithCodeCount() {
        NativeQuery searchQuery = new NativeQueryBuilder()
                .withQuery(q -> q.matchAll(ma -> ma))
                .withAggregation("codeSets", new TermsAggregation.Builder().field("codeSet.key").build()._toAggregation())
                .withMaxResults(0)
                .build();

        SearchHits<CodeEntity> searchHits = elasticsearchTemplate.search(searchQuery, CodeEntity.class);

        ElasticsearchAggregations aggregationContainer = (ElasticsearchAggregations) searchHits.getAggregations();
        if(aggregationContainer != null) {
            Map<String, ElasticsearchAggregation> aggregations = aggregationContainer.aggregationsAsMap();

            ElasticsearchAggregation codeSets = aggregations.get("codeSets");

            if (codeSets != null) {
                return codeSets.aggregation().getAggregate().sterms().buckets().array().stream()
                    .map(cs -> new CodeSetKeyWithCodeCount(cs.key().stringValue(), cs.docCount()))
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
