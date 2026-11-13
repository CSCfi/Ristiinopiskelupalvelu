package fi.uta.ristiinopiskelu.datamodel.dto.current.common.network;

import fi.uta.ristiinopiskelu.datamodel.entity.common.network.PricingBasisEntity;

import java.util.Arrays;

public enum PricingBasis {

    EURO_PER_CREDIT,
    OTHER_PRICING_BASIS;

    public static PricingBasis from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.PricingBasis pricingBasis) {
        if(pricingBasis == null) {
            return null;
        }

        return Arrays.stream(PricingBasis.values())
            .filter(pb -> pb.name().equals(pricingBasis.name()))
            .findFirst()
            .orElse(null);
    }

    public static PricingBasis from(PricingBasisEntity pricingBasis) {
        if(pricingBasis == null) {
            return null;
        }

        return Arrays.stream(PricingBasis.values())
            .filter(pb -> pb.name().equals(pricingBasis.name()))
            .findFirst()
            .orElse(null);
    }
}
