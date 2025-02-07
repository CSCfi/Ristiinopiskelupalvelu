package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepositoryExtended;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;

import java.util.Optional;

public class CourseUnitRepositoryExtendedImpl implements CourseUnitRepositoryExtended {

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public Optional<CourseUnitEntity> findByIdAndAssessmentItemIdAndOrganizer(String id, String assessmentItemId, String organizingOrganisationId) {
        BoolQuery.Builder query = new BoolQuery.Builder()
                .must(q -> q
                    .match(mq -> mq
                        .field("studyElementId")
                        .query(id)))
                .must(q -> q
                    .match(mq -> mq
                        .field("organizingOrganisationId")
                        .query(organizingOrganisationId)))
                .must(q -> q
                    .nested(nq -> nq
                        .path("completionOptions.assessmentItems")
                        .scoreMode(ChildScoreMode.None)
                        .query(q2 -> q2
                            .bool(bq -> bq
                                .must(q3 -> q3
                                    .match(mq -> mq
                                        .field("completionOptions.assessmentItems.assessmentItemId")
                                        .query(assessmentItemId)))))));

        NativeQuery builder = new NativeQueryBuilder()
                .withQuery(query.build()._toQuery())
                .withPageable(Pageable.unpaged())
                .build();

        return elasticsearchTemplate.search(builder, CourseUnitEntity.class).get().map(SearchHit::getContent).findFirst();
    }
}
