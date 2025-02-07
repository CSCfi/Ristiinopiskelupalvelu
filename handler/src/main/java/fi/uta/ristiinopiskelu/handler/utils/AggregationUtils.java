package fi.uta.ristiinopiskelu.handler.utils;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.BucketDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.MultiBucketAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.SingleBucketAggregationDTO;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class AggregationUtils {

    public static List<AggregationDTO> mapAggregations(Map<String, Aggregate> aggregations) {
        if(CollectionUtils.isEmpty(aggregations)) {
            return Collections.emptyList();
        }

        List<AggregationDTO> dtos = new ArrayList<>();

        for(Entry<String, Aggregate> agg : aggregations.entrySet()) {
            AggregationDTO dto = mapAggregate(agg.getKey(), agg.getValue());
            if(dto != null) {
                dtos.add(dto);
            }
        }

        return dtos;
    }

    public static List<AggregationDTO> mapAggregates(Map<String, Aggregate> aggregates) {
        return aggregates.entrySet().stream().map(entry -> mapAggregate(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    public static AggregationDTO mapAggregate(String key, Aggregate aggregate) {
        if (aggregate._get() instanceof MultiBucketAggregateBase<?> multiBucketAggregate) {
            MultiBucketAggregationDTO dto = new MultiBucketAggregationDTO();
            dto.setName(key);

            if(multiBucketAggregate.buckets().isArray()) {
                dto.setBuckets(mapBuckets((List<? extends MultiBucketBase>) multiBucketAggregate.buckets().array()));
            } else {
                dto.setBuckets(mapBuckets((Map<String, ? extends MultiBucketBase>) multiBucketAggregate.buckets().keyed()));
            }
            return dto;
        }

        if(aggregate._get() instanceof SingleBucketAggregateBase singleBucketAggregate) {
            SingleBucketAggregationDTO dto = new SingleBucketAggregationDTO();
            dto.setName(key);
            dto.setCount(singleBucketAggregate.docCount());
            dto.setAggregations(mapAggregates(singleBucketAggregate.aggregations()));
            return dto;
        }

        return null;
    }

    public static List<BucketDTO> mapBuckets(List<? extends MultiBucketBase> buckets) {
        List<BucketDTO> dtos = new ArrayList<>();

        for(MultiBucketBase bucket : buckets) {
            BucketDTO dto = new BucketDTO();

            // add these as needed. MultiBucketBase has no key exposed.
            if(bucket instanceof StringTermsBucket stb) {
                dto.setKey(stb.key().stringValue());
            } else if(bucket instanceof RangeBucket rb) {
                dto.setKey(rb.key());
            } else {
                throw new IllegalStateException("Unhandled aggregation bucket type '%s'".formatted(bucket.getClass().getName()));
            }

            dto.setCount(bucket.docCount());
            dto.setAggregations(mapAggregates(bucket.aggregations()));
            dtos.add(dto);
        }

        return dtos;
    }

    public static List<BucketDTO> mapBuckets(Map<String, ? extends MultiBucketBase> buckets) {
        List<BucketDTO> dtos = new ArrayList<>();

        for(Map.Entry<String, ? extends MultiBucketBase> bucket : buckets.entrySet()) {
            BucketDTO dto = new BucketDTO();
            dto.setKey(bucket.getKey());
            dto.setCount(bucket.getValue().docCount());
            dto.setAggregations(mapAggregates(bucket.getValue().aggregations()));
            dtos.add(dto);
        }

        return dtos;
    }

    public static List<BucketDTO> mapBuckets(Buckets<? extends MultiBucketBase> buckets) {
        if(buckets.isArray()) {
            return mapBuckets(buckets.array());
        }

        return mapBuckets(buckets.keyed());
    }

    /**
     * finds a specific aggregation by traveling the whole object tree recursively
     */
    public static Aggregate findAggregation(String name, Map<String, Aggregate> aggregations) {
        if(aggregations == null) {
            return null;
        }

        Aggregate agg = aggregations.get(name);
        if(agg != null) {
            return agg;
        }

        for(Aggregate aggregation : aggregations.values()) {
            Aggregate subAggregation = findSubAggregation(name, aggregation);

            if(subAggregation != null) {
                return subAggregation;
            }
        }

        return null;
    }

    public static Aggregate findSubAggregation(String name, Aggregate aggregation) {
        if(aggregation == null) {
            return null;
        }

        if(aggregation._get() instanceof SingleBucketAggregateBase) {
            SingleBucketAggregateBase singleBucketAggregation = (SingleBucketAggregateBase) aggregation._get();
            if(!CollectionUtils.isEmpty(singleBucketAggregation.aggregations())) {
                Aggregate subAg = findAggregation(name, singleBucketAggregation.aggregations());
                if (subAg != null) {
                    return subAg;
                }
            }
        } else if(aggregation._get() instanceof MultiBucketAggregateBase<?>) {
            MultiBucketAggregateBase<? extends MultiBucketBase> multiBucketsAggregation = (MultiBucketAggregateBase<? extends MultiBucketBase>) aggregation._get();
            if(!CollectionUtils.isEmpty(multiBucketsAggregation.buckets().array())) {
                for(MultiBucketBase bucket : multiBucketsAggregation.buckets().array()) {
                    if(!CollectionUtils.isEmpty(bucket.aggregations())) {
                        Aggregate subAg = findAggregation(name, bucket.aggregations());
                        if(subAg != null) {
                            return subAg;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static Map<String, Aggregate> toAggregateMap(ElasticsearchAggregations aggregations) {
        if(aggregations == null || CollectionUtils.isEmpty(aggregations.aggregationsAsMap())) {
            return Collections.emptyMap();
        }

        return aggregations.aggregationsAsMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        e -> e.getValue().aggregation().getAggregate())
                );
    }

    public static Buckets<? extends MultiBucketBase> getMultiBucketAggregationBuckets(Aggregate aggregation) {
        if(aggregation._get() instanceof MultiBucketAggregateBase<?>) {
            MultiBucketAggregateBase<? extends MultiBucketBase> multiBucketsAggregation = (MultiBucketAggregateBase<? extends MultiBucketBase>) aggregation._get();
            return multiBucketsAggregation.buckets();
        }

        throw new IllegalArgumentException("Aggregation '%s' is not a multi bucket aggregation".formatted(aggregation._get().getClass().getName()));
    }
}
