package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

// Use "BucketDTO" instead
@Schema(name = "SimpleBucket")
@Deprecated(since = "9.0.0")
public class SimpleBucketDTO {
    private String key;
    private long count;
    private List<SimpleBucketDTO> buckets = new ArrayList<>();

    public SimpleBucketDTO() {

    }

    public SimpleBucketDTO(String key, long count) {
        this.key = key;
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public String getKey() {
        return key;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<SimpleBucketDTO> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<SimpleBucketDTO> buckets) {
        this.buckets = buckets;
    }
}
