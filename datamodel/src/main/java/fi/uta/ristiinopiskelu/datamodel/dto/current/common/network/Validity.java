package fi.uta.ristiinopiskelu.datamodel.dto.current.common.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.ValidityEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;
import java.util.Arrays;

@Schema(name = "ValidityV9", title = "ValidityV9")
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
        INDEFINITELY;

        public static ContinuityEnum from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Validity.ContinuityEnum continuity) {
            if(continuity == null) {
                return null;
            }

            return Arrays.stream(ContinuityEnum.values())
                .filter(c -> c.name().equals(continuity.name()))
                .findFirst()
                .orElse(null);
        }

        public static ContinuityEnum from(ValidityEntity.ContinuityEntity continuity) {
            if(continuity == null) {
                return null;
            }

            return Arrays.stream(ContinuityEnum.values())
                .filter(c -> c.name().equals(continuity.name()))
                .findFirst()
                .orElse(null);
        }
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

    public static Validity from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Validity source) {
        if (source == null) {
            return null;
        }

        Validity target = new Validity();
        target.setStart(source.getStart());
        target.setEnd(source.getEnd());
        target.setContinuity(ContinuityEnum.from(source.getContinuity()));
        return target;
    }

    public static Validity from(ValidityEntity source) {
        if (source == null) {
            return null;
        }
        Validity target = new Validity();
        target.setContinuity(ContinuityEnum.from(source.getContinuity()));
        target.setStart(source.getStart());
        target.setEnd(source.getEnd());
        return target;
    }
}
