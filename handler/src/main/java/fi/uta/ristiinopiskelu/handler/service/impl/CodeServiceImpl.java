package fi.uta.ristiinopiskelu.handler.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.code.CodeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.code.CodeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.service.CodeService;
import fi.uta.ristiinopiskelu.persistence.repository.CodeRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodeServiceImpl extends AbstractService<CodeWriteDTO, CodeEntity, CodeReadDTO> implements CodeService {

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    public CodeServiceImpl() {
        super(CodeWriteDTO.class, CodeEntity.class, CodeReadDTO.class);
    }

    @Override
    protected CodeRepository getRepository() {
        return codeRepository;
    }
    
    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    protected ModelMapper getModelMapper() {
        return modelMapper;
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

        NativeQuery builder = new NativeQueryBuilder()
                .withQuery(query.build()._toQuery())
                .withPageable(searchParameters.getPageRequest())
                .build();

        List<CodeReadDTO> results = codeRepository.search(builder.getQuery()).stream()
            .map(this::toReadDTO)
            .collect(Collectors.toList());

        return new CodeSearchResults(results);
    }
}
