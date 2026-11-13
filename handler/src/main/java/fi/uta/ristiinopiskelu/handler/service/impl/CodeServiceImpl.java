package fi.uta.ristiinopiskelu.handler.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.code.CodeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.code.CodeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.service.CodeService;
import fi.uta.ristiinopiskelu.persistence.repository.CodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodeServiceImpl extends AbstractService<CodeWriteDTO, CodeEntity, CodeReadDTO> implements CodeService {

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private JsonMapper jsonMapper;

    public CodeServiceImpl() {
        super(CodeWriteDTO.class, CodeEntity.class, CodeReadDTO.class);
    }

    @Override
    protected CodeRepository getRepository() {
        return codeRepository;
    }
    
    @Override
    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Override
    public CodeSearchResults search(String organisationId, CodeSearchParameters searchParameters) throws FindFailedException {
        BoolQuery.Builder query = new BoolQuery.Builder();

        if(!StringUtils.isEmpty(searchParameters.getCodeKey())) {
            query.must(q -> q.match(mq -> mq.field("key").query(searchParameters.getCodeKey())));
        }

        if(!StringUtils.isEmpty(searchParameters.getCodeSetKey())) {
            query.must(q -> q.match(mq -> mq.field("codeSet.key").query(searchParameters.getCodeSetKey())));
        }

        List<CodeReadDTO> results = codeRepository.search(query.build()._toQuery(), searchParameters.getPageRequest()).stream()
            .map(this::toReadDTO)
            .collect(Collectors.toList());

        return new CodeSearchResults(results);
    }
}
