package fi.uta.ristiinopiskelu.datamodel.dto.current.common.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;

public class Validity {
    @JsonProperty("continuity")
    private ContinuityEnum continuity = null;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    @JsonProperty("start")
    private OffsetDateTime start = null;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    @JsonProperty("end")
    private OffsetDateTime end = null;

    public enum ContinuityEnum {

        FIXED,
        INDEFINITELY
    }

    public Validity() {

    }

    public Validity(ContinuityEnum continuity, OffsetDateTime start, OffsetDateTime end) {
        this.continuity = continuity;
        this.start = start;
        this.end = end;
    }

    public ContinuityEnum getContinuity() {
        return continuity;
    }

    public void setContinuity(ContinuityEnum continuity) {
        this.continuity = continuity;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public void setStart(OffsetDateTime start) {
        this.start = start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public void setEnd(OffsetDateTime end) {
        this.end = end;
    }
}
