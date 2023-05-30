package fi.uta.ristiinopiskelu.handler.controller.v9.converter;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.*;
import fi.uta.ristiinopiskelu.handler.controller.v9.converter.dto.*;
import fi.uta.ristiinopiskelu.handler.utils.AggregationUtils;
import fi.uta.ristiinopiskelu.handler.utils.CsvUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.SingleBucketAggregation;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        if(results.getAggregations() != null && searchParams.getGroupBy() != null) {
            if (searchParams.getGroupBy() == StudyRecordGrouping.DATES) {
                List<StudyRecordAmountCsvByDatesDTO> dtos = mapMultiBucketsAggregationBucketValues("dateRanges", results.getAggregations(),
                    StudyRecordAmountCsvByDatesDTO.class);
                return CsvUtils.generateCsvContent(dtos, StudyRecordAmountCsvByDatesDTO.class, "LUKUVUOSI");
            } else if(searchParams.getGroupBy() == StudyRecordGrouping.STUDYELEMENT_IDENTIFIER_CODE) {
                List<StudyRecordAmountCsvByStudyElementDTO> dtos = mapMultiBucketsAggregationBucketValues("studyElementIdentifierCodes",
                    results.getAggregations(), StudyRecordAmountCsvByStudyElementDTO.class);
                return CsvUtils.generateCsvContent(dtos, StudyRecordAmountCsvByStudyElementDTO.class, "OPINNON_TUNNISTE");
            } else if (searchParams.getGroupBy() == StudyRecordGrouping.RECEIVING_ORGANISATION ||
                searchParams.getGroupBy() == StudyRecordGrouping.SENDING_ORGANISATION) {

                if(searchParams.getDivideBy() != null) {
                    MultiBucketsAggregation organisationAggregation = (MultiBucketsAggregation) AggregationUtils.findAggregation("organisation", results.getAggregations());

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

    private static <T extends StudyRecordAmountKeyValueCsvDTO> List<T> mapMultiBucketsAggregationBucketValues(String aggregationName, Aggregations aggregations, Class<T> clazz) {
        MultiBucketsAggregation multiBucketsAggregation = (MultiBucketsAggregation) AggregationUtils.findAggregation(aggregationName, aggregations);

        if(multiBucketsAggregation != null) {
            List<T> dtos = new ArrayList<>();

            for(MultiBucketsAggregation.Bucket bucket : multiBucketsAggregation.getBuckets()) {
                try {
                    Constructor<T> constructor = clazz.getConstructor(String.class, long.class);
                    T instance = constructor.newInstance(bucket.getKeyAsString(), bucket.getDocCount());
                    dtos.add(instance);
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to instantiate requested class " + clazz.getName(), e);
                }
            }

            return dtos;
        }

        return Collections.emptyList();
    }

    private static List<StudyRecordAmountCsvByOrganisationDTO> mapMultiBucketsSubAggregationByOrganisation(String aggregationName, MultiBucketsAggregation multiBucketsAggregation) {
        if(multiBucketsAggregation == null) {
            return null;
        }

        List<StudyRecordAmountCsvByOrganisationDTO> dtos = new ArrayList<>();

        for(MultiBucketsAggregation.Bucket bucket : multiBucketsAggregation.getBuckets()) {
            MultiBucketsAggregation subAggregation = (MultiBucketsAggregation) AggregationUtils.findAggregation(aggregationName, bucket.getAggregations());
            if(subAggregation != null) {
                MultiValuedMap<String, String> map = new HashSetValuedHashMap<>();

                for (MultiBucketsAggregation.Bucket subBucket : subAggregation.getBuckets()) {
                    map.put(subBucket.getKeyAsString(), String.valueOf(subBucket.getDocCount()));
                }

                dtos.add(new StudyRecordAmountCsvByOrganisationDTO(bucket.getKeyAsString(), map));
            }
        }

        return dtos;
    }

    private static List<StudyRecordAmountCsvByOrganisationDTO> mapGradingAggregation(MultiBucketsAggregation organisationAggregation) {
        if(organisationAggregation != null) {
            List<StudyRecordAmountCsvByOrganisationDTO> dtos = new ArrayList<>();

            for (MultiBucketsAggregation.Bucket organisationBucket : organisationAggregation.getBuckets()) {
                MultiValuedMap<String, String> map = new HashSetValuedHashMap<>();
                SingleBucketAggregation approved = (SingleBucketAggregation) AggregationUtils.findAggregation("approvedFilter", organisationBucket.getAggregations());
                SingleBucketAggregation rejected = (SingleBucketAggregation) AggregationUtils.findAggregation("rejectedFilter", organisationBucket.getAggregations());
                SingleBucketAggregation ungraded = (SingleBucketAggregation) AggregationUtils.findAggregation("ungradedFilter", organisationBucket.getAggregations());

                map.put("approved", String.valueOf(getAggregationDocCount(approved)));
                map.put("rejected", String.valueOf(getAggregationDocCount(rejected)));
                map.put("ungraded", String.valueOf(getAggregationDocCount(ungraded)));

                dtos.add(new StudyRecordAmountCsvByOrganisationDTO(organisationBucket.getKeyAsString(), map));
            }

            return dtos;
        }

        return Collections.emptyList();
    }

    private static long getAggregationDocCount(SingleBucketAggregation aggregation) {
        if(aggregation != null) {
            return aggregation.getDocCount();
        }

        return 0;
    }
}
