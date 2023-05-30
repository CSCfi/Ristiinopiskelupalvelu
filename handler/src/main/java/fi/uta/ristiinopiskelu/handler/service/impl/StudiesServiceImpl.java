package fi.uta.ristiinopiskelu.handler.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.degree.DegreeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchRealisationQueries;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchSortField;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.StudiesService;
import fi.uta.ristiinopiskelu.handler.service.impl.processor.StudiesSearchPostProcessor;
import fi.uta.ristiinopiskelu.persistence.querybuilder.CourseUnitRealisationQueryBuilder;
import fi.uta.ristiinopiskelu.persistence.querybuilder.StudiesQueryBuilder;
import fi.uta.ristiinopiskelu.persistence.repository.StudiesRepository;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudiesServiceImpl implements StudiesService, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(StudiesServiceImpl.class);

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudiesRepository studiesRepository;

    @Autowired
    private NetworkService networkService;
    
    private StudiesSearchPostProcessor studiesSearchPostProcessor;

    @Override
    public List<StudyElementEntity> findAllStudiesByParentReferences(String referenceIdentifier, String referenceOrganizer) throws FindFailedException {
        SearchResponse response = studiesRepository.findAllStudiesByParentReferences(referenceIdentifier, referenceOrganizer);

        SearchHits hits = response.getHits();

        // check for failures
        if(response.getFailedShards() > 0) {
            for(ShardSearchFailure failure : response.getShardFailures()) {
                throw new IllegalStateException("Error while searching studies by parent references", failure.getCause());
            }
        }

        if(hits == null || (hits.getHits() == null || (hits.getTotalHits() != null && hits.getTotalHits().value == 0))) {
            return Collections.emptyList();
        }

        return Arrays.stream(hits.getHits())
            .map(hit -> objectMapper.convertValue(hit.getSourceAsMap(), StudyElementEntity.class))
            .collect(Collectors.toList());
    }

    @Override
    public StudiesSearchResults search(String organisationId, StudiesSearchParameters searchParams) throws FindFailedException {
        List<NetworkEntity> organisationNetworks;

        if(!searchParams.isIncludeInactive()) {
            organisationNetworks = networkService.findAllValidNetworksWhereOrganisationIsValid(organisationId, Pageable.unpaged());
        } else {
            organisationNetworks = networkService.findAllNetworksByOrganisationId(organisationId, Pageable.unpaged());
        }

        // get the actual networks used in the search
        List<NetworkEntity> actualNetworksUsedInSearch;
        if(!CollectionUtils.isEmpty(searchParams.getNetworkIdentifiers())) {
            // make sure all networks given in params are within our own networks
            actualNetworksUsedInSearch =  organisationNetworks.stream()
                .filter(org -> searchParams.getNetworkIdentifiers().contains(org.getId()))
                .collect(Collectors.toList());
        } else {
            actualNetworksUsedInSearch = organisationNetworks;
        }

        // take the entities too, we need them later
        searchParams.setActualNetworksUsedInSearch(actualNetworksUsedInSearch);

        // get a count of the organisations in the networks used in the search (used in aggregates later)
        if(!CollectionUtils.isEmpty(searchParams.getActualNetworkIdsUsedInSearch())) {
            searchParams.setOrganisationAmount(networkService.findOrganisationIdsFromNetworks(searchParams.getActualNetworkIdsUsedInSearch()).size());
        } else {
            searchParams.setOrganisationAmount(1);
        }

        StudiesQueryBuilder studiesQueryWithLanguages = new StudiesQueryBuilder();
        StudiesQueryBuilder studiesQueryWithoutLanguages = new StudiesQueryBuilder();

        // Find by name
        if(StringUtils.hasText(searchParams.getQuery())) {
            studiesQueryWithLanguages.filterByName(searchParams.getQuery(), searchParams.getLanguage() != null ? searchParams.getLanguage() : Language.FI);
            studiesQueryWithoutLanguages.filterByName(searchParams.getQuery(), searchParams.getLanguage() != null ? searchParams.getLanguage() : Language.FI);
        }

        // Filter to return only study elements for requesting organisations networks
        studiesQueryWithLanguages.filterByCooperationNetworks(organisationId, organisationNetworks,
            searchParams.getNetworkIdentifiers(), searchParams.isIncludeInactive(), searchParams.isIncludeOwn());

        if(!CollectionUtils.isEmpty(searchParams.getOrganizingOrganisationIdentifiers())) {
            studiesQueryWithLanguages.filterByOrganizingOrganisationIds(searchParams.getOrganizingOrganisationIdentifiers());
        }

        studiesQueryWithoutLanguages.filterByCooperationNetworks(organisationId, organisationNetworks,
            searchParams.getNetworkIdentifiers(), searchParams.isIncludeInactive(), searchParams.isIncludeOwn());

        if(!CollectionUtils.isEmpty(searchParams.getOrganizingOrganisationIdentifiers())) {
            studiesQueryWithoutLanguages.filterByOrganizingOrganisationIds(searchParams.getOrganizingOrganisationIdentifiers());
        }

        // filter out inactive study elements
        if(!searchParams.isIncludeInactive()) {
            studiesQueryWithLanguages.filterOnlyValid();
            studiesQueryWithoutLanguages.filterOnlyValid();
        }

        if(!CollectionUtils.isEmpty(searchParams.getStatuses())) {
            studiesQueryWithLanguages.filterByStatuses(searchParams.getStatuses());
            studiesQueryWithoutLanguages.filterByStatuses(searchParams.getStatuses());
        }

        // filter by teaching languages
        studiesQueryWithLanguages.filterByTeachingLanguages(searchParams.getTeachingLanguages());

        // Elasticsearch 6.x does not support _index query by alias so we have to get index name by course unit alias first
        // this should be fixed in 7.x https://github.com/elastic/elasticsearch/pull/46640
        String courseUnitIndexName;
        try {
            courseUnitIndexName = studiesRepository.findIndexNameByAlias(((Document) CourseUnitEntity.class.getAnnotations()[0]).indexName());
        } catch (Exception e) {
            logger.error("Unable to find alias name for index: " + ((Document) CourseUnitEntity.class.getAnnotations()[0]).indexName());
            throw new FindFailedException(StudyElementEntity.class, e);
        }

        StudiesSearchRealisationQueries realisationQueriesWithTeachingLanguages = getRealisationQueries(searchParams, true,
            "realisationsWithTeachingLanguagesQuery",
            "assessmentItemRealisationsWithTeachingLanguagesQuery", courseUnitIndexName);
        StudiesSearchRealisationQueries realisationQueriesWithoutTeachingLanguages = getRealisationQueries(searchParams, false,
            "realisationsWithoutTeachingLanguagesQuery",
            "assessmentItemRealisationsWithoutTeachingLanguagesQuery", courseUnitIndexName);

        BoolQueryBuilder noActiveRealisationsExistQuery = getNoActiveRealisationsExistQuery(searchParams, courseUnitIndexName);

        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();

        // finally add realisation queries
        if(!CollectionUtils.isEmpty(searchParams.getTeachingLanguages()) && !CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
            if(searchParams.getTeachingLanguages().contains(TeachingLanguage.UNSPECIFIED.getValue()) &&
                searchParams.getRealisationTeachingLanguages().contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                mainQuery.must(studiesQueryWithLanguages);
                mainQuery.must(QueryBuilders.boolQuery()
                    .should(realisationQueriesWithTeachingLanguages.getFinalQuery())
                    .should(noActiveRealisationsExistQuery));
            } else {
                mainQuery.should(QueryBuilders.boolQuery()
                    .must(studiesQueryWithLanguages)
                    .must(realisationQueriesWithoutTeachingLanguages.getFinalQuery()));

                mainQuery.should(QueryBuilders.boolQuery()
                    .must(studiesQueryWithoutLanguages)
                    .must(realisationQueriesWithTeachingLanguages.getFinalQuery()));

                mainQuery.should(QueryBuilders.boolQuery()
                    .must(studiesQueryWithLanguages)
                    .must(noActiveRealisationsExistQuery));
            }
        } else if(!CollectionUtils.isEmpty(searchParams.getTeachingLanguages()) && CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
            if(realisationQueriesWithoutTeachingLanguages != null) {
                mainQuery.must(studiesQueryWithLanguages);
                mainQuery.must(QueryBuilders.boolQuery()
                    .should(realisationQueriesWithoutTeachingLanguages.getFinalQuery())
                    .should(noActiveRealisationsExistQuery));
            } else {
                mainQuery.must(studiesQueryWithLanguages);
            }
        } else if(CollectionUtils.isEmpty(searchParams.getTeachingLanguages()) && !CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
            mainQuery.must(studiesQueryWithoutLanguages);
            mainQuery.must(QueryBuilders.boolQuery()
                .should(realisationQueriesWithTeachingLanguages.getFinalQuery())
                .should(noActiveRealisationsExistQuery));
        } else {
            if(realisationQueriesWithoutTeachingLanguages != null) {
                mainQuery.must(studiesQueryWithoutLanguages);
                mainQuery.must(QueryBuilders.boolQuery()
                    .should(realisationQueriesWithoutTeachingLanguages.getFinalQuery())
                    .should(noActiveRealisationsExistQuery));
            } else {
                mainQuery.must(studiesQueryWithoutLanguages);
            }
        }

        List<String> indices = Arrays.asList("opintojaksot", "opintokokonaisuudet", "tutkinnot");

        if(searchParams.getType() != null) {
            switch (searchParams.getType()) {
                case COURSE_UNIT:
                    indices = Arrays.asList("opintojaksot");
                    break;
                case STUDY_MODULE:
                    indices = Arrays.asList("opintokokonaisuudet");
                    break;
                case DEGREE:
                    indices = Arrays.asList("tutkinnot");
                    break;
                case ALL:
                default:
                    break;
            }
        }

        Sort sort = Sort.unsorted();
        if(searchParams.getSortBy() != null && searchParams.getSortBy() != StudiesSearchSortField.NONE) {
            String fieldName = searchParams.getSortBy().getFieldName();
            Language language = searchParams.getLanguage() != null ? searchParams.getLanguage() : Language.FI;
            Sort.Direction direction = searchParams.getSortDirection() != null ? searchParams.getSortDirection() : Sort.Direction.ASC;

            switch(searchParams.getSortBy()) {
                case NAME:
                    fieldName = String.format("%s.values.%s.sort", fieldName, language.getValue());
                    break;
                case IDENTIFIER_CODE:
                    fieldName = String.format("%s.sort", fieldName);
                    break;
                default:
                    break;
            }

            sort = Sort.by(direction, fieldName);
        }

        try {
            SearchResponse response = studiesRepository.findAllStudies(mainQuery,
                realisationQueriesWithTeachingLanguages,
                realisationQueriesWithoutTeachingLanguages,
                indices, searchParams.getPageRequest(sort), searchParams);

            SearchHits hits = response.getHits();
            Aggregations aggs = response.getAggregations();

            // check for failures
            if(response.getFailedShards() > 0) {
                for(ShardSearchFailure failure : response.getShardFailures()) {
                    throw new IllegalStateException("Error while searching studies by parent references", failure.getCause());
                }
            }

            if(hits == null || (hits.getHits() == null || (hits.getTotalHits() != null && hits.getTotalHits().value == 0))) {
                return new StudiesSearchResults();
            }

            return studiesSearchPostProcessor.postProcess(hits, aggs, searchParams);
        } catch (Exception e) {
            throw new FindFailedException(StudyElementEntity.class, e);
        }
    }

    @Override
    public AbstractStudyElementReadDTO toRestDTO(StudyElementEntity entity) {
        if(entity.getType() == CompositeIdentifiedEntityType.COURSE_UNIT) {
            return modelMapper.map(entity, CourseUnitReadDTO.class);
        } else if (entity.getType() == CompositeIdentifiedEntityType.STUDY_MODULE) {
            return modelMapper.map(entity, StudyModuleReadDTO.class);
        } else if (entity.getType() == CompositeIdentifiedEntityType.DEGREE) {
            return modelMapper.map(entity, DegreeReadDTO.class);
        } else {
            throw new IllegalStateException("Unknown StudyElement type:" + entity.getClass());
        }
    }

    private BoolQueryBuilder getNoActiveRealisationsExistQuery(StudiesSearchParameters searchParams, String courseUnitIndexName) {
        BoolQueryBuilder noActiveRealisationsExistQuery;

        if(searchParams.isIncludeCourseUnitsWithoutActiveRealisations()) {
            CourseUnitRealisationQueryBuilder realisationQueryBuilder = new CourseUnitRealisationQueryBuilder();

            noActiveRealisationsExistQuery = QueryBuilders.boolQuery()
                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("_index", courseUnitIndexName)))
                .should(QueryBuilders.boolQuery()
                    .mustNot(realisationQueryBuilder.generateRealisationQuery(
                        QueryBuilders.termQuery("realisations.status", StudyStatus.ACTIVE.name()), null))
                    .mustNot(realisationQueryBuilder.generateAssessmentItemRealisationQuery(
                        QueryBuilders.termQuery("completionOptions.assessmentItems.realisations.status", StudyStatus.ACTIVE.name()), null)));
        } else {
            noActiveRealisationsExistQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("_index", courseUnitIndexName));
        }
        return noActiveRealisationsExistQuery;
    }

    private StudiesSearchRealisationQueries getRealisationQueries(StudiesSearchParameters searchParams, boolean useLanguageSearch,
                                                   String realisationsInnerHitsName, String assessmentitemRealisationsInnerHitsName,
                                                   String courseUnitIndexName) {

        final boolean enrollmentPeriodParametersGiven = checkAndFixEnrollmentSearchParameters(searchParams);

        if(searchParams.getRealisationStartDate() != null || searchParams.getRealisationEndDate() != null
            || enrollmentPeriodParametersGiven || StringUtils.hasText(searchParams.getRealisationQuery()) || searchParams.isOnlyEnrollable()
            || !CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages()) || !CollectionUtils.isEmpty(searchParams.getRealisationStatuses())) {

            BoolQueryBuilder mainRealisationQuery = QueryBuilders.boolQuery();
            BoolQueryBuilder mainAssessmentItemRealisationQuery = QueryBuilders.boolQuery();

            // these need to be created separately since they are modified later
            BoolQueryBuilder aggregationRealisationQuery = QueryBuilders.boolQuery();
            BoolQueryBuilder aggregationAssessmentItemRealisationQuery = QueryBuilders.boolQuery();

            CourseUnitRealisationQueryBuilder courseUnitRealisationQueryBuilder = new CourseUnitRealisationQueryBuilder();

            courseUnitRealisationQueryBuilder.filterByRealisationCooperationNetwork(mainRealisationQuery,
                mainAssessmentItemRealisationQuery, searchParams.getActualNetworkIdsUsedInSearch(), searchParams.isOnlyEnrollable());

            courseUnitRealisationQueryBuilder.filterByRealisationCooperationNetwork(aggregationRealisationQuery,
                aggregationAssessmentItemRealisationQuery, searchParams.getActualNetworkIdsUsedInSearch(), searchParams.isOnlyEnrollable());

            if (searchParams.getRealisationStartDate() != null || searchParams.getRealisationEndDate() != null) {
                courseUnitRealisationQueryBuilder.filterByRealisationStartAndEndDate(mainRealisationQuery, mainAssessmentItemRealisationQuery,
                    searchParams.getRealisationStartDate(), searchParams.getRealisationEndDate());

                courseUnitRealisationQueryBuilder.filterByRealisationStartAndEndDate(aggregationRealisationQuery,
                    aggregationAssessmentItemRealisationQuery, searchParams.getRealisationStartDate(), searchParams.getRealisationEndDate());
            }

            if (enrollmentPeriodParametersGiven) {
                courseUnitRealisationQueryBuilder.filterByRealisationEnrollmentStartAndEndDateTime(mainRealisationQuery,
                    mainAssessmentItemRealisationQuery,
                    searchParams.getRealisationEnrollmentStartDateTimeFrom(), searchParams.getRealisationEnrollmentStartDateTimeTo(),
                    searchParams.getRealisationEnrollmentEndDateTimeFrom(), searchParams.getRealisationEnrollmentEndDateTimeTo());

                courseUnitRealisationQueryBuilder.filterByRealisationEnrollmentStartAndEndDateTime(aggregationRealisationQuery,
                    aggregationAssessmentItemRealisationQuery,
                    searchParams.getRealisationEnrollmentStartDateTimeFrom(), searchParams.getRealisationEnrollmentStartDateTimeTo(),
                    searchParams.getRealisationEnrollmentEndDateTimeFrom(), searchParams.getRealisationEnrollmentEndDateTimeTo());
            }

            if(StringUtils.hasText(searchParams.getRealisationQuery())) {
                courseUnitRealisationQueryBuilder.filterByRealisationNameOrIdentifierCode(mainRealisationQuery,
                    mainAssessmentItemRealisationQuery, searchParams.getRealisationQuery(),
                    searchParams.getLanguage() != null ? searchParams.getLanguage() : Language.FI);

                courseUnitRealisationQueryBuilder.filterByRealisationNameOrIdentifierCode(aggregationRealisationQuery,
                    aggregationAssessmentItemRealisationQuery, searchParams.getRealisationQuery(),
                    searchParams.getLanguage() != null ? searchParams.getLanguage() : Language.FI);
            }

            if(useLanguageSearch) {
                if (!CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
                    courseUnitRealisationQueryBuilder.filterByTeachingLanguages(mainRealisationQuery, mainAssessmentItemRealisationQuery,
                        searchParams.getRealisationTeachingLanguages());

                    courseUnitRealisationQueryBuilder.filterByTeachingLanguages(aggregationRealisationQuery, aggregationAssessmentItemRealisationQuery,
                        searchParams.getRealisationTeachingLanguages());
                }
            }

            if(!CollectionUtils.isEmpty(searchParams.getRealisationStatuses())) {
                courseUnitRealisationQueryBuilder.filterByStatuses(mainRealisationQuery, mainAssessmentItemRealisationQuery,
                    searchParams.getRealisationStatuses());

                courseUnitRealisationQueryBuilder.filterByStatuses(aggregationRealisationQuery, aggregationAssessmentItemRealisationQuery,
                    searchParams.getRealisationStatuses());
            }

            BoolQueryBuilder finalQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("_index", courseUnitIndexName))
                    .must(QueryBuilders.boolQuery()
                        .should(courseUnitRealisationQueryBuilder.generateAssessmentItemRealisationQuery(
                            mainAssessmentItemRealisationQuery, assessmentitemRealisationsInnerHitsName))
                        .should(courseUnitRealisationQueryBuilder.generateRealisationQuery(
                            mainRealisationQuery, realisationsInnerHitsName))));

            return new StudiesSearchRealisationQueries(finalQuery, aggregationRealisationQuery,
                aggregationAssessmentItemRealisationQuery);
        }

        return null;
    }

    private boolean checkAndFixEnrollmentSearchParameters(StudiesSearchParameters parameters) {
        boolean deprecatedGiven = parameters.getRealisationEnrollmentStartDateTime() != null || parameters.getRealisationEnrollmentEndDateTime() != null;
        boolean fromToGiven = parameters.getRealisationEnrollmentStartDateTimeFrom() != null || parameters.getRealisationEnrollmentStartDateTimeTo() != null
            || parameters.getRealisationEnrollmentEndDateTimeFrom() != null || parameters.getRealisationEnrollmentEndDateTimeTo() != null;

        if (deprecatedGiven || fromToGiven) {
            if (fromToGiven) {
                if (deprecatedGiven) {
                    logger.warn("Ignoring deprecated realisation enrollment period parameters as new ones are also given.");
                    parameters.setRealisationEnrollmentStartDateTime(null);
                    parameters.setRealisationEnrollmentEndDateTime(null);
                }
            } else {
                logger.debug("Converting deprecated realisation enrollment period parameters.");
                parameters.setRealisationEnrollmentStartDateTimeTo(parameters.getRealisationEnrollmentStartDateTime());
                parameters.setRealisationEnrollmentEndDateTimeFrom(parameters.getRealisationEnrollmentEndDateTime());
                parameters.setRealisationEnrollmentStartDateTime(null);
                parameters.setRealisationEnrollmentEndDateTime(null);
            }
            return true;
        }

        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.studiesSearchPostProcessor = new StudiesSearchPostProcessor(this, objectMapper);
    }
}
