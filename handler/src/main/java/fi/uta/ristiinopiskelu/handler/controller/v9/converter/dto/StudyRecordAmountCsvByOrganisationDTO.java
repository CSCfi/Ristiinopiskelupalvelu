package fi.uta.ristiinopiskelu.handler.controller.v9.converter.dto;

import com.opencsv.bean.CsvBindAndJoinByName;
import com.opencsv.bean.CsvBindByName;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

public class StudyRecordAmountCsvByOrganisationDTO implements StudyRecordAmountCsvDTO {

    @CsvBindByName(column = "ORGANISAATIO")
    private String organisation;

    @CsvBindAndJoinByName(column = ".*", elementType = String.class)
    private MultiValuedMap<String, String> values = new HashSetValuedHashMap<>();

    public StudyRecordAmountCsvByOrganisationDTO(String organisation, MultiValuedMap<String, String> values) {
        this.organisation = organisation;
        this.values = values;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public MultiValuedMap<String, String> getValues() {
        return values;
    }

    public void setValues(MultiValuedMap<String, String> values) {
        this.values = values;
    }
}
