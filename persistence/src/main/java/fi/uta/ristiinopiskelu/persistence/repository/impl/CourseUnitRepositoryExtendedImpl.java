package fi.uta.ristiinopiskelu.persistence.repository.impl;

import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepositoryExtended;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.Optional;

public class CourseUnitRepositoryExtendedImpl implements CourseUnitRepositoryExtended {

    @Autowired
    protected ElasticsearchRestTemplate elasticsearchTemplate;

    @Override
    public Optional<CourseUnitEntity> findByIdAndAssessmentItemIdAndOrganizer(String id, String assessmentItemId, String organizingOrganisationId) {
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("studyElementId", id))
                .must(QueryBuilders.matchQuery("organizingOrganisationId", organizingOrganisationId))
                .must(QueryBuilders.nestedQuery("completionOptions", QueryBuilders.boolQuery()
                        .must(QueryBuilders.nestedQuery("completionOptions.assessmentItems", QueryBuilders.boolQuery()
                                .must(QueryBuilders.matchQuery("completionOptions.assessmentItems.assessmentItemId", assessmentItemId)), ScoreMode.None)),
                        ScoreMode.None));

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .build();

        return elasticsearchTemplate.search(builder, CourseUnitEntity.class).get().map(SearchHit::getContent).findFirst();
    }
}
