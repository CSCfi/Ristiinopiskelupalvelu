package fi.uta.ristiinopiskelu.persistence.querybuilder;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CourseUnitReference;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.util.StringUtils;

import java.util.List;

public class RealisationQueryBuilder extends StudiesQueryBuilder {
    public void filterOngoingEnrollments() {
        String formattedNow = DateUtils.getFormattedNow();
        BoolQueryBuilder onGoingEnrollment = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("enrollmentStartDateTime").lte(formattedNow))
                .must(QueryBuilders.boolQuery()
                        .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("enrollmentEndDateTime")))
                        .should(QueryBuilders.rangeQuery("enrollmentEndDateTime").gte(formattedNow)));

        this.must(onGoingEnrollment);
    }

    @Override
    public void filterByComposedId(String id, String code, String organizingOrganisationId) {
        if(StringUtils.hasText(id)) {
            this.must(QueryBuilders.termQuery("realisationId", id));
        }

        if(StringUtils.hasText(code)) {
            this.must(QueryBuilders.matchQuery("realisationIdentifierCode", code));
        }

        if(StringUtils.hasText(organizingOrganisationId)) {
            this.must(QueryBuilders.termQuery("organizingOrganisationId", organizingOrganisationId));
        }
    }

    public void filterByCourseUnitReferences(List<CourseUnitReference> courseUnitReferences) {
        BoolQueryBuilder multipleReferencesQuery = QueryBuilders.boolQuery();

        for(CourseUnitReference ref : courseUnitReferences) {
            BoolQueryBuilder courseUnitReferenceQuery = QueryBuilders.boolQuery();
            if(ref.getCourseUnitId() != null) {
                courseUnitReferenceQuery.must(QueryBuilders.matchQuery("studyElementReferences.referenceIdentifier", ref.getCourseUnitId()));
            }

            if(ref.getOrganizingOrganisationId() != null) {
                courseUnitReferenceQuery.must(QueryBuilders.matchQuery("studyElementReferences.referenceOrganizer", ref.getOrganizingOrganisationId()));
            }

            multipleReferencesQuery.should(courseUnitReferenceQuery);
        }

        NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("studyElementReferences", multipleReferencesQuery, ScoreMode.None);
        this.must(nestedQueryBuilder);
    }

    public void filterPastEndDate() {
        this.must(QueryBuilders.rangeQuery("endDate").gte("now/d"));
    }
}
