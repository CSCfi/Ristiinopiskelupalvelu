package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.json.JsonData;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepositoryExtended;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;

import java.util.List;
import java.util.Optional;

public class NetworkRepositoryExtendedImpl implements NetworkRepositoryExtended {

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public Optional<NetworkEntity> findNetworkById(String networkId) {
        BoolQuery.Builder query = new BoolQuery.Builder()
            .must(q -> q.match(mq -> mq.field("id").query(networkId)))
            .mustNot(q -> q.term(tq -> tq.field("deleted").value(true)));

        NativeQuery builder = new NativeQueryBuilder()
            .withQuery(query.build()._toQuery())
            .withPageable(Pageable.unpaged())
            .build();

        return elasticsearchTemplate.search(builder, NetworkEntity.class).get().map(SearchHit::getContent).findFirst();
    }

    @Override
    public Optional<NetworkEntity> findValidNetworkById(String networkId) {
        return findValidNetworkById(networkId, true, true);
    }

    @Override
    public Optional<NetworkEntity> findValidNetworkById(String networkId, boolean validityStartMustBeValid, boolean validityEndMustBeValid) {
        BoolQuery.Builder query = new BoolQuery.Builder()
            .must(q -> q.match(mq -> mq.field("id").query(networkId)))
            .mustNot(q -> q.term(tq -> tq.field("deleted").value(true)));

        if(validityStartMustBeValid) {
            query.must(q -> q.bool(bq -> bq
                .should(q2 -> q2.range(rq -> rq.field("validity.start").lte(JsonData.of("now/d"))))));
        }

        if(validityEndMustBeValid) {
            query.must(q -> q.bool(bq -> bq
                .should(q2 -> q2.range(rq -> rq.field("validity.end").gte(JsonData.of("now/d"))))
                .should(q2 -> q2.bool(bq2 -> bq2.mustNot(q3 -> q3.exists(eq -> eq.field("validity.end")))))));
        }

        NativeQuery builder = new NativeQueryBuilder()
            .withQuery(query.build()._toQuery())
            .withPageable(Pageable.unpaged())
            .build();

        return elasticsearchTemplate.search(builder, NetworkEntity.class).get().map(SearchHit::getContent).findFirst();
    }

    @Override
    public List<NetworkEntity> findAllNetworksByOrganisationIdAndNetworkNameByLanguage(String orgId, String name, String lang, Pageable pageable) {
        BoolQuery.Builder query = new BoolQuery.Builder()
                .must(q -> q
                    .nested(nq -> nq
                        .path("organisations")
                        .scoreMode(ChildScoreMode.None)
                        .query(q2 -> q2
                            .bool(bq -> bq
                                .must(q3 -> q3
                                    .match(mq -> mq
                                        .field("organisations.organisationTkCode")
                                        .query(orgId)))))))
                .filter(q -> q
                    .bool(bq -> bq
                        .must(q2 -> q2
                            .match(mq -> mq
                                .field("name.values." + lang)
                                .query(name)))))
                .mustNot(q -> q
                    .term(tq -> tq
                        .field("deleted")
                        .value(true)));

        NativeQuery builder = new NativeQueryBuilder()
                .withQuery(query.build()._toQuery())
                .withPageable(pageable)
                .build();

        return elasticsearchTemplate.search(builder, NetworkEntity.class).get()
                .map(SearchHit::getContent)
                .toList();
    }

    @Override
    public Optional<NetworkEntity> findNetworkByOrganisationIdAndNetworkId(String orgId, String id) {
        BoolQuery.Builder query = new BoolQuery.Builder()
                .must(q -> q
                    .nested(nq -> nq
                        .path("organisations")
                        .scoreMode(ChildScoreMode.None)
                        .query(q2 -> q2
                            .bool(bq -> bq
                                .must(q3 -> q3
                                    .match(mq -> mq
                                        .field("organisations.organisationTkCode")
                                        .query(orgId)))))))
                .filter(q -> q
                    .bool(bq -> bq
                        .must(q2 -> q2
                            .match(mq -> mq
                                .field("_id")
                                .query(id)))))
                .mustNot(q -> q
                    .term(tq -> tq
                        .field("deleted")
                        .value(true)));

        NativeQuery builder = new NativeQueryBuilder()
                .withQuery(query.build()._toQuery())
                .withPageable(Pageable.unpaged())
                .build();

        return elasticsearchTemplate.search(builder, NetworkEntity.class).get().map(SearchHit::getContent).findFirst();
    }

    @Override
    public List<NetworkEntity> findAllNetworksByOrganisationId(String orgId, Pageable pageable) {
        BoolQuery.Builder query = new BoolQuery.Builder()
                .must(q -> q
                    .nested(nq -> nq
                        .path("organisations")
                        .scoreMode(ChildScoreMode.None)
                        .query(q2 -> q2
                            .bool(bq -> bq
                                .must(q3 -> q3
                                    .match(mq -> mq
                                        .field("organisations.organisationTkCode")
                                        .query(orgId)))))))
                .mustNot(q -> q
                    .term(tq -> tq
                        .field("deleted")
                        .value(true)));

        NativeQuery builder = new NativeQueryBuilder()
                .withQuery(query.build()._toQuery())
                .withSort(s -> s.field(fs -> fs.field("abbreviation")))
                .withPageable(pageable)
                .build();

        return elasticsearchTemplate.search(builder, NetworkEntity.class).get()
                .map(SearchHit::getContent)
                .toList();
    }
}
