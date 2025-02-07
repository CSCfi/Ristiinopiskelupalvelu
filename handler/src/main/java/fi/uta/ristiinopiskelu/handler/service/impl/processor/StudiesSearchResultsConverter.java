package fi.uta.ristiinopiskelu.handler.service.impl.processor;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.InternalStudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleBucketDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleMultiBucketAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleSingleBucketAggregationDTO;
import org.springframework.util.CollectionUtils;

import java.util.stream.Collectors;

public class StudiesSearchResultsConverter {

    public StudiesSearchResults convert(InternalStudiesSearchResults results) {
        return new StudiesSearchResults(results.getResults(), results.getAggregations().stream().map(this::convert).collect(Collectors.toList()),
            results.getTotalHits());
    }

    private SimpleAggregationDTO convert(AggregationDTO aggregation) {
        if(aggregation.getType() == AggregationType.MULTI) {
            MultiBucketAggregationDTO multiBucketAggregation = (MultiBucketAggregationDTO) aggregation;

            SimpleMultiBucketAggregationDTO aggregationDTO = new SimpleMultiBucketAggregationDTO();
            aggregationDTO.setName(multiBucketAggregation.getName());
            aggregationDTO.setBuckets(multiBucketAggregation.getBuckets().stream().map(this::convertBucket).collect(Collectors.toList()));
            return aggregationDTO;
        }

        SingleBucketAggregationDTO singleBucketAggregationDTO = (SingleBucketAggregationDTO) aggregation;

        if(!CollectionUtils.isEmpty(singleBucketAggregationDTO.getAggregations())) {
            SimpleMultiBucketAggregationDTO aggregationDTO = new SimpleMultiBucketAggregationDTO();
            aggregationDTO.setName(singleBucketAggregationDTO.getName());
            aggregationDTO.setBuckets(singleBucketAggregationDTO.getAggregations().stream().map(this::convertAggregationToBucket).collect(Collectors.toList()));
            return aggregationDTO;

        }

        SimpleSingleBucketAggregationDTO aggregationDTO = new SimpleSingleBucketAggregationDTO();
        aggregationDTO.setName(singleBucketAggregationDTO.getName());
        aggregationDTO.setCount(singleBucketAggregationDTO.getCount());
        return aggregationDTO;
    }

    private SimpleBucketDTO convertBucket(BucketDTO bucket) {
        SimpleBucketDTO converted = new SimpleBucketDTO();
        converted.setKey(bucket.getKey());
        converted.setCount(bucket.getCount());
        converted.setBuckets(bucket.getAggregations().stream().map(this::convertAggregationToBucket).collect(Collectors.toList()));
        return converted;
    }

    private SimpleBucketDTO convertAggregationToBucket(AggregationDTO aggregation) {
        if(aggregation.getType() == AggregationType.MULTI) {
            MultiBucketAggregationDTO multiAggregation = (MultiBucketAggregationDTO) aggregation;

            SimpleBucketDTO subBucket = new SimpleBucketDTO();
            subBucket.setKey(multiAggregation.getName());
            subBucket.setCount(multiAggregation.getBuckets().size());
            subBucket.setBuckets(multiAggregation.getBuckets().stream().map(this::convertBucket).collect(Collectors.toList()));
            return subBucket;
        }

        SingleBucketAggregationDTO singleAggregation = (SingleBucketAggregationDTO) aggregation;

        SimpleBucketDTO subBucket = new SimpleBucketDTO();
        subBucket.setKey(singleAggregation.getName());
        subBucket.setCount(singleAggregation.getCount());
        subBucket.setBuckets(singleAggregation.getAggregations().stream().map(this::convertAggregationToBucket).collect(Collectors.toList()));
        return subBucket;
    }
}
