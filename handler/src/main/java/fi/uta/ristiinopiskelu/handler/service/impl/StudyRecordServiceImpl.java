package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyrecord.StudyRecordReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyrecord.StudyRecordWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.InvalidSearchParametersException;
import fi.uta.ristiinopiskelu.handler.service.StudyRecordService;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyRecordRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
public class StudyRecordServiceImpl extends AbstractService<StudyRecordWriteDTO, StudyRecordEntity, StudyRecordReadDTO> implements StudyRecordService {

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private JsonMapper jsonMapper;

    public StudyRecordServiceImpl() {
        super(StudyRecordWriteDTO.class, StudyRecordEntity.class, StudyRecordReadDTO.class);
    }

    @Override
    protected StudyRecordRepository getRepository() {
        return studyRecordRepository;
    }

    @Override
    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Override
    public StudyRecordSearchResults search(String organisationId, StudyRecordSearchParameters searchParams) throws FindFailedException, InvalidSearchParametersException {
        Assert.notNull(searchParams, "Search parameters cannot be null");

        validateOrganisationAndNetworkParameters(organisationId, searchParams.getSendingOrganisation(), searchParams.getReceivingOrganisation(),
            null);

        try {
            List<StudyRecordReadDTO> results = this.getRepository().findAllByParams(searchParams).stream()
                .map(this::toReadDTO)
                .toList();
            return new StudyRecordSearchResults(results);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public StudyRecordAmountSearchResults searchAmounts(String organisationId, StudyRecordAmountSearchParameters searchParams) throws FindFailedException, InvalidSearchParametersException {
        Assert.notNull(searchParams, "Search parameters cannot be null");

        validateOrganisationAndNetworkParameters(organisationId, searchParams.getSendingOrganisation(), searchParams.getReceivingOrganisation(),
            searchParams.getNetworkIdentifier());

        if(searchParams.getGroupBy() != null && searchParams.getGroupBy() == StudyRecordGrouping.DATES) {
            if(CollectionUtils.isEmpty(searchParams.getGroupByDates())) {
                throw new InvalidSearchParametersException("groupByDates parameter cannot be empty if groupBy = DATES");
            }

            searchParams.getGroupByDates().forEach(gbd -> {
                if(gbd.getStart() == null || gbd.getEnd() == null) {
                    throw new InvalidSearchParametersException("groupByDates parameter must have both start and end date specified");
                }
            });
        }

        try {
            SearchHits<StudyRecordEntity> response = studyRecordRepository.findAmounts(searchParams);
            return new StudyRecordAmountSearchResults(response.getTotalHits(), (ElasticsearchAggregations) response.getAggregations());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }
    
    private void validateOrganisationAndNetworkParameters(String organisationId, String sendingOrganisation, String receivingOrganisation,
                                                          @Nullable String networkIdentifier) {
        if(StringUtils.hasText(networkIdentifier)) {
            if(networkRepository.findNetworkByOrganisationIdAndNetworkId(organisationId, networkIdentifier).isEmpty()) {
                throw new InvalidSearchParametersException("Requesting organisation must belong to the specified cooperation network");
            }
        } else {
            if (!StringUtils.hasText(sendingOrganisation) && !StringUtils.hasText(receivingOrganisation)) {
                throw new InvalidSearchParametersException("Either 'sendingOrganisation' or 'receivingOrganisation' field must be specified");
            }

            if (StringUtils.hasText(sendingOrganisation) && !StringUtils.hasText(receivingOrganisation)) {
                if (!sendingOrganisation.equals(organisationId)) {
                    throw new InvalidSearchParametersException("Requesting organisation must match 'sendingOrganisation' field");
                }
            } else if (!StringUtils.hasText(sendingOrganisation) && StringUtils.hasText(receivingOrganisation)) {
                if (!receivingOrganisation.equals(organisationId)) {
                    throw new InvalidSearchParametersException("Requesting organisation must match 'receivingOrganisation' field");
                }
            } else if (!sendingOrganisation.equals(organisationId) && !receivingOrganisation.equals(organisationId)) {
                throw new InvalidSearchParametersException("Requesting organisation must match either 'sendingOrganisation' or 'receivingOrganisation' field");
            }
        }
    }
}
