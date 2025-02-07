package fi.uta.ristiinopiskelu.handler.controller.v9;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyrecord.StudyRecordReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.*;
import fi.uta.ristiinopiskelu.handler.controller.v9.converter.StudyRecordCsvConverter;
import fi.uta.ristiinopiskelu.handler.service.StudyRecordService;
import fi.uta.ristiinopiskelu.handler.utils.AggregationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "studyrecords", description = "Opintosuoritukset v9")
@RequestMapping("/api/v9/studyrecords")
@RestController
public class StudyRecordControllerV9 {

    private static final Logger logger = LoggerFactory.getLogger(StudyRecordControllerV9.class);

    @Autowired
    private StudyRecordService studyRecordService;

    @Operation(summary = "Hae opintosuorituksia", description = "Operaatio hakee opintosuorituksia annetuilla hakuehdoilla.", tags = "studyrecords")
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public List<StudyRecordReadDTO> findStudyRecords(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                              @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody StudyRecordSearchParameters searchParams) {

        return new ArrayList<>(studyRecordService.search(SSL_CLIENT_S_DN_O, searchParams).getResults());
    }

    @Operation(summary = "Hae opintosuorituksia CSV-muodossa", description = "Operaatio hakee opintosuorituksia CSV-muodossa annetuilla hakuehdoilla.",
        tags = "studyrecords")
    @RequestMapping(value = "/search/csv", method = RequestMethod.POST, produces = "text/csv")
    public ResponseEntity<String> findStudyRecordsAsCSV(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                     @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody StudyRecordSearchParameters searchParams) {
        StudyRecordSearchResults results = studyRecordService.search(SSL_CLIENT_S_DN_O, searchParams);

        String content;

        try {
            content = StudyRecordCsvConverter.convertStudyRecordSearchResultsToCsv(results);
        } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            logger.error("Error while generating study record CSV file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        return generateAndReturnCsvContent(content, String.format("ripa_opintosuoritukset_%s.csv", LocalDate.now()));
    }

    @Operation(summary = "Hae opintosuoritusten lukumääriä", description = "Operaatio hakee opintosuoritusten lukumääriä annetuilla hakuehdoilla.",
        tags = "studyrecords")
    @RequestMapping(value = "/search/amounts", method = RequestMethod.POST)
    public StudyRecordAmountRestSearchResults findStudyRecordAmounts(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody StudyRecordAmountSearchParameters searchParams) {
        StudyRecordAmountSearchResults results = studyRecordService.searchAmounts(SSL_CLIENT_S_DN_O, searchParams);
        return new StudyRecordAmountRestSearchResults(results.getTotalHits(), AggregationUtils.mapAggregates(AggregationUtils.toAggregateMap(results.getAggregations())));
    }

    @Operation(summary = "Hae opintosuoritusten lukumääriä CSV-muodossa", description = "Operaatio hakee opintosuoritusten lukumääriä CSV-muodossa annetuilla hakuehdoilla.", tags = "studyrecords")
    @RequestMapping(value = "/search/amounts/csv", method = RequestMethod.POST)
    public ResponseEntity<String> findStudyRecordAmountsAsCSV(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                                 @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody StudyRecordAmountSearchParameters searchParams) {
        StudyRecordAmountSearchResults results = studyRecordService.searchAmounts(SSL_CLIENT_S_DN_O, searchParams);

        String content;

        try {
            content = StudyRecordCsvConverter.convertStudyRecordAmountSearchResultsToCsv(results, searchParams);
        } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            logger.error("Error while generating study record CSV file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return generateAndReturnCsvContent(content, String.format("ripa_opintosuoritus_lukumaarat_%s.csv", LocalDate.now()));
    }

    private ResponseEntity<String> generateAndReturnCsvContent(String content, String filename) {
        if(!StringUtils.hasText(content)) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok()
            .header("Content-Type", "text/csv; charset=UTF-8")
            .header("Content-Disposition", String.format("attachment; filename=%s", filename))
            .body(content);
    }
}
