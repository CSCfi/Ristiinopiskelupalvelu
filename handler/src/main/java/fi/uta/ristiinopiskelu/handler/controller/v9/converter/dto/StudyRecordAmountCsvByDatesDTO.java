package fi.uta.ristiinopiskelu.handler.controller.v9.converter.dto;

import com.opencsv.bean.CsvBindByName;

public class StudyRecordAmountCsvByDatesDTO extends StudyRecordAmountKeyValueCsvDTO {

    @CsvBindByName(column = "LUKUVUOSI")
    private String key;

    @CsvBindByName(column = "SUORITUKSET")
    private String value;

    public StudyRecordAmountCsvByDatesDTO(String key, long value) {
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
