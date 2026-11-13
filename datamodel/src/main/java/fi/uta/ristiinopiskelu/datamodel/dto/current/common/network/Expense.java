package fi.uta.ristiinopiskelu.datamodel.dto.current.common.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.ExpenseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExpenseV9", title = "ExpenseV9")
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

    public static Expense from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Expense source) {
        if (source == null) {
            return null;
        }

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Expense target =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Expense();
        target.setPay(source.getPay());
        target.setPrice(source.getPrice());
        target.setPricingBasis(PricingBasis.from(source.getPricingBasis()));
        return target;
    }

    public static Expense from(ExpenseEntity source) {
        if (source == null) {
            return null;
        }
        Expense target = new Expense();
        target.setPay(source.getPay());
        target.setPricingBasis(PricingBasis.from(source.getPricingBasis()));
        target.setPrice(source.getPrice());
        return target;
    }
}
