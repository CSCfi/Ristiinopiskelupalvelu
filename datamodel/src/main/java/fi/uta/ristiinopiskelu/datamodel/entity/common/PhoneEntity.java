package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhoneEntity   {

    private String number;
    private String description;

    public static PhoneEntity from(Phone source) {
        if (source == null) {
            return null;
        }
        PhoneEntity target = new PhoneEntity();
        target.setNumber(source.getNumber());
        target.setDescription(source.getDescription());
        return target;
    }
}




