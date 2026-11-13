package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.Rank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankEntity {

    private int rankAll;
    private int maxSeatsAll;
    private int rankCrossStudy;
    private int maxSeatsCrossStudy;

    public static RankEntity from(Rank source) {
        if (source == null) {
            return null;
        }

        return new RankEntity(source.getRankAll(), source.getMaxSeatsAll(), source.getRankCrossStudy(), source.getMaxSeatsCrossStudy());
    }
}
