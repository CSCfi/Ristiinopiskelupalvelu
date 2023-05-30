package fi.uta.ristiinopiskelu.handler.controller.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.Code;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.code.CodeSearchParameters;
import fi.uta.ristiinopiskelu.handler.controller.AbstractController;
import fi.uta.ristiinopiskelu.handler.service.CodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "codes", description = "Koodistot v8")
@RequestMapping("/api/v8/codes")
@RestController
public class CodeControllerV8 extends AbstractController {

    @Autowired
    private CodeService codeService;

    @Autowired
    private ModelMapper modelMapper;

    @Operation(summary = "Hae kaikki koodistot", description = "Operaatio hakee koodistot.", tags = "codes")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public List<Code> findAll(@RequestParam(defaultValue = "0", required = false) Integer page, @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        return codeService.findAll(PageRequest.of(page, pageSize)).stream().map(code -> modelMapper.map(code, Code.class)).collect(Collectors.toList());
    }

    @Operation(summary = "Hae koodistoja", description = "Operaatio hakee koodistoja annetuilla hakuehdoilla.", tags = "codes")
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public List<Code> findStudyRecords(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                           @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody CodeSearchParameters searchParams) {
        fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchParameters mappedParams =
            super.mapToDto(searchParams, fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchParameters.class);
        CodeSearchResults results = codeService.search(SSL_CLIENT_S_DN_O, mappedParams);
        return super.mapSearchResultsToList(results, Code.class);
    }
}
