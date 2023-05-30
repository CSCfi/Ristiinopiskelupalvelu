package fi.uta.ristiinopiskelu.handler.controller.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studyrecord.StudyRecordAmountSearchParameters;
import fi.uta.ristiinopiskelu.handler.controller.AbstractController;
import fi.uta.ristiinopiskelu.handler.service.StudyRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "studyrecords", description = "Opintosuoritukset v8")
@RequestMapping("/api/v8/studyrecords")
@RestController
public class StudyRecordControllerV8 extends AbstractController {

    private static final Logger logger = LoggerFactory.getLogger(StudyRecordControllerV8.class);

    @Autowired
    private StudyRecordService studyRecordService;

    @Operation(summary = "Hae opintosuoritusten lukumääriä", description = "Operaatio hakee opintosuoritusten lukumääriä annetuilla hakuehdoilla.",
        tags = "studyrecords")
    @RequestMapping(value = "/search/amounts", method = RequestMethod.POST)
    public Long findStudyRecordAmounts(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody StudyRecordAmountSearchParameters searchParams) {

        fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordAmountSearchParameters currentSearchParams =
            super.mapToDto(searchParams, fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordAmountSearchParameters.class);

        return studyRecordService.searchAmounts(SSL_CLIENT_S_DN_O, currentSearchParams).getTotalHits();
    }
}
