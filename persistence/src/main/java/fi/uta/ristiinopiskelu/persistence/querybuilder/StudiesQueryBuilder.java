package fi.uta.ristiinopiskelu.persistence.querybuilder;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StudiesQueryBuilder extends AbstractStudiesQueryBuilder {

    public void filterByComposedId(String id, String code, String organizingOrganisationId) {
        if(StringUtils.hasText(id)) {
            this.must(q -> q.term(tq -> tq.field("studyElementId").value(id)));
        }

        if(StringUtils.hasText(code)) {
            this.must(q -> q.match(mq -> mq.field("studyElementIdentifierCode").query(code)));
        }

        if(StringUtils.hasText(organizingOrganisationId)) {
            this.must(q -> q.term(tq -> tq.field("organizingOrganisationId").value(organizingOrganisationId)));
        }
    }

    public void filterByOrganizingOrganisationIds(List<String> organizingOrganisationIds) {
        this.must(getOrganizingOrganisationIdsFilter(organizingOrganisationIds));
    }

    public void filterByName(String query, Language lang) {
        // Leading wildcard queries might be slow in elasticsearch.
        // In case this query ever gets too slow, it might be because of this. Then ngram tokenizer or some other solution might be required for name field

        String formattedQuery = String.format("*%s*", query.toLowerCase());
        this.must(q -> q.bool(bq -> bq
            .should(q2 -> q2.
                wildcard(wq -> wq
                    .field(String.format("name.values.%s.lowercase", lang.getValue()))
                    .value(formattedQuery)))
            .should(q2 -> q2
                .wildcard(wq -> wq
                    .field("studyElementIdentifierCode.lowercase")
                    .value(formattedQuery)))));
    }

    public void filterOnlyValid() {
        this.must(q -> q.bool(bq -> bq
                .should(q2 -> q2.bool(bq2 -> bq2
                        .must(q3 -> q3.bool(bq3 -> bq3.mustNot(eq -> eq.exists(eq2 -> eq2.field("validityStartDate")))))
                        .must(q3 -> q3.bool(bq3 -> bq3.mustNot(eq -> eq.exists(eq2 -> eq2.field("validityEndDate")))))))
                .should(q2 -> q2.bool(bq2 -> bq2
                        .must(rq -> rq.range(rq2 -> rq2.field("validityStartDate").lte(JsonData.of("now/d"))))
                        .must(bq3 -> bq3.bool(bq4 -> bq4
                                .should(bq5 -> bq5.bool(bq6 -> bq6.mustNot(eq -> eq.exists(eq2 -> eq2.field("validityEndDate")))))
                                .should(rq -> rq.range(rq2 -> rq2.field("validityEndDate").gte(JsonData.of("now/d"))))))))));
    }

    public void filterByStatuses(List<StudyStatus> statuses) {
        if(!CollectionUtils.isEmpty(statuses)) {
            statuses.removeIf(Objects::isNull);
        }

        if(CollectionUtils.isEmpty(statuses)) {
            statuses = Arrays.asList(StudyStatus.ACTIVE);
        }

        List<StudyStatus> finalStatuses = statuses;
        this.must(q -> q
            .terms(tq -> tq
                .field("status")
                .terms(t -> t.value(finalStatuses.stream().map(Enum::name).map(FieldValue::of).collect(Collectors.toList())))));
    }

    public void filterByMinEduGuidanceAreas(List<MinEduGuidanceArea> minEduGuidanceAreas) {
        if(CollectionUtils.isEmpty(minEduGuidanceAreas)) {
            return;
        }

        this.must(q -> q
            .terms(tq -> tq
                .field("minEduGuidanceArea")
                .terms(t -> t.value(minEduGuidanceAreas.stream()
                    .map(MinEduGuidanceArea::getCode)
                    .map(FieldValue::of)
                    .toList()))));
    }

    public void filterByTeachingLanguages(List<String> teachingLanguages) {
        if (!CollectionUtils.isEmpty(teachingLanguages)) {

            BoolQuery.Builder studyElementQuery = new BoolQuery.Builder();

            List<FieldValue> teachingLanguageValuesExcludingUnspecified = teachingLanguages.stream()
                .filter(tl -> !tl.equals(TeachingLanguage.UNSPECIFIED.getValue()))
                .map(FieldValue::of)
                .collect(Collectors.toList());

            if (teachingLanguages.contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                if (!CollectionUtils.isEmpty(teachingLanguageValuesExcludingUnspecified)) {
                    studyElementQuery.should(q -> q
                        .bool(bq -> bq
                            .mustNot(eq -> eq.exists(eq2 -> eq2.field("teachingLanguage.lowercase")))));
                } else {
                    studyElementQuery.must(q -> q
                        .bool(bq -> bq
                            .mustNot(eq -> eq.exists(eq2 -> eq2.field("teachingLanguage.lowercase")))));
                }
            }

            if (!CollectionUtils.isEmpty(teachingLanguageValuesExcludingUnspecified)) {
                if (teachingLanguages.contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                    studyElementQuery.should(q -> q
                        .terms(tq -> tq
                            .field("teachingLanguage.lowercase")
                            .terms(tqf -> tqf.value(teachingLanguageValuesExcludingUnspecified))));
                } else {
                    studyElementQuery.must(q -> q
                        .terms(tq -> tq
                            .field("teachingLanguage.lowercase")
                            .terms(tqf -> tqf.value(teachingLanguageValuesExcludingUnspecified))));
                }
            }

            this.must(studyElementQuery.build()._toQuery());
        }
    }

    public void filterByCooperationNetworks(String organisationId, List<NetworkEntity> organisationNetworkIds, List<String> networkIdSearchParams,
                                            boolean includeInactive, boolean includeOwn) {
        this.must(getCooperationNetworksFilter(organisationId, organisationNetworkIds, networkIdSearchParams, includeInactive,
            includeOwn));
    }

    private Query getCooperationNetworksFilter(String organisationId, List<NetworkEntity> organisationNetworks,
                                                    List<String> networkIdSearchParams, boolean includeInactive, boolean includeOwn) {
        // own organisation filter
        Query ownOrganisationQuery = getOrganizingOrganisationIdsFilter(Collections.singletonList(organisationId));

        // the main query
        BoolQuery.Builder query = new BoolQuery.Builder();

        Query networkQuery = null;

        // network query. always search only our own networks if there is any
        if(!CollectionUtils.isEmpty(organisationNetworks)) {
            BoolQuery.Builder networkQueryBuilder = new BoolQuery.Builder()
                .must(getCooperationNetworksFilter(organisationNetworks.stream().map(NetworkEntity::getId).collect(Collectors.toList()), includeInactive));

            // if network search params were given, filter also by those. hence, if only networks outside of organisation
            // own networks were given, no results would be returned
            if (!CollectionUtils.isEmpty(networkIdSearchParams)) {
                networkQueryBuilder.must(getCooperationNetworksFilter(networkIdSearchParams, includeInactive));
            }

            // also filter out unpublished networks
            Query unallowedCooperatoinNetworksFilter = getUnallowedCooperationNetworksFilter(organisationNetworks);
            if(unallowedCooperatoinNetworksFilter != null) {
                networkQueryBuilder.must(unallowedCooperatoinNetworksFilter);
            }

            networkQuery = networkQueryBuilder.build()._toQuery();
        }

        if(networkQuery != null) {
            final Query finalNetworkQuery = networkQuery;

            if (includeOwn && includeInactive) {
                query.must(q -> q
                    .bool(bq -> bq
                        .should(finalNetworkQuery)
                        .should(ownOrganisationQuery)));
            } else {
                query.must(networkQuery);
            }
        } else {
            query.must(ownOrganisationQuery);
        }

        if(!includeOwn) {
            query.mustNot(ownOrganisationQuery);
        }

        return query.build()._toQuery();
    }

    private Query getOrganizingOrganisationIdsFilter(List<String> organizingOrganisationIds) {
        return new Query.Builder().terms(tq -> tq
            .field("organizingOrganisationId")
            .terms(tqf -> tqf
                .value(organizingOrganisationIds.stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList()))))
            .build();
    }

    private Query getCooperationNetworksFilter(List<String> organisationNetworkIds, boolean includeInactive) {
        return new Query.Builder().nested(nq -> nq
            .path("cooperationNetworks")
            .query(getNetworksValidFilter("cooperationNetworks", organisationNetworkIds, includeInactive))
            .scoreMode(ChildScoreMode.None))
            .build();
    }

    private Query getUnallowedCooperationNetworksFilter(List<NetworkEntity> organisationNetworks) {
        Query query = getUnallowedNetworksFilter("cooperationNetworks", organisationNetworks);
        if(query == null) {
            return null;
        }

        return new Query.Builder().nested(nq -> nq
            .path("cooperationNetworks")
            .query(query)
            .scoreMode(ChildScoreMode.None))
            .build();
    }
}
