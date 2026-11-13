package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RankEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Rank {

    private int rankAll;
    private int maxSeatsAll;
    private int rankCrossStudy;
    private int maxSeatsCrossStudy;

    public static Rank from(RankEntity rank) {
        if (rank == null) {
            return null;
        }

        return new Rank(rank.getRankAll(), rank.getMaxSeatsAll(), rank.getRankCrossStudy(), rank.getMaxSeatsCrossStudy());
    }
}
