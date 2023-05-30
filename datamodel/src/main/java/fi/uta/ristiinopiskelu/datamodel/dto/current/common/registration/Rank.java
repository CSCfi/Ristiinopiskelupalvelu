package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import java.util.Objects;

public class Rank {

    private int rankAll;
    private int maxSeatsAll;
    private int rankCrossStudy;
    private int maxSeatsCrossStudy;

    public Rank() { }
    public Rank(int rankAll, int maxSeatsAll, int rankCrossStudy, int maxSeatsCrossStudy){
        this.rankAll = rankAll;
        this.maxSeatsAll = maxSeatsAll;
        this.rankCrossStudy = rankCrossStudy;
        this.maxSeatsCrossStudy = maxSeatsCrossStudy;
    }
    public int getRankAll() { return rankAll; }

    public void setRankAll(int rankAll) { this.rankAll = rankAll; }

    public int getMaxSeatsAll() { return maxSeatsAll; }

    public void setMaxSeatsAll(int maxSeatsAll) { this.maxSeatsAll = maxSeatsAll; }

    public int getRankCrossStudy() { return rankCrossStudy; }

    public void setRankCrossStudy(int rankCrossStudy) { this.rankCrossStudy = rankCrossStudy; }

    public int getMaxSeatsCrossStudy() { return maxSeatsCrossStudy; }

    public void setMaxSeatsCrossStudy(int maxSeatsCrossStudy) { this.maxSeatsCrossStudy = maxSeatsCrossStudy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rank)) return false;
        Rank rank = (Rank) o;
        return Objects.equals(rankAll, rank.rankAll) &&
            Objects.equals(maxSeatsAll, rank.maxSeatsAll) &&
            Objects.equals(rankCrossStudy, rank.rankCrossStudy) &&
            Objects.equals(maxSeatsCrossStudy, rank.maxSeatsCrossStudy);
    }
}
