package fi.uta.ristiinopiskelu.handler.utils;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.BucketDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.MultiBucketAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.SingleBucketAggregationDTO;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.SingleBucketAggregation;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AggregationUtils {

    public static List<AggregationDTO> mapAggregations(Aggregations aggregations) {
        if(aggregations == null || aggregations.asList().size() == 0) {
            return Collections.emptyList();
        }

        List<AggregationDTO> dtos = new ArrayList<>();

        for(Aggregation agg : aggregations.asList()) {
            AggregationDTO dto = mapAggregation(agg);
            if(dto != null) {
                dtos.add(dto);
            }
        }

        return dtos;
    }

    public static AggregationDTO mapAggregation(Aggregation aggregation) {
        if(aggregation instanceof SingleBucketAggregation) {
            SingleBucketAggregation singleBucketAggregation = (SingleBucketAggregation) aggregation;
            SingleBucketAggregationDTO dto = new SingleBucketAggregationDTO();
            dto.setCount(singleBucketAggregation.getDocCount());
            dto.setName(singleBucketAggregation.getName());
            dto.setAggregations(mapAggregations(singleBucketAggregation.getAggregations()));
            return dto;
        } else if (aggregation instanceof MultiBucketsAggregation) {
            MultiBucketsAggregation multiBucketsAggregation = (MultiBucketsAggregation) aggregation;

            MultiBucketAggregationDTO dto = new MultiBucketAggregationDTO();
            dto.setName(multiBucketsAggregation.getName());
            dto.setBuckets(mapBuckets(multiBucketsAggregation.getBuckets()));
            return dto;
        }

        return null;
    }

    public static <B extends MultiBucketsAggregation.Bucket> List<BucketDTO> mapBuckets(List<B> buckets) {
        List<BucketDTO> dtos = new ArrayList<>();

        for(MultiBucketsAggregation.Bucket bucket : buckets) {
            BucketDTO dto = new BucketDTO();
            dto.setKey(bucket.getKeyAsString());
            dto.setCount(bucket.getDocCount());
            dto.setAggregations(mapAggregations(bucket.getAggregations()));
            dtos.add(dto);
        }

        return dtos;
    }

    /**
     * finds a specific aggregation by traveling the whole object tree recursively
     */
    public static Aggregation findAggregation(String name, Aggregations aggregations) {
        if(aggregations == null) {
            return null;
        }

        Aggregation agg = aggregations.get(name);
        if(agg != null) {
            return agg;
        }

        for(Aggregation aggregation : aggregations.asList()) {
            Aggregation subAggregation = findSubAggregation(name, aggregation);
            if(subAggregation != null) {
                return subAggregation;
            }
        }

        return null;
    }

    public static Aggregation findSubAggregation(String name, Aggregation aggregation) {
        if(aggregation == null) {
            return null;
        }

        if(aggregation instanceof SingleBucketAggregation) {
            SingleBucketAggregation singleBucketAggregation = (SingleBucketAggregation) aggregation;
            if(singleBucketAggregation.getAggregations() != null) {
                Aggregation subAg = findAggregation(name, singleBucketAggregation.getAggregations());
                if (subAg != null) {
                    return subAg;
                }
            }
        } else if(aggregation instanceof MultiBucketsAggregation) {
            MultiBucketsAggregation multiBucketsAggregation = (MultiBucketsAggregation) aggregation;
            if(!CollectionUtils.isEmpty(multiBucketsAggregation.getBuckets())) {
                for(MultiBucketsAggregation.Bucket bucket : multiBucketsAggregation.getBuckets()) {
                    if(bucket.getAggregations() != null) {
                        Aggregation subAg = findAggregation(name, bucket.getAggregations());
                        if(subAg != null) {
                            return subAg;
                        }
                    }
                }
            }
        }

        return null;
    }
}
