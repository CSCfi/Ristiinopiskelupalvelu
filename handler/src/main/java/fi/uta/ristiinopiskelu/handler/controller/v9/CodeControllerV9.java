package fi.uta.ristiinopiskelu.handler.controller.v9;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.code.CodeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchParameters;
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

@Tag(name = "codes", description = "Koodistot v9")
@RequestMapping("/api/v9/codes")
@RestController
public class CodeControllerV9 {

    @Autowired
    private CodeService codeService;

    @Autowired
    private ModelMapper modelMapper;

    @Operation(summary = "Hae kaikki koodistot", description = "Operaatio hakee koodistot.", tags = "codes")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public List<CodeReadDTO> findAll(@RequestParam(defaultValue = "0", required = false) Integer page, @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        return codeService.findAll(PageRequest.of(page, pageSize)).stream().map(code -> modelMapper.map(code, CodeReadDTO.class)).collect(Collectors.toList());
    }

    @Operation(summary = "Hae koodistoja", description = "Operaatio hakee koodistoja annetuilla hakuehdoilla.", tags = "codes")
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public List<CodeReadDTO> findStudyRecords(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                              @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody CodeSearchParameters searchParams) {
        return codeService.search(SSL_CLIENT_S_DN_O, searchParams).getResults().stream().map(code -> modelMapper.map(code, CodeReadDTO.class)).collect(Collectors.toList());
    }
}
