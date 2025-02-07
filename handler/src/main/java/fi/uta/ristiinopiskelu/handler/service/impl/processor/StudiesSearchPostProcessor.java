package fi.uta.ristiinopiskelu.handler.service.impl.processor;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.InnerHitsResult;
import co.elastic.clients.json.JsonData;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.InternalStudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.BucketDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.MultiBucketAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.SingleBucketAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.service.StudiesService;
import fi.uta.ristiinopiskelu.handler.utils.AggregationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StudiesSearchPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StudiesSearchPostProcessor.class);

    private StudiesService studiesService;

    public StudiesSearchPostProcessor(StudiesService studiesService) {
        this.studiesService = studiesService;
    }

    public InternalStudiesSearchResults postProcess(HitsMetadata<StudyElementEntity> searchHits, Map<String, Aggregate> aggregations, StudiesSearchParameters searchParams) {
        List<StudyElementEntity> results = searchHits.hits().stream()
            .map(h -> {
                StudyElementEntity entity = h.source();
                Map<String, InnerHitsResult> innerHits = h.innerHits();

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
                            mapCourseUnitRealisations(innerHits.get("assessmentItemRealisationsWithTeachingLanguagesQuery")));
                    }

                    if(innerHits.containsKey("assessmentItemRealisationsWithoutTeachingLanguagesQuery")) {
                        assessmentItemRealisationsWithoutTeachingLanguagesQueryHits.addAll(
                                mapCourseUnitRealisations(innerHits.get("assessmentItemRealisationsWithoutTeachingLanguagesQuery")));
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
            }).toList();

        InternalStudiesSearchResults studiesSearchResults = new InternalStudiesSearchResults();
        studiesSearchResults.setResults(results.stream().map(studiesService::toRestDTO).collect(Collectors.toList()));
        studiesSearchResults.setAggregations(convertAggregations(aggregations));
        studiesSearchResults.setTotalHits(searchHits.total().value());
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
    
    private List<CourseUnitRealisationEntity> mapCourseUnitRealisations(InnerHitsResult innerHits) {

        if(innerHits == null || innerHits.hits() == null || CollectionUtils.isEmpty(innerHits.hits().hits())) {
            return Collections.emptyList();
        }

        List<CourseUnitRealisationEntity> mapped = new ArrayList<>();

        for(Hit<JsonData> innerHit : innerHits.hits().hits()) {
            mapped.add(innerHit.source().to(CourseUnitRealisationEntity.class));
        }

        return mapped;
    }

    private List<AggregationDTO> convertAggregations(Map<String, Aggregate> aggregations) {
        List<AggregationDTO> convertedAggregations = new ArrayList<>();

        if (!CollectionUtils.isEmpty(aggregations)) {
            convertedAggregations.add(mapTeachingLanguageAggregation("teachingLanguages", aggregations.get("teachingLanguages")));
            // deprecated since v9.0.0 in favor of "studyElementsByRealisationTeachingLanguages"
            convertedAggregations.add(mapRealisationTeachingLanguageAggregation(aggregations,
                "realisationTeachingLanguages",
                "assessmentItemRealisationTeachingLanguages"));

            AggregationDTO networksAggregation = mapNetworkAggregation("networks", aggregations.get("networks"));
            if(networksAggregation != null) {
                convertedAggregations.add(networksAggregation);
            }
            
            convertedAggregations.add(mapOrganisationAggregation("organisations", aggregations.get("organisations")));
            convertedAggregations.add(mapTypeAggregation("types", aggregations.get("types")));

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

    private MultiBucketAggregationDTO mapTeachingLanguageAggregation(String name, Aggregate aggregate) {
        StringTermsAggregate languages = aggregate.sterms();

        List<BucketDTO> languageBuckets = AggregationUtils.mapBuckets(languages.buckets());

        return new MultiBucketAggregationDTO(name, languageBuckets);
    }

    private AggregationDTO mapNetworkAggregation(String name, Aggregate aggregate) {
        if(aggregate == null) {
            return null;
        }

        NestedAggregate networks = aggregate.nested();
        StringTermsAggregate networkIds = networks.aggregations().get("id").sterms();
        List<BucketDTO> networkBuckets = AggregationUtils.mapBuckets(networkIds.buckets());

        return new MultiBucketAggregationDTO(name, networkBuckets);
    }

    private AggregationDTO mapOrganisationAggregation(String name, Aggregate aggregate) {
        StringTermsAggregate organisations = aggregate.sterms();

        List<BucketDTO> organisationBuckets = AggregationUtils.mapBuckets(organisations.buckets());
        return new MultiBucketAggregationDTO(name, organisationBuckets);
    }
    


    private AggregationDTO mapTypeAggregation(String name, Aggregate aggregate) {
        StringTermsAggregate types = aggregate.sterms();

        List<BucketDTO> typeBuckets = AggregationUtils.mapBuckets(types.buckets());
        return new MultiBucketAggregationDTO(name, typeBuckets);
    }
    
    private AggregationDTO mapRealisationTeachingLanguagesByStudyElementAggregation(String finalName, Map<String, Aggregate> aggregations,
                                                                                    String realisationTeachingLanguageAggregationName,
                                                                                    String assessmentItemRealisationTeachingLanguageAggregationName) {
        if(CollectionUtils.isEmpty(aggregations)) {
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

        List<BucketDTO> mappedBuckets = studyElementCountByLang.entrySet().stream()
            .map(entry -> new BucketDTO(entry.getKey(), entry.getValue())).collect(Collectors.toList());

        MultiBucketAggregationDTO multiBucketAggregationDTO = new MultiBucketAggregationDTO();
        multiBucketAggregationDTO.setName(finalName);
        multiBucketAggregationDTO.setBuckets(mappedBuckets);

        return multiBucketAggregationDTO;
    }

    private void extractRealisationTeachingLanguagesByStudyElementAggregationBuckets(Map<String, Map<String, Long>> languagesCount, Aggregate aggregation) {
        if(aggregation == null) {
            return;
        }

        Aggregate statuses = AggregationUtils.findSubAggregation("status", aggregation);
        if(statuses != null && statuses.isSterms() && statuses.sterms().buckets() != null) {
            for(StringTermsBucket statusBucket : statuses.sterms().buckets().array()) {
                Aggregate teachingLanguages = statusBucket.aggregations().get("teachingLanguages");
                if (teachingLanguages != null && teachingLanguages.isSterms() && teachingLanguages.sterms().buckets() != null) {
                    for (StringTermsBucket teachingLanguageBucket : teachingLanguages.sterms().buckets().array()) {
                        Aggregate studyElements = teachingLanguageBucket.aggregations().get("studyElements");
                        if(studyElements != null && studyElements.isReverseNested() && !CollectionUtils.isEmpty(studyElements.reverseNested().aggregations())) {
                            Aggregate idAggregation = studyElements.reverseNested().aggregations().get("id");
                            if (idAggregation != null && idAggregation.isSterms() && idAggregation.sterms().buckets() != null) {
                                for (StringTermsBucket idBucket : idAggregation.sterms().buckets().array()) {
                                    Map<String, Long> existing = languagesCount.get(idBucket.key().stringValue());
                                    if (existing == null) {
                                        Map<String, Long> map = new HashMap<>();
                                        map.put(teachingLanguageBucket.key().stringValue(), idBucket.docCount());
                                        languagesCount.put(idBucket.key().stringValue(), map);
                                    } else {
                                        Long existingCount = existing.get(teachingLanguageBucket.key().stringValue());
                                        if (existingCount == null) {
                                            existing.put(teachingLanguageBucket.key().stringValue(), idBucket.docCount());
                                        } else {
                                            existing.put(teachingLanguageBucket.key().stringValue(), idBucket.docCount() + existingCount);
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
    private AggregationDTO mapRealisationTeachingLanguageAggregation(Map<String, Aggregate> aggregations, String realisationTeachingLanguageAggregationName,
                                                                           String assessmentItemRealisationTeachingLanguageAggregationName) {
        if(aggregations == null) {
            return null;
        }

        Map<String, Map<String, Long>> languagesCount = new HashMap<>();
        extractRealisationTeachingLanguageAggregationBuckets(realisationTeachingLanguageAggregationName, aggregations, languagesCount);
        extractRealisationTeachingLanguageAggregationBuckets(assessmentItemRealisationTeachingLanguageAggregationName, aggregations, languagesCount);

        List<AggregationDTO> mappedAggs = new ArrayList<>();
        for(Map.Entry<String, Map<String, Long>> entry : languagesCount.entrySet()) {

            List<BucketDTO> langs = entry.getValue().entrySet().stream().map(e -> new BucketDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

            MultiBucketAggregationDTO aggregationDTO = new MultiBucketAggregationDTO();
            aggregationDTO.setName(entry.getKey());
            aggregationDTO.setBuckets(langs);

            mappedAggs.add(aggregationDTO);
        }

        SingleBucketAggregationDTO singleBucketAggregationDTO = new SingleBucketAggregationDTO();
        singleBucketAggregationDTO.setName(realisationTeachingLanguageAggregationName);
        singleBucketAggregationDTO.setCount(mappedAggs.size());
        singleBucketAggregationDTO.setAggregations(mappedAggs);

        return singleBucketAggregationDTO;
    }


    @Deprecated
    private void extractRealisationTeachingLanguageAggregationBuckets(String aggregationName, Map<String, Aggregate> aggregations,
                                                                      Map<String, Map<String, Long>> languagesCount) {

        Aggregate realisationTeachingLanguagesAggregation = aggregations.get(aggregationName);
        if(realisationTeachingLanguagesAggregation != null) {
            Aggregate realisations = AggregationUtils.findSubAggregation("realisationFilter", realisationTeachingLanguagesAggregation);
            if(realisations != null && realisations.isFilter() && !CollectionUtils.isEmpty(realisations.filter().aggregations())) {
                Aggregate status = AggregationUtils.findSubAggregation("status", realisations);

                if (status != null && status.isSterms() && status.sterms().buckets() != null) {
                    for (StringTermsBucket statusBucket : status.sterms().buckets().array()) {
                        Map<String, Long> languages = new HashMap<>();

                        if (!CollectionUtils.isEmpty(statusBucket.aggregations())) {
                            StringTermsAggregate teachingLanguages = statusBucket.aggregations().get("teachingLanguages").sterms();

                            if (teachingLanguages != null && teachingLanguages.buckets() != null) {
                                for (StringTermsBucket bucket : teachingLanguages.buckets().array()) {
                                    if (!languages.containsKey(bucket.key().stringValue())) {
                                        // "language X appears in search result realisations at least once" for now. fix later.
                                        languages.put(bucket.key().stringValue(), 1L);
                                    }
                                }
                            }
                        }

                        if (!languagesCount.containsKey(statusBucket.key().stringValue())) {
                            languagesCount.put(statusBucket.key().stringValue(), languages);
                        } else {
                            Map<String, Long> existingLangs = languagesCount.get(statusBucket.key().stringValue());
                            for (Map.Entry<String, Long> entry : languages.entrySet()) {
                                if (!existingLangs.containsKey(entry.getKey())) {
                                    existingLangs.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private AggregationDTO mapRealisationAggregation(Map<String, Aggregate> aggregations, String realisationAggregationName,
                                                           String assessmentItemRealisationAggregationName) {
        if(CollectionUtils.isEmpty(aggregations)) {
            return null;
        }

        Map<String, Map<String, Long>> allExtractedRealisationBuckets = new HashMap<>();
        Map<String, Map<String, Long>> extractedAssessmentItemRealisationBuckets = new HashMap<>();

        allExtractedRealisationBuckets.putAll(extractRealisationAggregationBuckets(realisationAggregationName, aggregations));
        extractedAssessmentItemRealisationBuckets.putAll(extractRealisationAggregationBuckets(assessmentItemRealisationAggregationName, aggregations));

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

        List<AggregationDTO> mappedBuckets = mapExtractedBucketsToDTO(allExtractedRealisationBuckets);

        SingleBucketAggregationDTO singleBucketAggregationDTO = new SingleBucketAggregationDTO(realisationAggregationName, mappedBuckets.size());
        singleBucketAggregationDTO.setAggregations(mappedBuckets);
        return singleBucketAggregationDTO;
    }

    private List<AggregationDTO> mapExtractedBucketsToDTO(Map<String, Map<String, Long>> extracted) {
        return extracted.entrySet().stream()
            .map(e -> {
                MultiBucketAggregationDTO multiBucketAggregationDTO = new MultiBucketAggregationDTO();
                multiBucketAggregationDTO.setBuckets(e.getValue().entrySet().stream()
                    .map(e2 -> new BucketDTO(e2.getKey(), e2.getValue()))
                    .collect(Collectors.toList()));
                multiBucketAggregationDTO.setName(e.getKey());
                return multiBucketAggregationDTO;
            }).collect(Collectors.toList());
    }

    private Map<String, Map<String, Long>> extractRealisationAggregationBuckets(String aggregationName, Map<String, Aggregate> aggregations) {

        Map<String, Map<String, Long>> extractedBuckets = new HashMap<>();
        Aggregate realisationsAggregation = aggregations.get(aggregationName);

        if(realisationsAggregation != null && realisationsAggregation.isNested() && !CollectionUtils.isEmpty(realisationsAggregation.nested().aggregations())) {
            Aggregate realisationsFilter = AggregationUtils.findSubAggregation("realisationsFilter", realisationsAggregation);

            if(realisationsFilter != null && realisationsFilter.isFilter() && !CollectionUtils.isEmpty(realisationsFilter.filter().aggregations())) {
                ReverseNestedAggregate byOrganizingOrganisationId = realisationsFilter.filter().aggregations().get("byOrganizingOrganisationId").reverseNested();
                if (byOrganizingOrganisationId != null && !CollectionUtils.isEmpty(byOrganizingOrganisationId.aggregations())) {
                    StringTermsAggregate organizingOrganisationId = byOrganizingOrganisationId.aggregations().get("organizingOrganisationId").sterms();
                    if (organizingOrganisationId != null && organizingOrganisationId.buckets() != null) {

                        for (StringTermsBucket organizingOrganisationIdBucket : organizingOrganisationId.buckets().array()) {
                            Map<String, Long> studyElementBuckets = new HashMap<>();

                            if (organizingOrganisationIdBucket != null && !CollectionUtils.isEmpty(organizingOrganisationIdBucket.aggregations())) {
                                StringTermsAggregate studyElementId = organizingOrganisationIdBucket.aggregations().get("studyElementId").sterms();
                                if (studyElementId != null && studyElementId.buckets() != null) {
                                    for (StringTermsBucket studyElementIdBucket : studyElementId.buckets().array()) {
                                        studyElementBuckets.put(studyElementIdBucket.key().stringValue(), studyElementIdBucket.docCount());
                                    }
                                }
                            }

                            extractedBuckets.put(organizingOrganisationIdBucket.key().stringValue(), studyElementBuckets);
                        }
                    }
                }
            }
        }

        return extractedBuckets;
    }
}
