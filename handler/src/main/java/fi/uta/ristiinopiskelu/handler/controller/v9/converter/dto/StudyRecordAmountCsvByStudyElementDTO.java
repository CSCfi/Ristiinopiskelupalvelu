package fi.uta.ristiinopiskelu.handler.controller.v9.converter.dto;

import com.opencsv.bean.CsvBindByName;

public class StudyRecordAmountCsvByStudyElementDTO extends StudyRecordAmountKeyValueCsvDTO {

    @CsvBindByName(column = "OPINNON_TUNNISTE")
    private String key;

    @CsvBindByName(column = "SUORITUKSET")
    private String value;

    public StudyRecordAmountCsvByStudyElementDTO(String key, long value) {
        this.key = key;
        this.value = String.valueOf(value);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }
}
