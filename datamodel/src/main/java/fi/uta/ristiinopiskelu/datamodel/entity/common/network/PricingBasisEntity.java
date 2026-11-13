package fi.uta.ristiinopiskelu.datamodel.entity.common.network;

import java.util.Arrays;

public enum PricingBasisEntity {

    EURO_PER_CREDIT,
    OTHER_PRICING_BASIS;

    public static PricingBasisEntity from(fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.PricingBasis source) {
        if (source == null) {
            return null;
        }
        return Arrays.stream(PricingBasisEntity.values())
            .filter(v -> v.name().equals(source.name()))
            .findFirst()
            .orElse(null);
    }

    public static PricingBasisEntity from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.PricingBasis source) {
        if (source == null) {
            return null;
        }
        return Arrays.stream(PricingBasisEntity.values())
            .filter(v -> v.name().equals(source.name()))
            .findFirst()
            .orElse(null);
    }
}
