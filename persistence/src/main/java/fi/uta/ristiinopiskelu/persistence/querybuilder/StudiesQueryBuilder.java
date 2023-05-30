package fi.uta.ristiinopiskelu.persistence.querybuilder;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

public class StudiesQueryBuilder extends AbstractStudiesQueryBuilder {

    public void filterByComposedId(String id, String code, String organizingOrganisationId) {
        if(StringUtils.hasText(id)) {
            this.must(QueryBuilders.termQuery("studyElementId", id));
        }

        if(StringUtils.hasText(code)) {
            this.must(QueryBuilders.matchQuery("studyElementIdentifierCode", code));
        }

        if(StringUtils.hasText(organizingOrganisationId)) {
            this.must(QueryBuilders.termQuery("organizingOrganisationId", organizingOrganisationId));
        }
    }

    public void filterByOrganizingOrganisationIds(List<String> organizingOrganisationIds) {
        this.must(getOrganizingOrganisationIdsFilter(organizingOrganisationIds));
    }

    public void filterByName(String query, Language lang) {
        // Leading wildcard queries might be slow in elasticsearch.
        // In case this query ever gets too slow, it might be because of this. Then ngram tokenizer or some other solution might be required for name field

        String formattedQuery = String.format("*%s*", query.toLowerCase());
        this.must(QueryBuilders.boolQuery()
            .should(
                wildcardQuery(String.format("name.values.%s.lowercase", lang.getValue()), formattedQuery))
            .should(
                wildcardQuery("studyElementIdentifierCode.lowercase", formattedQuery)));
    }

    public void filterOnlyValid() {
        BoolQueryBuilder onlyActiveStudyElementQuery = QueryBuilders.boolQuery()
                .should(QueryBuilders.boolQuery()
                        .must(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("validityStartDate")))
                        .must(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("validityEndDate"))))
                .should(QueryBuilders.boolQuery()
                        .must(QueryBuilders.rangeQuery("validityStartDate").lte("now/d"))
                        .must(QueryBuilders.boolQuery()
                                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("validityEndDate")))
                                .should(QueryBuilders.rangeQuery("validityEndDate").gte("now/d"))));

        this.must(onlyActiveStudyElementQuery);
    }

    public void filterByStatuses(List<StudyStatus> statuses) {
        if(!CollectionUtils.isEmpty(statuses)) {
            statuses.removeIf(Objects::isNull);
        }

        if(CollectionUtils.isEmpty(statuses)) {
            statuses = Arrays.asList(StudyStatus.ACTIVE);
        }

        this.must(QueryBuilders.termsQuery("status", statuses.stream().map(Enum::name).collect(Collectors.toList())));
    }

    public void filterByTeachingLanguages(List<String> teachingLanguages) {
        if (!CollectionUtils.isEmpty(teachingLanguages)) {
            BoolQueryBuilder studyElementQuery = QueryBuilders.boolQuery();

            String[] teachingLanguageValuesExcludingUnspecified = teachingLanguages.stream()
                .filter(tl -> !tl.equals(TeachingLanguage.UNSPECIFIED.getValue()))
                .toArray(String[]::new);

            if (teachingLanguages.contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                if (teachingLanguageValuesExcludingUnspecified != null && teachingLanguageValuesExcludingUnspecified.length > 0) {
                    studyElementQuery.should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("teachingLanguage.lowercase")));
                } else {
                    studyElementQuery.must(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("teachingLanguage.lowercase")));
                }
            }

            if (teachingLanguageValuesExcludingUnspecified != null && teachingLanguageValuesExcludingUnspecified.length > 0) {
                if (teachingLanguages.contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                    studyElementQuery.should(QueryBuilders.termsQuery("teachingLanguage.lowercase", teachingLanguageValuesExcludingUnspecified));
                } else {
                    studyElementQuery.must(QueryBuilders.termsQuery("teachingLanguage.lowercase", teachingLanguageValuesExcludingUnspecified));
                }
            }

            this.must(studyElementQuery);
        }
    }

    public void filterByCooperationNetworks(String organisationId, List<NetworkEntity> organisationNetworkIds, List<String> networkIdSearchParams,
                                            boolean includeInactive, boolean includeOwn) {
        this.must(getCooperationNetworksFilter(organisationId, organisationNetworkIds, networkIdSearchParams, includeInactive,
            includeOwn));
    }

    private QueryBuilder getCooperationNetworksFilter(String organisationId, List<NetworkEntity> organisationNetworks,
                                                    List<String> networkIdSearchParams, boolean includeInactive, boolean includeOwn) {
        // own organisation filter
        QueryBuilder ownOrganisationQuery = getOrganizingOrganisationIdsFilter(Collections.singletonList(organisationId));

        // the main query
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        BoolQueryBuilder networkQuery = QueryBuilders.boolQuery();

        // network query. always search only our own networks if there is any
        if(!CollectionUtils.isEmpty(organisationNetworks)) {
            networkQuery.must(getCooperationNetworksFilter(organisationNetworks.stream().map(NetworkEntity::getId).collect(Collectors.toList()), includeInactive));

            // if network search params were given, filter also by those. hence, if only networks outside of organisation
            // own networks were given, no results would be returned
            if (!CollectionUtils.isEmpty(networkIdSearchParams)) {
                networkQuery.must(getCooperationNetworksFilter(networkIdSearchParams, includeInactive));
            }

            // also filter out unpublished networks
            NestedQueryBuilder unallowedCooperatoinNetworksFilter = getUnallowedCooperationNetworksFilter(organisationNetworks);
            if(unallowedCooperatoinNetworksFilter != null) {
                networkQuery.must(unallowedCooperatoinNetworksFilter);
            }
        }

        if(networkQuery.hasClauses()) {
            if (includeOwn && includeInactive) {
                query.must(QueryBuilders.boolQuery()
                    .should(networkQuery)
                    .should(ownOrganisationQuery));
            } else {
                query.must(networkQuery);
            }
        } else {
            query.must(ownOrganisationQuery);
        }

        if(!includeOwn) {
            query.mustNot(ownOrganisationQuery);
        }

        return query;
    }

    private QueryBuilder getOrganizingOrganisationIdsFilter(List<String> organizingOrganisationIds) {
        return QueryBuilders.termsQuery("organizingOrganisationId", organizingOrganisationIds);
    }

    private NestedQueryBuilder getCooperationNetworksFilter(List<String> organisationNetworkIds, boolean includeInactive) {
        return QueryBuilders.nestedQuery("cooperationNetworks",
            getNetworksValidFilter("cooperationNetworks", organisationNetworkIds, includeInactive), ScoreMode.None);
    }

    private NestedQueryBuilder getUnallowedCooperationNetworksFilter(List<NetworkEntity> organisationNetworks) {
        BoolQueryBuilder query = getUnallowedNetworksFilter("cooperationNetworks", organisationNetworks);
        if(query == null) {
            return null;
        }

        return QueryBuilders.nestedQuery("cooperationNetworks", query, ScoreMode.None);
    }
}
