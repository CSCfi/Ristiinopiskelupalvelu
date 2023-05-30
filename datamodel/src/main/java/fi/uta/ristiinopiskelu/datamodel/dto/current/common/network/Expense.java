package fi.uta.ristiinopiskelu.datamodel.dto.current.common.network;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Expense {

    @JsonProperty("pay")
    private Boolean pay = null;

    @JsonProperty("pricingBasis")
    private PricingBasis pricingBasis = null;

    @JsonProperty("price")
    private Double price = null;

    public Boolean getPay() {
        return pay;
    }

    public void setPay(Boolean pay) {
        this.pay = pay;
    }

    public PricingBasis getPricingBasis() {
        return pricingBasis;
    }

    public void setPricingBasis(PricingBasis pricingBasis) {
        this.pricingBasis = pricingBasis;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
