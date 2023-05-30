package fi.uta.ristiinopiskelu.persistence.repository.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepositoryExtended;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class RealisationRepositoryExtendedImpl implements RealisationRepositoryExtended {

    @Autowired
    protected ElasticsearchRestTemplate elasticsearchTemplate;

    @Override
    public List<RealisationEntity> findByAssessmentItemReference(String referenceIdentifier, String referenceOrganizer, String referenceAssessmentItemId) {
        BoolQueryBuilder query = QueryBuilders.boolQuery()
            .must(QueryBuilders.nestedQuery("studyElementReferences", QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("studyElementReferences.referenceIdentifier", referenceIdentifier))
                .must(QueryBuilders.matchQuery("studyElementReferences.referenceOrganizer", referenceOrganizer))
                .must(QueryBuilders.matchQuery("studyElementReferences.referenceType", StudyElementType.ASSESSMENT_ITEM.name()))
                .must(QueryBuilders.matchQuery("studyElementReferences.referenceAssessmentItemId", referenceAssessmentItemId)), ScoreMode.None));

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
            .withQuery(query)
            .build();

        return elasticsearchTemplate.search(builder, RealisationEntity.class).get().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
