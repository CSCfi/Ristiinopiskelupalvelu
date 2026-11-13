package fi.uta.ristiinopiskelu.datamodel.entity.common.network;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Expense;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseEntity {

    private Boolean pay = null;
    private PricingBasisEntity pricingBasis = null;
    private Double price = null;

    public static ExpenseEntity from(fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Expense source) {
        if (source == null) {
            return null;
        }

        ExpenseEntity target = new ExpenseEntity();
        target.setPay(source.getPay());
        target.setPrice(source.getPrice());
        target.setPricingBasis(PricingBasisEntity.from(source.getPricingBasis()));
        return target;
    }

    public static ExpenseEntity from(Expense source) {
        if (source == null) {
            return null;
        }

        ExpenseEntity target = new ExpenseEntity();
        target.setPay(source.getPay());
        target.setPrice(source.getPrice());
        target.setPricingBasis(PricingBasisEntity.from(source.getPricingBasis()));
        return target;
    }
}
