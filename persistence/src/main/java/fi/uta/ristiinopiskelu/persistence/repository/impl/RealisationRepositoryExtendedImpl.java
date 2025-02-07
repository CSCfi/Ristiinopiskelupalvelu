package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepositoryExtended;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;

import java.util.List;

public class RealisationRepositoryExtendedImpl implements RealisationRepositoryExtended {

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public List<RealisationEntity> findByAssessmentItemReference(String referenceIdentifier, String referenceOrganizer, String referenceAssessmentItemId) {
        BoolQuery.Builder query = new BoolQuery.Builder()
            .must(q -> q
                .nested(nq -> nq
                    .path("studyElementReferences")
                    .scoreMode(ChildScoreMode.None)
                    .query(q2 -> q2
                        .bool(bq -> bq
                            .must(q3 -> q3
                                .match(mq -> mq
                                    .field("studyElementReferences.referenceIdentifier")
                                    .query(referenceIdentifier)))
                            .must(q3 -> q3
                                .match(mq -> mq
                                    .field("studyElementReferences.referenceOrganizer")
                                    .query(referenceOrganizer)))
                            .must(q3 -> q3
                                .match(mq -> mq
                                    .field("studyElementReferences.referenceType")
                                    .query(StudyElementType.ASSESSMENT_ITEM.name())))
                            .must(q3 -> q3
                                .match(mq -> mq
                                    .field("studyElementReferences.referenceAssessmentItemId")
                                    .query(referenceAssessmentItemId)))))));

        NativeQuery builder = new NativeQueryBuilder()
                .withQuery(query.build()._toQuery())
                .withPageable(Pageable.unpaged())
                .build();

        return elasticsearchTemplate.search(builder, RealisationEntity.class).get()
                .map(SearchHit::getContent)
                .toList();
    }
}
