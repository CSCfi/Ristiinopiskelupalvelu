package fi.csc.ristiinopiskelu.admin.controller;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.virta.VirtaCode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.virta.VirtaCodeSet;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.virta.VirtaCodeValue;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSetKeyWithCodeCount;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LanguageEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeSetEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeValueEntity;
import fi.uta.ristiinopiskelu.persistence.repository.CodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequestMapping("/api/codes")
@RestController
public class CodesController {

    private static final Logger logger = LoggerFactory.getLogger(CodesController.class);

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @PreAuthorize("isAuthenticated() and hasAnyRole('ADMIN','SUPERUSER')")
    @RequestMapping(method = RequestMethod.GET)
    public List<CodeEntity> codes() {
        return StreamSupport.stream(codeRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    @PreAuthorize("isAuthenticated() and hasAnyRole('ADMIN','SUPERUSER')")
    @RequestMapping(value = "/codesets", method = RequestMethod.GET)
    public List<CodeSetKeyWithCodeCount> codesCodeSets() {
        return codeRepository.findCodeSetKeysWithCodeCount();
    }

    @PreAuthorize("isAuthenticated() and hasAnyRole('ADMIN','SUPERUSER')")
    @RequestMapping(value = "/codesets/{codeset}", method = RequestMethod.GET)
    public List<CodeEntity> codesWithCodeset(@PathVariable("codeset") String codeset) {
        try (Stream<CodeEntity> codes = codeRepository.findAllByCodeSetKeyOrderByCodeUri(codeset)) {
            return codes.toList();
        }
    }

    @PreAuthorize("isAuthenticated() and hasAnyRole('ADMIN','SUPERUSER')")
    @RequestMapping(value = "/code/{code}", method = RequestMethod.GET)
    public Optional<CodeEntity> codeWithId(@PathVariable("code") String code) {
        return codeRepository.findById(code);
    }

    @PreAuthorize("isAuthenticated() and hasAnyRole('SUPERUSER')")
    @RequestMapping(value = "/import", method = RequestMethod.POST)
    public void importCodes(@RequestParam("file") MultipartFile file) throws Exception {
        mapAndCreateAll(file.getInputStream());
    }

    @PreAuthorize("isAuthenticated() and hasAnyRole('SUPERUSER')")
    @RequestMapping(value = "/update", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateCodes(@RequestBody CodeEntity code) {
        code.setVersion(code.getVersion() != null ? code.getVersion() + 1 : 1);
        codeRepository.save(code);
    }

    @PreAuthorize("isAuthenticated() and hasAnyRole('SUPERUSER')")
    @RequestMapping(value = "/codesets/{codeset}", method = RequestMethod.PUT)
    public CodeEntity addEmptyCode(@PathVariable("codeset") String codeset) {
        CodeSetEntity cs = new CodeSetEntity();
        cs.setKey(codeset);
        cs.setVersion(1);

        CodeEntity ent = new CodeEntity();
        ent.setCodeVersion(1);
        ent.setKey("new");
        ent.setUpdateDate(LocalDate.now());
        ent.setCodeUri(String.format("%s_%s", codeset, ent.getKey()));
        ent.setCodeSet(cs);

        return codeRepository.save(ent);
    }

    @PreAuthorize("isAuthenticated() and hasAnyRole('SUPERUSER')")
    @RequestMapping(value = "/codesets/{codeset}", method = RequestMethod.DELETE)
    public void codesDelete(@PathVariable("codeset") String codeset) {
        codeRepository.deleteAll(codesWithCodeset(codeset));
    }

    @PreAuthorize("isAuthenticated() and hasAnyRole('SUPERUSER')")
    @RequestMapping(value = "/code/{code}", method = RequestMethod.DELETE)
    public void codeDelete(@PathVariable("code") String code) {
        codeRepository.deleteById(code);
    }

    private void mapAndCreateAll(InputStream codeset) {
        try{
            List<VirtaCode> myObjects = jsonMapper.readValue(codeset, new TypeReference<>(){});
            List<CodeEntity> entities = myObjects.stream().map(this::toCodeEntity).collect(Collectors.toList());
            codeRepository.saveAll(entities);
        } catch(JacksonException | NullPointerException e) {
            logger.error("Failed to read codes mapper : " + e.getMessage());
            throw e;
        }
    }

    private CodeEntity toCodeEntity(VirtaCode source) {
        CodeEntity target = new CodeEntity();
        target.setCodeUri(source.getKoodiUri());
        target.setResourceUri(source.getResourceUri());
        target.setCodeSet(toCodeSet(source.getKoodisto()));
        target.setKey(source.getKoodiArvo());
        target.setCodeVersion(source.getVersio());
        target.setUpdateDate(toLocalDate(source.getPaivitysPvm()));
        target.setValidityStartDate(toLocalDate(source.getVoimassaAlkuPvm()));
        target.setValidityEndDate(toLocalDate(source.getVoimassaLoppuPvm()));
        target.setStatus(source.getTila());
        target.setCodeValues(source.getMetadata() == null ? null : source.getMetadata().stream().map(this::toCodeValue).collect(Collectors.toList()));
        return target;
    }

    private CodeSetEntity toCodeSet(VirtaCodeSet source) {
        if (source == null) {
            return null;
        }
        CodeSetEntity target = new CodeSetEntity();
        target.setKey(source.getKoodistoUri());
        target.setVersion(source.getKoodistoVersio());
        target.setOrganisationOid(source.getOrganisaatioOid());
        return target;
    }

    private CodeValueEntity toCodeValue(VirtaCodeValue source) {
        CodeValueEntity target = new CodeValueEntity();
        target.setLanguage(LanguageEntity.fromValue(source.getKieli()));
        target.setDescription(source.getKuvaus());
        target.setAbbreviation(source.getLyhytNimi());
        target.setValue(source.getNimi());
        return target;
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }
}
