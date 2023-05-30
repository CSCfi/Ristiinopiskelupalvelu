package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyrecord.StudyRecordReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyrecord.StudyRecordWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.InvalidSearchParametersException;
import fi.uta.ristiinopiskelu.handler.service.StudyRecordService;
import fi.uta.ristiinopiskelu.persistence.repository.StudyRecordRepository;
import org.elasticsearch.action.search.SearchResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudyRecordServiceImpl extends AbstractService<StudyRecordWriteDTO, StudyRecordEntity, StudyRecordReadDTO> implements StudyRecordService {

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    @Autowired
    private ModelMapper modelMapper;

    public StudyRecordServiceImpl() {
        super(StudyRecordWriteDTO.class, StudyRecordEntity.class, StudyRecordReadDTO.class);
    }

    @Override
    protected StudyRecordRepository getRepository() {
        return studyRecordRepository;
    }

    @Override
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    @Override
    public StudyRecordEntity create(StudyRecordEntity studyRecord) throws CreateFailedException {
        Assert.notNull(studyRecord, "StudyRecord cannot be null");
        try {
            return this.studyRecordRepository.create(studyRecord);
        } catch (Exception e) {
            throw new CreateFailedException(getEntityClass(), e);
        }
    }

    @Override
    public List<StudyRecordEntity> findAllByStudentOidOrStudentHomeEppn(String oid, String eppn, Pageable pageable) throws FindFailedException {
        try {
            return studyRecordRepository.findAllByStudentOidOrStudentHomeEppn(oid, eppn, pageable);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public StudyRecordSearchResults search(String organisationId, StudyRecordSearchParameters searchParams) throws FindFailedException, InvalidSearchParametersException {
        Assert.notNull(searchParams, "Search parameters cannot be null");

        validateSearchParameters(organisationId, searchParams.getSendingOrganisation(), searchParams.getReceivingOrganisation());

        try {
            List<StudyRecordReadDTO> results = this.getRepository().findAllByParams(searchParams).stream().map(this::toReadDTO).collect(Collectors.toList());
            return new StudyRecordSearchResults(results);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public StudyRecordAmountSearchResults searchAmounts(String organisationId, StudyRecordAmountSearchParameters searchParams) throws FindFailedException, InvalidSearchParametersException {
        Assert.notNull(searchParams, "Search parameters cannot be null");

        validateSearchParameters(organisationId, searchParams.getSendingOrganisation(), searchParams.getReceivingOrganisation());

        if((searchParams.getGroupBy() != null && searchParams.getGroupBy() == StudyRecordGrouping.DATES) &&
            CollectionUtils.isEmpty(searchParams.getGroupByDates())) {
            throw new InvalidSearchParametersException("groupByDates parameter cannot be empty if groupBy = DATES");
        }

        try {
            SearchResponse response = studyRecordRepository.findAmounts(searchParams);
            return new StudyRecordAmountSearchResults(response.getHits().getTotalHits().value, response.getAggregations());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }
    
    private void validateSearchParameters(String organisationId, String sendingOrganisation, String receivingOrganisation) {
        if(!StringUtils.hasText(sendingOrganisation) && !StringUtils.hasText(receivingOrganisation)) {
            throw new InvalidSearchParametersException("Either 'sendingOrganisation' or 'receivingOrganisation' field must be specified");
        }

        if(StringUtils.hasText(sendingOrganisation) && !StringUtils.hasText(receivingOrganisation)) {
            if(!sendingOrganisation.equals(organisationId)) {
                throw new InvalidSearchParametersException("Requesting organisation must match 'sendingOrganisation' field");
            }
        } else if(!StringUtils.hasText(sendingOrganisation) && StringUtils.hasText(receivingOrganisation)) {
            if(!receivingOrganisation.equals(organisationId)) {
                throw new InvalidSearchParametersException("Requesting organisation must match 'receivingOrganisation' field");
            }
        } else if(!sendingOrganisation.equals(organisationId) && !receivingOrganisation.equals(organisationId)) {
            throw new InvalidSearchParametersException("Requesting organisation must match either 'sendingOrganisation' or 'receivingOrganisation' field");
        }
    }
}
