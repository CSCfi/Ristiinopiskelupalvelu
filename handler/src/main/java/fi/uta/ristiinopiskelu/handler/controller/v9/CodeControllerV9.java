package fi.uta.ristiinopiskelu.handler.controller.v9;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.code.CodeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchParameters;
import fi.uta.ristiinopiskelu.handler.service.CodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "codes", description = "Koodistot v9")
@RequestMapping("/api/v9/codes")
@RestController
public class CodeControllerV9 {

    @Autowired
    private CodeService codeService;

    @Operation(summary = "Hae kaikki koodistot", description = "Operaatio hakee koodistot.", tags = "codes")
    @GetMapping
    public List<CodeReadDTO> findAll(@RequestParam(defaultValue = "0", required = false) Integer page,
                                     @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        return codeService.findAll(PageRequest.of(page, pageSize)).stream()
            .map(CodeReadDTO::from)
            .toList();
    }

    @Operation(summary = "Hae koodistoja", description = "Operaatio hakee koodistoja annetuilla hakuehdoilla.", tags = "codes")
    @PostMapping(value = "/search")
    public List<CodeReadDTO> findStudyRecords(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                  @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                              @RequestBody CodeSearchParameters searchParams) {
        return codeService.search(SSL_CLIENT_S_DN_O, searchParams).getResults();
    }
}
