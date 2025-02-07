package fi.uta.ristiinopiskelu.handler.controller.v9.converter;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.BucketDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.*;
import fi.uta.ristiinopiskelu.handler.controller.v9.converter.dto.*;
import fi.uta.ristiinopiskelu.handler.utils.AggregationUtils;
import fi.uta.ristiinopiskelu.handler.utils.CsvUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudyRecordCsvConverter {

    public static String convertStudyRecordSearchResultsToCsv(StudyRecordSearchResults results) throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        if(results == null || CollectionUtils.isEmpty(results.getResults())) {
            return null;
        }

        List<StudyRecordCsvDTO> converted = results.getResults()
            .stream()
            .flatMap(result -> StudyRecordCsvDTO.from(result).stream())
            .collect(Collectors.toList());

        return CsvUtils.generateCsvContent(converted, StudyRecordCsvDTO.class);
    }

    public static String convertStudyRecordAmountSearchResultsToCsv(StudyRecordAmountSearchResults results, StudyRecordAmountSearchParameters searchParams) throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        if(results == null) {
            return null;
        }

        Map<String, Aggregate> aggregateMap = AggregationUtils.toAggregateMap(results.getAggregations());

        if(results.getAggregations() != null && searchParams.getGroupBy() != null) {
            if (searchParams.getGroupBy() == StudyRecordGrouping.DATES) {
                List<StudyRecordAmountCsvByDatesDTO> dtos = mapMultiBucketsAggregationBucketValues("dateRanges", aggregateMap,
                    StudyRecordAmountCsvByDatesDTO.class);
                return CsvUtils.generateCsvContent(dtos, StudyRecordAmountCsvByDatesDTO.class, "LUKUVUOSI");
            } else if(searchParams.getGroupBy() == StudyRecordGrouping.STUDYELEMENT_IDENTIFIER_CODE) {
                List<StudyRecordAmountCsvByStudyElementDTO> dtos = mapMultiBucketsAggregationBucketValues("studyElementIdentifierCodes",
                    aggregateMap, StudyRecordAmountCsvByStudyElementDTO.class);
                return CsvUtils.generateCsvContent(dtos, StudyRecordAmountCsvByStudyElementDTO.class, "OPINNON_TUNNISTE");
            } else if (searchParams.getGroupBy() == StudyRecordGrouping.RECEIVING_ORGANISATION ||
                searchParams.getGroupBy() == StudyRecordGrouping.SENDING_ORGANISATION) {

                if(searchParams.getDivideBy() != null) {
                    Aggregate organisationAggregation = AggregationUtils.findAggregation("organisation", aggregateMap);

                    List<StudyRecordAmountCsvByOrganisationDTO> dtos = null;

                    if (searchParams.getDivideBy() == StudyRecordDividing.GRADING) {
                        dtos = mapGradingAggregation(organisationAggregation);
                    } else if(searchParams.getDivideBy() == StudyRecordDividing.ORGANISATION_RESPONSIBLE_FOR_COMPLETION) {
                        dtos = mapMultiBucketsSubAggregationByOrganisation("organisationTkCode", organisationAggregation);
                    } else if(searchParams.getDivideBy() == StudyRecordDividing.MIN_EDU_GUIDANCE_AREA) {
                        dtos = mapMultiBucketsSubAggregationByOrganisation("code", organisationAggregation);
                    }

                    if(!CollectionUtils.isEmpty(dtos)) {
                        return CsvUtils.generateCsvContent(dtos, StudyRecordAmountCsvByOrganisationDTO.class, "ORGANISAATIO");
                    }
                }
            }
        }

        MultiValuedMap<String, String> values = new HashSetValuedHashMap<>();
        values.put("SUORITUKSET", String.valueOf(results.getTotalHits()));
        List<StudyRecordAmountCsvByOrganisationDTO> dtos = Collections.singletonList(new StudyRecordAmountCsvByOrganisationDTO("KAIKKI", values));

        return CsvUtils.generateCsvContent(dtos, StudyRecordAmountCsvByOrganisationDTO.class, "ORGANISAATIO");
    }

    private static <T extends StudyRecordAmountKeyValueCsvDTO> List<T> mapMultiBucketsAggregationBucketValues(String aggregationName,
                                                                                                              Map<String, Aggregate> aggregations,
                                                                                                              Class<T> clazz) {

        Aggregate multiBucketsAggregation = AggregationUtils.findAggregation(aggregationName, aggregations);

        if(multiBucketsAggregation != null) {
            Buckets<? extends MultiBucketBase> buckets = AggregationUtils.getMultiBucketAggregationBuckets(multiBucketsAggregation);
            List<BucketDTO> mappedBuckets = AggregationUtils.mapBuckets(buckets);

            if (!CollectionUtils.isEmpty(mappedBuckets)) {
                return mappedBuckets.stream().map(bucket -> {
                    try {
                        Constructor<T> constructor = clazz.getConstructor(String.class, long.class);
                        return constructor.newInstance(bucket.getKey(), bucket.getCount());
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to instantiate requested class " + clazz.getName(), e);
                    }
                }).toList();
            }
        }

        return Collections.emptyList();
    }

    private static List<StudyRecordAmountCsvByOrganisationDTO> mapMultiBucketsSubAggregationByOrganisation(String aggregationName, Aggregate multiBucketsAggregation) {
        if(multiBucketsAggregation == null) {
            return null;
        }

        List<StudyRecordAmountCsvByOrganisationDTO> dtos = new ArrayList<>();

        for(StringTermsBucket bucket : multiBucketsAggregation.sterms().buckets().array()) {

            Aggregate subAggregation = AggregationUtils.findSubAggregation(aggregationName, multiBucketsAggregation);
            if(subAggregation != null) {
                MultiValuedMap<String, String> map = new HashSetValuedHashMap<>();

                for (StringTermsBucket subBucket : subAggregation.sterms().buckets().array()) {
                    map.put(subBucket.key().stringValue(), String.valueOf(subBucket.docCount()));
                }

                dtos.add(new StudyRecordAmountCsvByOrganisationDTO(bucket.key().stringValue(), map));
            }
        }

        return dtos;
    }

    private static List<StudyRecordAmountCsvByOrganisationDTO> mapGradingAggregation(Aggregate organisationAggregation) {
        if(organisationAggregation != null) {
            List<StudyRecordAmountCsvByOrganisationDTO> dtos = new ArrayList<>();

            for (StringTermsBucket organisationBucket : organisationAggregation.sterms().buckets().array()) {
                MultiValuedMap<String, String> map = new HashSetValuedHashMap<>();
                Aggregate approved = AggregationUtils.findAggregation("approvedFilter", organisationBucket.aggregations());
                Aggregate rejected = AggregationUtils.findAggregation("rejectedFilter", organisationBucket.aggregations());
                Aggregate ungraded = AggregationUtils.findAggregation("ungradedFilter", organisationBucket.aggregations());

                map.put("approved", String.valueOf(getAggregationDocCount(approved)));
                map.put("rejected", String.valueOf(getAggregationDocCount(rejected)));
                map.put("ungraded", String.valueOf(getAggregationDocCount(ungraded)));

                dtos.add(new StudyRecordAmountCsvByOrganisationDTO(organisationBucket.key().stringValue(), map));
            }

            return dtos;
        }

        return Collections.emptyList();
    }

    private static long getAggregationDocCount(Aggregate aggregation) {
        if(aggregation != null && aggregation._get() instanceof SingleBucketAggregateBase singleBucketAggregateBase) {
            return singleBucketAggregateBase.docCount();
        }

        return 0;
    }
}
