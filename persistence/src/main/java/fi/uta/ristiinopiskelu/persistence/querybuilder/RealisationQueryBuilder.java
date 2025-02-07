package fi.uta.ristiinopiskelu.persistence.querybuilder;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.json.JsonData;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CourseUnitReference;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.springframework.util.StringUtils;

import java.util.List;

public class RealisationQueryBuilder extends StudiesQueryBuilder {

    public void filterOngoingEnrollments() {
        String formattedNow = DateUtils.getFormattedNow();
        this.must(q -> q.bool(bq -> bq
                .must(q2 -> q2.range(rq -> rq
                    .field("enrollmentStartDateTime")
                    .lte(JsonData.of(formattedNow))))
                .must(q2 -> q2.bool(bq2 -> bq2
                        .should(q3 -> q3.bool(bq3 -> bq3
                            .mustNot(q4 -> q4
                                .exists(eq -> eq.field("enrollmentEndDateTime")))))
                        .should(q3 -> q3.range(rq -> rq
                            .field("enrollmentEndDateTime")
                            .gte(JsonData.of(formattedNow))))))));
    }

    @Override
    public void filterByComposedId(String id, String code, String organizingOrganisationId) {
        if(StringUtils.hasText(id)) {
            this.must(q -> q
                .term(tq -> tq
                    .field("realisationId")
                    .value(id)));
        }

        if(StringUtils.hasText(code)) {
            this.must(q -> q
                .match(mq -> mq
                    .field("realisationIdentifierCode")
                    .query(code)));
        }

        if(StringUtils.hasText(organizingOrganisationId)) {
            this.must(q -> q
                .term(tq -> tq
                    .field("organizingOrganisationId")
                    .value(organizingOrganisationId)));
        }
    }

    public void filterByCourseUnitReferences(List<CourseUnitReference> courseUnitReferences) {
        BoolQuery.Builder multipleReferencesQuery = new BoolQuery.Builder();

        for(CourseUnitReference ref : courseUnitReferences) {
            BoolQuery.Builder courseUnitReferenceQuery = new BoolQuery.Builder();
            if(ref.getCourseUnitId() != null) {
                courseUnitReferenceQuery.must(q -> q
                    .match(mq -> mq
                        .field("studyElementReferences.referenceIdentifier")
                        .query(ref.getCourseUnitId())));
            }

            if(ref.getOrganizingOrganisationId() != null) {
                courseUnitReferenceQuery.must(q -> q
                    .match(mq -> mq
                        .field("studyElementReferences.referenceOrganizer")
                        .query(ref.getOrganizingOrganisationId())));
            }

            multipleReferencesQuery.should(courseUnitReferenceQuery.build()._toQuery());
        }

        this.must(q -> q
            .nested(nq -> nq
                .path("studyElementReferences")
                .query(multipleReferencesQuery.build()._toQuery())
                .scoreMode(ChildScoreMode.None)));
    }

    public void filterPastEndDate() {
        this.must(q -> q
            .range(rq -> rq
                .field("endDate")
                .gte(JsonData.of("now/d"))));
    }
}
