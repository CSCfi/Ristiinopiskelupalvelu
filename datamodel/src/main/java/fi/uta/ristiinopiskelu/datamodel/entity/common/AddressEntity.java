package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressEntity {

    private String street;
    private String postalCode;
    private String postOffice;
    private CountryEntity country;

    public static AddressEntity from(Address source) {
        if (source == null) {
            return null;
        }
        AddressEntity target = new AddressEntity();
        target.setStreet(source.getStreet());
        target.setPostalCode(source.getPostalCode());
        target.setPostOffice(source.getPostOffice());
        target.setCountry(source.getCountry() == null ? null : CountryEntity.fromValue(source.getCountry().getCode()));
        return target;
    }
}
