package fi.uta.ristiinopiskelu.datamodel.entity.common.network;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;
import java.util.Arrays;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidityEntity {

    private ContinuityEntity continuity = null;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime start = null;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime end = null;

    public enum ContinuityEntity {
        FIXED,
        INDEFINITELY;

        public static ContinuityEntity from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Validity.ContinuityEnum continuity) {
            if(continuity == null) {
                return null;
            }

            return Arrays.stream(ValidityEntity.ContinuityEntity.values())
                .filter(c -> c.name().equals(continuity.name()))
                .findFirst()
                .orElse(null);
        }

        public static ContinuityEntity from(Validity.ContinuityEnum continuity) {
            if(continuity == null) {
                return null;
            }

            return Arrays.stream(ValidityEntity.ContinuityEntity.values())
                .filter(c -> c.name().equals(continuity.name()))
                .findFirst()
                .orElse(null);
        }
    }

    public static ValidityEntity from(Validity source) {
        if (source == null) {
            return null;
        }

        ValidityEntity target = new ValidityEntity();
        target.setStart(source.getStart());
        target.setEnd(source.getEnd());
        target.setContinuity(ValidityEntity.ContinuityEntity.from(source.getContinuity()));
        return target;
    }

    public static ValidityEntity from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Validity source) {
        if (source == null) {
            return null;
        }

        ValidityEntity target = new ValidityEntity();
        target.setStart(source.getStart());
        target.setEnd(source.getEnd());
        target.setContinuity(ValidityEntity.ContinuityEntity.from(source.getContinuity()));
        return target;
    }
}
