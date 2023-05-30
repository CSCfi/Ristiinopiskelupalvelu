package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.code.CodeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.code.CodeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.service.CodeService;
import fi.uta.ristiinopiskelu.persistence.repository.CodeRepository;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
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

    public CodeServiceImpl() {
        super(CodeWriteDTO.class, CodeEntity.class, CodeReadDTO.class);
    }

    @Override
    protected CodeRepository getRepository() {
        return codeRepository;
    }

    @Override
    protected ModelMapper getModelMapper() {
        return modelMapper;
    }

    @Override
    public CodeSearchResults search(String organisationId, CodeSearchParameters searchParameters) throws FindFailedException {
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        if(!StringUtils.isEmpty(searchParameters.getCodeKey())) {
            query.must(QueryBuilders.matchQuery("key", searchParameters.getCodeKey()));
        }

        if(!StringUtils.isEmpty(searchParameters.getCodeSetKey())) {
            query.must(QueryBuilders.matchQuery("codeSet.key", searchParameters.getCodeSetKey()));
        }

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .withPageable(searchParameters.getPageRequest())
                .build();

        List<CodeReadDTO> results = codeRepository.search(builder).stream()
            .map(this::toReadDTO)
            .collect(Collectors.toList());

        return new CodeSearchResults(results);
    }
}
