package fi.uta.ristiinopiskelu.persistence.querybuilder;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractStudiesQueryBuilder extends BoolQuery.Builder {

    public Query getUnallowedNetworksFilter(String path, List<NetworkEntity> networkSearchParams) {
        List<String> unpublishedNetworkIds = networkSearchParams.stream().filter(network -> !network.isPublished()).map(NetworkEntity::getId).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(unpublishedNetworkIds)) {
            return null;
        }

        return new Query.Builder().bool(bq -> bq
            .mustNot(q -> new Query.Builder().terms(tq -> tq
                    .field(String.format("%s.id", path))
                    .terms(t -> t.value(unpublishedNetworkIds.stream().map(FieldValue::of).collect(Collectors.toList()))))))
                .build();
    }

    protected Query getNetworksValidFilter(String path, List<String> networkIds, boolean includeInactive) {
        BoolQuery.Builder allQueries = new BoolQuery.Builder();

        allQueries.must(new Query.Builder().terms(tq -> tq
                        .field(String.format("%s.id", path))
                        .terms(t -> t.value(networkIds.stream().map(FieldValue::of).collect(Collectors.toList()))))
                .build());

        // If not wanting inactive study elements, filter also study elements in which requesting organisations network is not currently valid
        if (!includeInactive) {
            allQueries.must(must -> new Query.Builder().bool(bq -> bq
                .should(s -> new Query.Builder().bool(bq2 -> bq2
                        .must(m -> new Query.Builder().bool(bq3 -> bq3
                                .mustNot(mn -> new Query.Builder().exists(eq -> eq
                                        .field(String.format("%s.validityStartDate", path))))))
                        .must(m -> new Query.Builder().bool(bq3 -> bq3
                                .mustNot(mn -> new Query.Builder().exists(eq -> eq
                                        .field(String.format("%s.validityEndDate", path))))))))
                .should(s -> new Query.Builder().bool(bq3 -> bq3
                    .must(m -> new Query.Builder().range(rq -> rq
                            .field(String.format("%s.validityStartDate", path))
                            .lte(JsonData.of("now/d"))))
                    .must(m -> new Query.Builder().bool(bq4 -> bq4
                        .should(s2 -> new Query.Builder().bool(bq5 -> bq5
                                        .mustNot(mn -> new Query.Builder().exists(eq -> eq
                                                .field(String.format("%s.validityEndDate", path))))))
                        .should(s2 -> new Query.Builder().range(rq -> rq
                                .field(String.format("%s.validityEndDate", path))
                                .gte(JsonData.of("now/d"))))))))));
        }

        return allQueries.build()._toQuery();
    }
}
