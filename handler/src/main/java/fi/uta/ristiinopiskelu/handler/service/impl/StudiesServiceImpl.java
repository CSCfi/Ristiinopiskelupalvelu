package fi.uta.ristiinopiskelu.handler.service.impl;

import co.elastic.clients.elasticsearch._types.ShardFailure;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.degree.DegreeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.InternalStudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchRealisationQueries;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudiesServiceImpl implements StudiesService, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(StudiesServiceImpl.class);

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private StudiesRepository studiesRepository;

    @Autowired
    private NetworkService networkService;
    
    private StudiesSearchPostProcessor studiesSearchPostProcessor;

    @Override
    public List<StudyElementEntity> findAllStudiesByParentReferences(String referenceIdentifier, String referenceOrganizer) throws FindFailedException {
        SearchResponse<StudyElementEntity> response = studiesRepository.findAllStudiesByParentReferences(referenceIdentifier, referenceOrganizer);

        // check for failures
        if(response.shards().failed().intValue() > 0) {
            for(ShardFailure failure : response.shards().failures()) {
                throw new IllegalStateException("Error while searching studies by parent references: %s".formatted(failure.status()));
            }
        }

        if(response.hits() == null || response.hits().total() == null || response.hits().total().value() == 0) {
            return Collections.emptyList();
        }

        return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
    }

    @Override
    public InternalStudiesSearchResults search(String organisationId, StudiesSearchParameters searchParams) throws FindFailedException {
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

        StudiesQueryBuilder studiesQueryWithLanguagesBuilder = new StudiesQueryBuilder();
        StudiesQueryBuilder studiesQueryWithoutLanguagesBuilder = new StudiesQueryBuilder();

        // Find by name
        if(StringUtils.hasText(searchParams.getQuery())) {
            studiesQueryWithLanguagesBuilder.filterByName(searchParams.getQuery(), searchParams.getLanguage() != null ? searchParams.getLanguage() : Language.FI);
            studiesQueryWithoutLanguagesBuilder.filterByName(searchParams.getQuery(), searchParams.getLanguage() != null ? searchParams.getLanguage() : Language.FI);
        }

        // Filter to return only study elements for requesting organisations networks
        studiesQueryWithLanguagesBuilder.filterByCooperationNetworks(organisationId, organisationNetworks,
            searchParams.getNetworkIdentifiers(), searchParams.isIncludeInactive(), searchParams.isIncludeOwn());

        if(!CollectionUtils.isEmpty(searchParams.getOrganizingOrganisationIdentifiers())) {
            studiesQueryWithLanguagesBuilder.filterByOrganizingOrganisationIds(searchParams.getOrganizingOrganisationIdentifiers());
        }

        studiesQueryWithoutLanguagesBuilder.filterByCooperationNetworks(organisationId, organisationNetworks,
            searchParams.getNetworkIdentifiers(), searchParams.isIncludeInactive(), searchParams.isIncludeOwn());

        if(!CollectionUtils.isEmpty(searchParams.getOrganizingOrganisationIdentifiers())) {
            studiesQueryWithoutLanguagesBuilder.filterByOrganizingOrganisationIds(searchParams.getOrganizingOrganisationIdentifiers());
        }

        // filter out inactive study elements
        if(!searchParams.isIncludeInactive()) {
            studiesQueryWithLanguagesBuilder.filterOnlyValid();
            studiesQueryWithoutLanguagesBuilder.filterOnlyValid();
        }

        if(!CollectionUtils.isEmpty(searchParams.getStatuses())) {
            studiesQueryWithLanguagesBuilder.filterByStatuses(searchParams.getStatuses());
            studiesQueryWithoutLanguagesBuilder.filterByStatuses(searchParams.getStatuses());
        }

        if(!CollectionUtils.isEmpty(searchParams.getMinEduGuidanceAreas())) {
            studiesQueryWithLanguagesBuilder.filterByMinEduGuidanceAreas(searchParams.getMinEduGuidanceAreas());
            studiesQueryWithoutLanguagesBuilder.filterByMinEduGuidanceAreas(searchParams.getMinEduGuidanceAreas());
        }

        // filter by teaching languages
        studiesQueryWithLanguagesBuilder.filterByTeachingLanguages(searchParams.getTeachingLanguages());

        String courseUnitIndexName = ((Document) CourseUnitEntity.class.getAnnotations()[0]).indexName();

        StudiesSearchRealisationQueries realisationQueriesWithTeachingLanguages = getRealisationQueries(searchParams, true,
            "realisationsWithTeachingLanguagesQuery",
            "assessmentItemRealisationsWithTeachingLanguagesQuery", courseUnitIndexName);
        StudiesSearchRealisationQueries realisationQueriesWithoutTeachingLanguages = getRealisationQueries(searchParams, false,
            "realisationsWithoutTeachingLanguagesQuery",
            "assessmentItemRealisationsWithoutTeachingLanguagesQuery", courseUnitIndexName);

        Query noActiveRealisationsExistQuery = getNoActiveRealisationsExistQuery(searchParams, courseUnitIndexName);
        Query studiesQueryWithLanguages = studiesQueryWithLanguagesBuilder.build()._toQuery();
        Query studiesQueryWithoutLanguages = studiesQueryWithoutLanguagesBuilder.build()._toQuery();

        BoolQuery.Builder mainQuery = new BoolQuery.Builder();

        // finally add realisation queries
        if(!CollectionUtils.isEmpty(searchParams.getTeachingLanguages()) && !CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
            if(searchParams.getTeachingLanguages().contains(TeachingLanguage.UNSPECIFIED.getValue()) &&
                searchParams.getRealisationTeachingLanguages().contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                mainQuery.must(studiesQueryWithLanguages);
                mainQuery.must(q -> q.bool(bq -> bq
                    .should(realisationQueriesWithTeachingLanguages.getFinalQuery())
                    .should(noActiveRealisationsExistQuery)));
            } else {
                mainQuery.should(q -> q.bool(bq -> bq
                    .must(studiesQueryWithLanguages)
                    .must(realisationQueriesWithoutTeachingLanguages.getFinalQuery())));

                mainQuery.should(q -> q.bool(bq -> bq
                    .must(studiesQueryWithoutLanguages)
                    .must(realisationQueriesWithTeachingLanguages.getFinalQuery())));

                mainQuery.should(q -> q.bool(bq -> bq
                    .must(studiesQueryWithLanguages)
                    .must(noActiveRealisationsExistQuery)));
            }
        } else if(!CollectionUtils.isEmpty(searchParams.getTeachingLanguages()) && CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
            if(realisationQueriesWithoutTeachingLanguages != null) {
                mainQuery.must(studiesQueryWithLanguages);
                mainQuery.must(q -> q.bool(bq -> bq
                    .should(realisationQueriesWithoutTeachingLanguages.getFinalQuery())
                    .should(noActiveRealisationsExistQuery)));
            } else {
                mainQuery.must(studiesQueryWithLanguages);
            }
        } else if(CollectionUtils.isEmpty(searchParams.getTeachingLanguages()) && !CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
            mainQuery.must(studiesQueryWithoutLanguages);
            mainQuery.must(q -> q.bool(bq -> bq
                .should(realisationQueriesWithTeachingLanguages.getFinalQuery())
                .should(noActiveRealisationsExistQuery)));
        } else {
            if(realisationQueriesWithoutTeachingLanguages != null) {
                mainQuery.must(studiesQueryWithoutLanguages);
                mainQuery.must(q -> q.bool(bq -> bq
                    .should(realisationQueriesWithoutTeachingLanguages.getFinalQuery())
                    .should(noActiveRealisationsExistQuery)));
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
            SearchResponse<StudyElementEntity> response = studiesRepository.findAllStudies(mainQuery,
                realisationQueriesWithTeachingLanguages,
                realisationQueriesWithoutTeachingLanguages,
                indices, searchParams.getPageRequest(sort), searchParams);

            HitsMetadata<StudyElementEntity> hits = response.hits();
            Map<String, Aggregate> aggs = response.aggregations();

            // check for failures
            if(response.shards().failed().intValue() > 0) {
                for(ShardFailure failure : response.shards().failures()) {
                    throw new IllegalStateException("Error while searching studies by parent references: " + failure.reason().reason());
                }
            }

            if(hits.total().value() == 0) {
                return new InternalStudiesSearchResults();
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

    private Query getNoActiveRealisationsExistQuery(StudiesSearchParameters searchParams, String courseUnitIndexName) {
        BoolQuery.Builder noActiveRealisationsExistQuery;

        if(searchParams.isIncludeCourseUnitsWithoutActiveRealisations()) {
            CourseUnitRealisationQueryBuilder realisationQueryBuilder = new CourseUnitRealisationQueryBuilder();

            noActiveRealisationsExistQuery = new BoolQuery.Builder()
                .should(q -> q
                    .bool(bq -> bq
                        .mustNot(q2 -> q2
                            .term(tq -> tq.field("_index")
                                .value(courseUnitIndexName)))))
                .should(q -> q.bool(bq -> bq
                    .mustNot(realisationQueryBuilder.generateRealisationQuery(
                        new TermQuery.Builder()
                            .field("realisations.status")
                            .value(StudyStatus.ACTIVE.name()).build()._toQuery(), null))
                    .mustNot(realisationQueryBuilder.generateAssessmentItemRealisationQuery(
                        new TermQuery.Builder()
                            .field("completionOptions.assessmentItems.realisations.status")
                            .value(StudyStatus.ACTIVE.name()).build()._toQuery(), null))));
        } else {
            noActiveRealisationsExistQuery = new BoolQuery.Builder()
                .mustNot(q -> q
                    .term(tq -> tq
                        .field("_index")
                        .value(courseUnitIndexName)));
        }

        return noActiveRealisationsExistQuery.build()._toQuery();
    }

    private StudiesSearchRealisationQueries getRealisationQueries(StudiesSearchParameters searchParams, boolean useLanguageSearch,
                                                   String realisationsInnerHitsName, String assessmentitemRealisationsInnerHitsName,
                                                   String courseUnitIndexName) {

        final boolean enrollmentPeriodParametersGiven = checkAndFixEnrollmentSearchParameters(searchParams);

        if(searchParams.getRealisationStartDate() != null || searchParams.getRealisationEndDate() != null
            || enrollmentPeriodParametersGiven || StringUtils.hasText(searchParams.getRealisationQuery()) || searchParams.isOnlyEnrollable()
            || !CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages()) || !CollectionUtils.isEmpty(searchParams.getRealisationStatuses())) {

            BoolQuery.Builder mainRealisationQuery = new BoolQuery.Builder();
            BoolQuery.Builder mainAssessmentItemRealisationQuery = new BoolQuery.Builder();

            // these need to be created separately since they are modified later
            BoolQuery.Builder aggregationRealisationQuery = new BoolQuery.Builder();
            BoolQuery.Builder aggregationAssessmentItemRealisationQuery = new BoolQuery.Builder();

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

            if(!CollectionUtils.isEmpty(searchParams.getRealisationMinEduGuidanceAreas())) {
                courseUnitRealisationQueryBuilder.filterByMinEduGuidanceAreas(mainRealisationQuery, mainAssessmentItemRealisationQuery,
                    searchParams.getRealisationMinEduGuidanceAreas());

                courseUnitRealisationQueryBuilder.filterByMinEduGuidanceAreas(aggregationRealisationQuery, aggregationAssessmentItemRealisationQuery,
                    searchParams.getRealisationMinEduGuidanceAreas());
            }

            Query finalQuery = new Query.Builder().bool(q -> q
                .must(tq -> tq
                    .term(tq2 -> tq2
                        .field("_index")
                        .value(courseUnitIndexName)))
                .must(bq4 -> bq4.bool(bq5 -> bq5
                    .should(courseUnitRealisationQueryBuilder.generateAssessmentItemRealisationQuery(
                        mainAssessmentItemRealisationQuery.build()._toQuery(), assessmentitemRealisationsInnerHitsName))
                    .should(courseUnitRealisationQueryBuilder.generateRealisationQuery(
                        mainRealisationQuery.build()._toQuery(), realisationsInnerHitsName)))))
                .build();

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
        this.studiesSearchPostProcessor = new StudiesSearchPostProcessor(this);
    }
}
