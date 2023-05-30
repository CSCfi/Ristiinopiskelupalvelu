package fi.uta.ristiinopiskelu.handler.service.impl.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleBucketDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleMultiBucketAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.service.StudiesService;
import fi.uta.ristiinopiskelu.handler.utils.AggregationUtils;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.SingleBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StudiesSearchPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StudiesSearchPostProcessor.class);

    private StudiesService studiesService;
    private ObjectMapper objectMapper;

    public StudiesSearchPostProcessor(StudiesService studiesService, ObjectMapper objectMapper) {
        this.studiesService = studiesService;
        this.objectMapper = objectMapper;
    }

    public StudiesSearchResults postProcess(SearchHits searchHits, Aggregations aggregations, StudiesSearchParameters searchParams) {
        List<StudyElementEntity> results = Arrays.stream(searchHits.getHits())
            .map(h -> {
                StudyElementEntity entity = objectMapper.convertValue(h.getSourceAsMap(), StudyElementEntity.class);

                Map<String, SearchHits> innerHits = h.getInnerHits();

                List<CourseUnitRealisationEntity> assessmentItemRealisationsWithTeachingLanguagesQueryHits = new ArrayList<>();
                List<CourseUnitRealisationEntity> assessmentItemRealisationsWithoutTeachingLanguagesQueryHits = new ArrayList<>();
                List<CourseUnitRealisationEntity> realisationsWithTeachingLanguagesQueryHits = new ArrayList<>();
                List<CourseUnitRealisationEntity> realisationsWithoutTeachingLanguagesQueryHits = new ArrayList<>();

                if (!CollectionUtils.isEmpty(innerHits)) {
                    if(innerHits.containsKey("realisationsWithTeachingLanguagesQuery")) {
                        realisationsWithTeachingLanguagesQueryHits.addAll(
                            mapCourseUnitRealisations(innerHits.get("realisationsWithTeachingLanguagesQuery")));
                    }

                    if(innerHits.containsKey("realisationsWithoutTeachingLanguagesQuery")) {
                        realisationsWithoutTeachingLanguagesQueryHits.addAll(
                            mapCourseUnitRealisations(innerHits.get("realisationsWithoutTeachingLanguagesQuery")));
                    }

                    if(innerHits.containsKey("assessmentItemRealisationsWithTeachingLanguagesQuery")) {
                        assessmentItemRealisationsWithTeachingLanguagesQueryHits.addAll(
                            mapAssessmentItemRealisations("assessmentItemRealisationsWithTeachingLanguagesQuery",
                                innerHits));
                    }

                    if(innerHits.containsKey("assessmentItemRealisationsWithoutTeachingLanguagesQuery")) {
                        assessmentItemRealisationsWithoutTeachingLanguagesQueryHits.addAll(
                            mapAssessmentItemRealisations("assessmentItemRealisationsWithoutTeachingLanguagesQuery",
                                innerHits));
                    }
                }

                /**
                 * Post-filter realisations for each CourseUnit.
                 *
                 * Background: We have to limit the returned realisations by a few of the realisation search parameters.
                 * ES cannot filter nested sets (not currently, at least) but it does give us the lists of matching nested set elements (inner hits).
                 * Therefore we have to do the nested set filtering manually here by comparing the inner hit lists to the actual nested set content.
                 */
                if(entity.getType() == CompositeIdentifiedEntityType.COURSE_UNIT) {
                    CourseUnitEntity courseUnitEntity = (CourseUnitEntity) entity;

                    if(!CollectionUtils.isEmpty(courseUnitEntity.getRealisations())) {
                        courseUnitEntity.setRealisations(courseUnitEntity.getRealisations().stream()
                            // we always have to limit manually by the current organisation/search network identifiers here.
                            // we don't want to ever return realisations that are outside of the current organisation networks,
                            // regardless of the given search parameters.
                            .filter(hasSameNetwork(searchParams.getActualNetworkIdsUsedInSearch())
                                .and(
                                    // always filter returned realisations by inner hits.
                                    includedIn(
                                        realisationsWithTeachingLanguagesQueryHits,
                                        realisationsWithoutTeachingLanguagesQueryHits,
                                        searchParams)))
                            .collect(Collectors.toList()));
                    }

                    if (!CollectionUtils.isEmpty(courseUnitEntity.getCompletionOptions())) {
                        for (CompletionOptionEntity completionOption : courseUnitEntity.getCompletionOptions()) {
                            if (!CollectionUtils.isEmpty(completionOption.getAssessmentItems())) {
                                for (AssessmentItemEntity assessmentItem : completionOption.getAssessmentItems()) {
                                    if(!CollectionUtils.isEmpty(assessmentItem.getRealisations())) {
                                        assessmentItem.setRealisations(assessmentItem.getRealisations().stream()
                                            // we always have to limit manually by the current organisation/search network identifiers here.
                                            // we don't want to ever return realisations that are outside of the current organisation networks,
                                            // regardless of the given search parameters.
                                            .filter(hasSameNetwork(searchParams.getActualNetworkIdsUsedInSearch())
                                                .and(
                                                    // always filter returned realisations by inner hits.
                                                    includedIn(
                                                        assessmentItemRealisationsWithTeachingLanguagesQueryHits,
                                                        assessmentItemRealisationsWithoutTeachingLanguagesQueryHits,
                                                        searchParams)))
                                            .collect(Collectors.toList()));
                                    }
                                }
                            }
                        }
                    }
                }

                return entity;
            }).collect(Collectors.toList());

        StudiesSearchResults studiesSearchResults = new StudiesSearchResults();
        studiesSearchResults.setResults(results.stream().map(studiesService::toRestDTO).collect(Collectors.toList()));
        studiesSearchResults.setAggregations(convertAggregations(aggregations));
        studiesSearchResults.setTotalHits(searchHits.getTotalHits().value);
        return studiesSearchResults;
    }

    private Predicate<CourseUnitRealisationEntity> includedIn(List<CourseUnitRealisationEntity> realisationsWithTeachingLanguagesQueryHits,
                                                              List<CourseUnitRealisationEntity> realisationsWithoutTeachingLanguagesQueryHits,
                                                              StudiesSearchParameters searchParameters) {
        return courseUnitRealisationEntity -> {
            boolean matches = true;

            if(!CollectionUtils.isEmpty(searchParameters.getTeachingLanguages()) && !CollectionUtils.isEmpty(searchParameters.getRealisationTeachingLanguages())) {
                if(searchParameters.getTeachingLanguages().contains(TeachingLanguage.UNSPECIFIED.getValue()) &&
                    searchParameters.getRealisationTeachingLanguages().contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                    matches = realisationsWithTeachingLanguagesQueryHits.stream()
                        .anyMatch(cure -> cure.getOrganizingOrganisationId().equals(courseUnitRealisationEntity.getOrganizingOrganisationId()) &&
                            cure.getRealisationId().equals(courseUnitRealisationEntity.getRealisationId()));
                }
            } else if(!CollectionUtils.isEmpty(realisationsWithTeachingLanguagesQueryHits)) {
                matches = realisationsWithTeachingLanguagesQueryHits.stream()
                    .anyMatch(cure -> cure.getOrganizingOrganisationId().equals(courseUnitRealisationEntity.getOrganizingOrganisationId()) &&
                        cure.getRealisationId().equals(courseUnitRealisationEntity.getRealisationId()));
            } else if(!CollectionUtils.isEmpty(realisationsWithoutTeachingLanguagesQueryHits)) {
                matches = realisationsWithoutTeachingLanguagesQueryHits.stream()
                    .anyMatch(cure -> cure.getOrganizingOrganisationId().equals(courseUnitRealisationEntity.getOrganizingOrganisationId()) &&
                        cure.getRealisationId().equals(courseUnitRealisationEntity.getRealisationId()));
            }

            return matches;
        };
    }

    private Predicate<CourseUnitRealisationEntity> hasSameNetwork(List<String> networkIdentifiers) {

        return courseUnitRealisationEntity -> {
            if (!CollectionUtils.isEmpty(courseUnitRealisationEntity.getCooperationNetworks())) {
                for (CooperationNetwork cn : courseUnitRealisationEntity.getCooperationNetworks()) {
                    if (!CollectionUtils.isEmpty(networkIdentifiers) && networkIdentifiers.contains(cn.getId())) {
                        return true;
                    }
                }
            }

            return false;
        };
    }

    private Predicate<CourseUnitRealisationEntity> hasStatuses(List<StudyStatus> realisationStatuses) {

        return courseUnitRealisationEntity -> {
            List<StudyStatus> statuses = CollectionUtils.isEmpty(realisationStatuses) ?
                Collections.singletonList(StudyStatus.ACTIVE) : realisationStatuses;
            return statuses.contains(courseUnitRealisationEntity.getStatus());
        };
    }

    private List<CourseUnitRealisationEntity> mapAssessmentItemRealisations(String innerHitName, Map<String, SearchHits> innerHits) {

        if(CollectionUtils.isEmpty(innerHits) || !innerHits.containsKey(innerHitName) ||
            (innerHits.get(innerHitName) == null ||
                (innerHits.get(innerHitName).getTotalHits() != null && innerHits.get(innerHitName).getTotalHits().value == 0))) {
            return Collections.emptyList();
        }

        SearchHits completionOptionHits = innerHits.get(innerHitName);

        List<CourseUnitRealisationEntity> mapped = new ArrayList<>();

        // :P
        if(completionOptionHits != null && (completionOptionHits.getTotalHits() != null && completionOptionHits.getTotalHits().value > 0)) {
            SearchHit[] actualCompletionOptionHits = completionOptionHits.getHits();
            for(SearchHit actualCompletionOptionHit : actualCompletionOptionHits) {
                if(!CollectionUtils.isEmpty(actualCompletionOptionHit.getInnerHits())) {
                    if (actualCompletionOptionHit.getInnerHits().containsKey(innerHitName + "_assessmentItems")) {
                        SearchHits assessmentItemHits = actualCompletionOptionHit.getInnerHits().get(innerHitName + "_assessmentItems");
                        if (assessmentItemHits != null && (assessmentItemHits.getTotalHits() != null && assessmentItemHits.getTotalHits().value > 0)) {
                            SearchHit[] actualAssessmentItemHits = assessmentItemHits.getHits();
                            if (actualAssessmentItemHits != null && actualAssessmentItemHits.length > 0) {
                                for (SearchHit actualAssessmentItemHit : actualAssessmentItemHits) {
                                    if (!CollectionUtils.isEmpty(actualAssessmentItemHit.getInnerHits())) {
                                        if (actualAssessmentItemHit.getInnerHits().containsKey(innerHitName + "_realisations")) {
                                            mapped.addAll(mapCourseUnitRealisations(actualAssessmentItemHit.getInnerHits().get(innerHitName + "_realisations")));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return mapped;
    }

    private List<CourseUnitRealisationEntity> mapCourseUnitRealisations(SearchHits... innerHits) {

        if(innerHits == null || innerHits.length == 0) {
            return Collections.emptyList();
        }

        List<CourseUnitRealisationEntity> mapped = new ArrayList<>();

        for(SearchHits innerHit : innerHits) {

            if(innerHit == null || (innerHit.getTotalHits() != null && innerHit.getTotalHits().value == 0)) {
                continue;
            }

            mapped.addAll(Arrays.stream(innerHit.getHits())
                .map(air -> objectMapper.convertValue(air.getSourceAsMap(), CourseUnitRealisationEntity.class))
                .collect(Collectors.toList()));
        }

        return mapped;
    }

    private List<SimpleAggregationDTO> convertAggregations(Aggregations aggregations) {
        List<SimpleAggregationDTO> convertedAggregations = new ArrayList<>();

        if (aggregations != null) {
            convertedAggregations.add(mapTeachingLanguageAggregation(aggregations, "teachingLanguages"));
            // deprecated since v9.0.0 in favor of "studyElementsByRealisationTeachingLanguages"
            convertedAggregations.add(mapRealisationTeachingLanguageAggregation(aggregations,
                "realisationTeachingLanguages",
                "assessmentItemRealisationTeachingLanguages"));

            SimpleAggregationDTO networksAggregation = mapNetworkAggregation(aggregations, "networks");
            if(networksAggregation != null) {
                convertedAggregations.add(networksAggregation);
            }
            
            convertedAggregations.add(mapOrganisationAggregation(aggregations, "organisations"));
            convertedAggregations.add(mapTypeAggregation(aggregations, "types"));

            convertedAggregations.add(mapRealisationAggregation(aggregations,
                "enrollableRealisations",
                "enrollableAssessmentItemRealisations"));
            convertedAggregations.add(mapRealisationAggregation(aggregations,
                "realisationsEnrollableThisSemester",
                "assessmentItemRealisationsEnrollableThisSemester"));
            convertedAggregations.add(mapRealisationAggregation(aggregations,
                "realisationsEnrollableNextSemester",
                "assessmentItemRealisationsEnrollableNextSemester"));
            convertedAggregations.add(mapRealisationAggregation(aggregations,
                "realisationsEnrollableAfterNextSemester",
                "assessmentItemRealisationsEnrollableAfterNextSemester"));

            convertedAggregations.add(mapRealisationTeachingLanguagesByStudyElementAggregation(
                "studyElementsByRealisationTeachingLanguages", aggregations,
                "realisationTeachingLanguages",
                "assessmentItemRealisationTeachingLanguages"));
        } else {
            logger.warn("No aggregations (parameter is null).");
        }
        return convertedAggregations;
    }

    private SimpleAggregationDTO mapTeachingLanguageAggregation(Aggregations aggregations, String name) {
        Terms languages = aggregations.get(name);

        List<SimpleBucketDTO> languageBuckets = mapBuckets(languages.getBuckets());
        return new SimpleMultiBucketAggregationDTO(name, languageBuckets);
    }

    private SimpleAggregationDTO mapNetworkAggregation(Aggregations aggregations, String name) {
        Nested networks = aggregations.get(name);

        if(networks == null) {
            return null;
        }

        Terms networkIds = networks.getAggregations().get("id");

        List<SimpleBucketDTO> networkBuckets = mapBuckets(networkIds.getBuckets());
        return new SimpleMultiBucketAggregationDTO(name, networkBuckets);
    }

    private SimpleAggregationDTO mapOrganisationAggregation(Aggregations aggregations, String name) {
        Terms organisations = aggregations.get(name);

        List<SimpleBucketDTO> organisationBuckets = mapBuckets(organisations.getBuckets());
        return new SimpleMultiBucketAggregationDTO(name, organisationBuckets);
    }

    private SimpleAggregationDTO mapTypeAggregation(Aggregations aggregations, String name) {
        Terms types = aggregations.get(name);

        List<SimpleBucketDTO> typeBuckets = mapBuckets(types.getBuckets());
        return new SimpleMultiBucketAggregationDTO(name, typeBuckets);
    }

    private SimpleAggregationDTO mapRealisationTeachingLanguagesByStudyElementAggregation(String finalName, Aggregations aggregations, String realisationTeachingLanguageAggregationName,
                                                                           String assessmentItemRealisationTeachingLanguageAggregationName) {
        if(aggregations == null) {
            return null;
        }

        // Map<STUDY_ELEMENT_INTERNAL_UNIQUE_ID, Map<TEACHING_LANGUAGE_KEY, COUNT_PER_UNIQUE_STUDY_ELEMENT>>
        Map<String, Map<String, Long>> languagesCount = new HashMap<>();

        extractRealisationTeachingLanguagesByStudyElementAggregationBuckets(languagesCount, aggregations.get(realisationTeachingLanguageAggregationName));
        extractRealisationTeachingLanguagesByStudyElementAggregationBuckets(languagesCount, aggregations.get(assessmentItemRealisationTeachingLanguageAggregationName));

        // Map<TEACHING_LANGUAGE_KEY, STUDY_ELEMENT_HITS>
        Map<String, Long> studyElementCountByLang = new HashMap<>();

        for(Map.Entry<String, Map<String, Long>> entry : languagesCount.entrySet()) {
            Map<String, Long> realisationLanguageCounts = entry.getValue();
            for(Map.Entry<String, Long> realisationLanguageCount : realisationLanguageCounts.entrySet()) {
                Long existingLanguageCount = studyElementCountByLang.get(realisationLanguageCount.getKey());
                if(realisationLanguageCount.getValue() > 0) {
                    if (existingLanguageCount == null) {
                        studyElementCountByLang.put(realisationLanguageCount.getKey(), 1L);
                    } else {
                        studyElementCountByLang.put(realisationLanguageCount.getKey(), existingLanguageCount + 1);
                    }
                }
            }
        }

        List<SimpleBucketDTO> mappedBuckets = studyElementCountByLang.entrySet().stream()
            .map(entry -> new SimpleBucketDTO(entry.getKey(), entry.getValue())).collect(Collectors.toList());

        SimpleMultiBucketAggregationDTO multiBucketAggregationDTO = new SimpleMultiBucketAggregationDTO();
        multiBucketAggregationDTO.setName(finalName);
        multiBucketAggregationDTO.setBuckets(mappedBuckets);

        return multiBucketAggregationDTO;
    }

    private void extractRealisationTeachingLanguagesByStudyElementAggregationBuckets(Map<String, Map<String, Long>> languagesCount, Aggregation aggregation) {
        if(aggregation == null) {
            return;
        }

        Terms statuses = (Terms) AggregationUtils.findSubAggregation("status", aggregation);
        if(statuses != null && !CollectionUtils.isEmpty(statuses.getBuckets())) {
            for(Terms.Bucket statusBucket : statuses.getBuckets()) {
                Terms teachingLanguages = statusBucket.getAggregations().get("teachingLanguages");
                if (teachingLanguages != null && !CollectionUtils.isEmpty(teachingLanguages.getBuckets())) {
                    for (Terms.Bucket teachingLanguageBucket : teachingLanguages.getBuckets()) {
                        ReverseNested studyElements = teachingLanguageBucket.getAggregations().get("studyElements");
                        if(studyElements != null) {
                            Terms idAggregation = studyElements.getAggregations().get("id");
                            if (idAggregation != null) {
                                for (MultiBucketsAggregation.Bucket idBucket : idAggregation.getBuckets()) {
                                    Map<String, Long> existing = languagesCount.get(idBucket.getKeyAsString());
                                    if (existing == null) {
                                        Map<String, Long> map = new HashMap<>();
                                        map.put(teachingLanguageBucket.getKeyAsString(), idBucket.getDocCount());
                                        languagesCount.put(idBucket.getKeyAsString(), map);
                                    } else {
                                        Long existingCount = existing.get(teachingLanguageBucket.getKeyAsString());
                                        if (existingCount == null) {
                                            existing.put(teachingLanguageBucket.getKeyAsString(), idBucket.getDocCount());
                                        } else {
                                            existing.put(teachingLanguageBucket.getKeyAsString(), idBucket.getDocCount() + existingCount);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Deprecated
    private SimpleAggregationDTO mapRealisationTeachingLanguageAggregation(Aggregations aggregations, String realisationTeachingLanguageAggregationName,
                                                                           String assessmentItemRealisationTeachingLanguageAggregationName) {
        if(aggregations == null) {
            return null;
        }

        Map<String, Map<String, Long>> languagesCount = new HashMap<>();

        Aggregation realisationTeachingLanguagesAggregation = aggregations.get(realisationTeachingLanguageAggregationName);
        if(realisationTeachingLanguagesAggregation != null) {
            if (realisationTeachingLanguagesAggregation instanceof Filter) {
                Filter realisationTeachingLanguagesAggregationFilter = (Filter) realisationTeachingLanguagesAggregation;
                if (realisationTeachingLanguagesAggregationFilter != null && realisationTeachingLanguagesAggregationFilter.getAggregations() != null) {
                    Nested realisations = realisationTeachingLanguagesAggregationFilter.getAggregations().get("realisations");
                    if (realisations != null && realisations.getAggregations() != null) {
                        extractRealisationTeachingLanguageAggregationBuckets(languagesCount, realisations);
                    }
                }
            } else {
                Nested realisations = (Nested) realisationTeachingLanguagesAggregation;
                if (realisations != null && realisations.getAggregations() != null) {
                    extractRealisationTeachingLanguageAggregationBuckets(languagesCount, realisations);
                }
            }
        }

        Aggregation assessmentItemRealisationTeachingLanguagesAggregation = aggregations.get(assessmentItemRealisationTeachingLanguageAggregationName);
        if(assessmentItemRealisationTeachingLanguagesAggregation != null) {
            if(assessmentItemRealisationTeachingLanguagesAggregation instanceof Filter) {
                Filter assessmentItemRealisationTeachingLanguagesAggregationFilter = (Filter) assessmentItemRealisationTeachingLanguagesAggregation;
                if(assessmentItemRealisationTeachingLanguagesAggregationFilter != null && assessmentItemRealisationTeachingLanguagesAggregationFilter.getAggregations() != null) {
                    Nested completionOptions = assessmentItemRealisationTeachingLanguagesAggregationFilter.getAggregations().get("completionOptions");
                    if(completionOptions != null && completionOptions.getAggregations() != null) {
                        Nested assessmentItems = completionOptions.getAggregations().get("assessmentItems");
                        if(assessmentItems != null && assessmentItems.getAggregations() != null) {
                            Nested realisations = assessmentItems.getAggregations().get("realisations");
                            if(realisations != null && realisations.getAggregations() != null) {
                                extractRealisationTeachingLanguageAggregationBuckets(languagesCount, realisations);
                            }
                        }
                    }
                }
            } else {
                Nested completionOptions = (Nested) assessmentItemRealisationTeachingLanguagesAggregation;
                if(completionOptions != null && completionOptions.getAggregations() != null) {
                    Nested assessmentItems = completionOptions.getAggregations().get("assessmentItems");
                    if(assessmentItems != null && assessmentItems.getAggregations() != null) {
                        Nested realisations = assessmentItems.getAggregations().get("realisations");
                        if(realisations != null && realisations.getAggregations() != null) {
                            extractRealisationTeachingLanguageAggregationBuckets(languagesCount, realisations);
                        }
                    }
                }
            }
        }

        List<SimpleBucketDTO> mappedBuckets = new ArrayList<>();
        for(Map.Entry<String, Map<String, Long>> entry : languagesCount.entrySet()) {
            List<SimpleBucketDTO> langs = entry.getValue().entrySet().stream().map(e -> new SimpleBucketDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

            SimpleBucketDTO byStatusBucket = new SimpleBucketDTO(entry.getKey(), langs.size());
            byStatusBucket.setBuckets(langs);
            mappedBuckets.add(byStatusBucket);
        }

        SimpleMultiBucketAggregationDTO multiBucketAggregationDTO = new SimpleMultiBucketAggregationDTO();
        multiBucketAggregationDTO.setName(realisationTeachingLanguageAggregationName);
        multiBucketAggregationDTO.setBuckets(mappedBuckets);

        return multiBucketAggregationDTO;
    }

    @Deprecated
    private void extractRealisationTeachingLanguageAggregationBuckets(Map<String, Map<String, Long>> languagesCount, SingleBucketAggregation realisations) {
        if(realisations != null && realisations.getAggregations() != null) {
            Filter realisationsFilter = realisations.getAggregations().get("realisationFilter");
            if(realisationsFilter != null && realisationsFilter.getAggregations() != null) {
                Terms status = realisationsFilter.getAggregations().get("status");
                if(status != null && !CollectionUtils.isEmpty(status.getBuckets())) {
                    for(Terms.Bucket statusBucket : status.getBuckets()) {
                        Map<String, Long> languages = new HashMap<>();

                        if(statusBucket.getAggregations() != null) {
                            Terms teachingLanguages = statusBucket.getAggregations().get("teachingLanguages");

                            if (teachingLanguages != null && !CollectionUtils.isEmpty(teachingLanguages.getBuckets())) {
                                for (Terms.Bucket bucket : teachingLanguages.getBuckets()) {
                                    if(!languages.containsKey(bucket.getKeyAsString())) {
                                        // "language X appears in search result realisations at least once" for now. fix later.
                                        languages.put(bucket.getKeyAsString(), 1L);
                                    }
                                }
                            }
                        }

                        if(!languagesCount.containsKey(statusBucket.getKeyAsString())) {
                            languagesCount.put(statusBucket.getKeyAsString(), languages);
                        } else {
                            Map<String, Long> existingLangs = languagesCount.get(statusBucket.getKeyAsString());
                            for(Map.Entry<String, Long> entry : languages.entrySet()) {
                                if(!existingLangs.containsKey(entry.getKey())) {
                                    existingLangs.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private SimpleAggregationDTO mapRealisationAggregation(Aggregations aggregations, String realisationAggregationName,
                                                           String assessmentItemRealisationAggregationName) {
        if(aggregations == null) {
            return null;
        }

        Nested realisationsAggregation = aggregations.get(realisationAggregationName);
        Nested completionOptionsAggregation = aggregations.get(assessmentItemRealisationAggregationName);

        Map<String, Map<String, Long>> allExtractedRealisationBuckets = new HashMap<>();
        Map<String, Map<String, Long>> extractedAssessmentItemRealisationBuckets = new HashMap<>();

        if(realisationsAggregation != null && realisationsAggregation.getAggregations() != null) {
            Filter realisationsFilter = realisationsAggregation.getAggregations().get("realisationsFilter");
            allExtractedRealisationBuckets.putAll(extractRealisationAggregationBuckets(realisationsFilter));
        }

        if(completionOptionsAggregation != null && completionOptionsAggregation.getAggregations() != null) {
            Nested assessmentItems = completionOptionsAggregation.getAggregations().get("assessmentItems");
            if(assessmentItems != null && assessmentItems.getAggregations() != null) {
                Nested realisations = assessmentItems.getAggregations().get("realisations");
                if(realisations != null && realisations.getAggregations() != null) {
                    Filter realisationsFilter = realisations.getAggregations().get("realisationsFilter");
                    extractedAssessmentItemRealisationBuckets.putAll(extractRealisationAggregationBuckets(realisationsFilter));
                }
            }
        }

        for(Map.Entry<String, Map<String, Long>> entry : extractedAssessmentItemRealisationBuckets.entrySet()) {
            if(allExtractedRealisationBuckets.containsKey(entry.getKey())) {
                Map<String, Long> mappedStudyElements = allExtractedRealisationBuckets.get(entry.getKey());

                for(Map.Entry<String, Long> studyElement : entry.getValue().entrySet()) {
                    if (mappedStudyElements.containsKey(studyElement.getKey())) {
                        mappedStudyElements.put(studyElement.getKey(), studyElement.getValue() + mappedStudyElements.get(studyElement.getKey()).longValue());
                    } else {
                        mappedStudyElements.put(studyElement.getKey(), studyElement.getValue());
                    }
                }
            } else {
                allExtractedRealisationBuckets.put(entry.getKey(), entry.getValue());
            }
        }

        return new SimpleMultiBucketAggregationDTO(realisationAggregationName, mapExtractedBucketsToDTO(allExtractedRealisationBuckets));
    }

    private List<SimpleBucketDTO> mapExtractedBucketsToDTO(Map<String, Map<String, Long>> extracted) {
        return extracted.entrySet().stream()
            .map(e -> {
                SimpleBucketDTO bucketDTO = new SimpleBucketDTO(e.getKey(), 0);
                bucketDTO.setBuckets(e.getValue().entrySet().stream()
                    .map(e2 -> new SimpleBucketDTO(e2.getKey(), e2.getValue()))
                    .collect(Collectors.toList()));
                bucketDTO.setCount(e.getValue().keySet().size());
                return bucketDTO;
            }).collect(Collectors.toList());
    }

    private Map<String, Map<String, Long>> extractRealisationAggregationBuckets(Filter realisationFilter) {
        Map<String, Map<String, Long>> extractedBuckets = new HashMap<>();

        if(realisationFilter != null && realisationFilter.getAggregations() != null) {
            ReverseNested byOrganizingOrganisationId = realisationFilter.getAggregations().get("byOrganizingOrganisationId");
            if (byOrganizingOrganisationId != null && byOrganizingOrganisationId.getAggregations() != null) {
                Terms organizingOrganisationId = byOrganizingOrganisationId.getAggregations().get("organizingOrganisationId");
                if (organizingOrganisationId != null && !CollectionUtils.isEmpty(organizingOrganisationId.getBuckets())) {

                    for (Terms.Bucket organizingOrganisationIdBucket : organizingOrganisationId.getBuckets()) {
                        Map<String, Long> studyElementBuckets = new HashMap<>();

                        if (organizingOrganisationIdBucket != null && organizingOrganisationIdBucket.getAggregations() != null) {
                            Terms studyElementId = organizingOrganisationIdBucket.getAggregations().get("studyElementId");
                            if (studyElementId != null && !CollectionUtils.isEmpty(studyElementId.getBuckets())) {
                                for (Terms.Bucket studyElementIdBucket : studyElementId.getBuckets()) {
                                    studyElementBuckets.put(studyElementIdBucket.getKeyAsString(), studyElementIdBucket.getDocCount());
                                }
                            }
                        }

                        extractedBuckets.put(organizingOrganisationIdBucket.getKeyAsString(), studyElementBuckets);
                    }
                }
            }
        }

        return extractedBuckets;
    }

    private List<SimpleBucketDTO> mapBuckets(List<? extends Terms.Bucket> buckets) {
        if(CollectionUtils.isEmpty(buckets)) {
            return Collections.emptyList();
        }

        return buckets.stream()
            .map(bucket -> new SimpleBucketDTO(bucket.getKeyAsString(), bucket.getDocCount()))
            .collect(Collectors.toList());
    }
}
