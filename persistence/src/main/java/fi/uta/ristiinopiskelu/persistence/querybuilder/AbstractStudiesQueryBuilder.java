package fi.uta.ristiinopiskelu.persistence.querybuilder;

import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractStudiesQueryBuilder extends BoolQueryBuilder {

    public BoolQueryBuilder getUnallowedNetworksFilter(String path, List<NetworkEntity> networkSearchParams) {
        List<String> unpublishedNetworkIds = networkSearchParams.stream().filter(network -> !network.isPublished()).map(NetworkEntity::getId).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(unpublishedNetworkIds)) {
            return null;
        }

        return QueryBuilders.boolQuery()
            .mustNot(QueryBuilders.termsQuery(String.format("%s.id", path), unpublishedNetworkIds));
    }

    protected BoolQueryBuilder getNetworksValidFilter(String path, List<String> networkIds, boolean includeInactive) {
        BoolQueryBuilder allQueries = QueryBuilders.boolQuery();

        allQueries.must(QueryBuilders.termsQuery(String.format("%s.id", path), StringUtils.toStringArray(networkIds)));

        // If not wanting inactive study elements, filter also study elements in which requesting organisations network is not currently valid
        if (!includeInactive) {
            BoolQueryBuilder validityQuery = QueryBuilders.boolQuery()
                .should(QueryBuilders.boolQuery()
                    .must(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(String.format("%s.validityStartDate", path))))
                    .must(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(String.format("%s.validityEndDate", path)))))
                .should(QueryBuilders.boolQuery()
                    .must(QueryBuilders.rangeQuery(String.format("%s.validityStartDate", path)).lte("now/d"))
                    .must(QueryBuilders.boolQuery()
                        .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(String.format("%s.validityEndDate", path))))
                        .should(QueryBuilders.rangeQuery(String.format("%s.validityEndDate", path)).gte("now/d"))));

            allQueries.must(validityQuery);
        }

        return allQueries;
    }
}
