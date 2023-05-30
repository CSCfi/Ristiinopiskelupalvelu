package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

public class BucketDTO {
    private String key;
    private long count;
    private List<BucketDTO> buckets = new ArrayList<>();

    public BucketDTO() {
        
    }

    public BucketDTO(String key, long count) {
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

    public List<BucketDTO> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<BucketDTO> buckets) {
        this.buckets = buckets;
    }
}
