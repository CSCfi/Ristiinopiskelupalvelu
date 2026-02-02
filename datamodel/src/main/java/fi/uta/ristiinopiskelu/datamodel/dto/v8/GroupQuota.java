package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "GroupQuotaV8", title = "GroupQuotaV8")
public class GroupQuota {
    private List<String> networkIdentifiers;
    private Integer minSeats;
    private Integer maxSeats;

    public List<String> getNetworkIdentifiers() {
        return networkIdentifiers;
    }

    public void setNetworkIdentifiers(List<String> networkIdentifiers) {
        this.networkIdentifiers = networkIdentifiers;
    }

    public Integer getMinSeats() {
        return minSeats;
    }

    public void setMinSeats(Integer minSeats) {
        this.minSeats = minSeats;
    }

    public Integer getMaxSeats() {
        return maxSeats;
    }

    public void setMaxSeats(Integer maxSeats) {
        this.maxSeats = maxSeats;
    }
}
