package fi.uta.ristiinopiskelu.handler.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.organisation.OrganisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.organisation.OrganisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.persistence.repository.ExtendedRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class OrganisationServiceImpl extends AbstractService<OrganisationWriteDTO, OrganisationEntity, OrganisationReadDTO> implements OrganisationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganisationServiceImpl.class);

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    public OrganisationServiceImpl() {
        super(OrganisationWriteDTO.class, OrganisationEntity.class, OrganisationReadDTO.class);
    }

    @Override
    protected ExtendedRepository<OrganisationEntity, String> getRepository() {
        return organisationRepository;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    // organisation is allowed to be created with a custom id
    @Override
    protected boolean isValidateId() {
        return false;
    }

    @Override
    public List<OrganisationEntity> findAllById(List<String> ids) throws FindFailedException {
        try {
            return StreamSupport.stream(organisationRepository.findAllById(ids).spliterator(), false).collect(Collectors.toList());
        } catch(Exception e) {
            throw new FindFailedException(getEntityClass(), ids, e);
        }
    }
}
