package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.registration.RegistrationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.registration.RegistrationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studyrecord.SearchDirection;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RegistrationRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Service
public class RegistrationServiceImpl extends AbstractService<RegistrationWriteDTO, RegistrationEntity, RegistrationReadDTO> implements RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private ModelMapper modelMapper;

    public RegistrationServiceImpl() {
        super(RegistrationWriteDTO.class, RegistrationEntity.class, RegistrationReadDTO.class);
    }

    @Override
    protected RegistrationRepository getRepository() {
        return registrationRepository;
    }

    @Override
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    @Override
    public List<RegistrationEntity> findByStudentAndSelectionsReplies(StudyRecordStudent student, String selectionItemId,
                                                                      String selectionItemOrganisation, String selectionItemType) throws FindFailedException {
        try {
            return registrationRepository.findAllByStudentAndSelectionsReplies(student, selectionItemId, selectionItemOrganisation, selectionItemType);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public List<RegistrationEntity> findByStudentAndSelections(StudyRecordStudent student, String selectionItemId,
                                                               String selectionItemOrganisation, String selectionItemType) throws FindFailedException {
        try {
            return registrationRepository.findAllByStudentAndSelections(student, selectionItemId, selectionItemOrganisation, selectionItemType);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public Map<OrganisationEntity, List<RegistrationEntity>> findAllRegistrationsWithValidStudyRightPerOrganisation(String studyRightId, String organisation) throws FindFailedException {
        try {
            // find all registrations for this study right
            List<RegistrationEntity> registrations = registrationRepository.findByStudentHomeStudyRightIdentifiersStudyRightIdAndStudentHomeStudyRightIdentifiersOrganisationTkCodeReference(
                studyRightId, organisation).orElse(Collections.emptyList());

            return getRegistrationsPerOrganisation(registrations);

        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public Map<OrganisationEntity, List<RegistrationEntity>> findAllStudentRegistrationsPerOrganisation(String organisationId, String personId, String personOid, Pageable pageable) throws FindFailedException {
        try {
            List<RegistrationEntity> registrations = getRepository().findAllBySendingOrganisationTkCodeAndStudentPersonIdOrStudentOid(
                organisationId, personId, personOid, pageable);

            return getRegistrationsPerOrganisation(registrations);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    private Map<OrganisationEntity, List<RegistrationEntity>> getRegistrationsPerOrganisation(List<RegistrationEntity> registrations) {
        // get the queues of all of the organisations that should be aware of the student
        List<String> receivingOrganisationIds = registrations.stream().map(RegistrationEntity::getReceivingOrganisationTkCode).distinct().collect(Collectors.toList());

        if(CollectionUtils.isEmpty(receivingOrganisationIds)) {
            return new HashMap<>();
        }

        List<OrganisationEntity> organisationEntities = StreamSupport.stream(organisationRepository.findAllById(receivingOrganisationIds).spliterator(), false)
            .collect(Collectors.toList());

        Map<String, List<RegistrationEntity>> groupedRegistrations = registrations.stream().collect(groupingBy(RegistrationEntity::getReceivingOrganisationTkCode));

        Map<OrganisationEntity, List<RegistrationEntity>> organisationsAndRegistrations = new HashMap<>();
        for(Map.Entry<String, List<RegistrationEntity>> entry : groupedRegistrations.entrySet()) {
            String organisationId = entry.getKey();
            List<RegistrationEntity> organisationRegistrations = entry.getValue();

            organisationEntities.stream().filter(org -> org.getId().equals(organisationId)).findFirst()
                .ifPresent(o -> organisationsAndRegistrations.put(o, organisationRegistrations));
        }
        return organisationsAndRegistrations;
    }

    @Override
    public RegistrationEntity create(RegistrationEntity registration) throws CreateFailedException {
        Assert.notNull(registration, "Registration cannot be null");
        try {
            return this.registrationRepository.create(registration);
        } catch(Exception e) {
            throw new CreateFailedException(getEntityClass(), e);
        }
    }

    @Override
    public RegistrationSearchResults search(String organisationId, RegistrationSearchParameters searchParameters) throws FindFailedException {
        Assert.notNull(searchParameters, "Search parameters cannot be null");

        List<String> networkIdentifiers = searchParameters.getNetworkIdentifiers();

        // filter out all but the actually valid network identifiers from the search params
        List<NetworkEntity> validSearchNetworks = networkRepository.findAllNetworksByOrganisationId(organisationId, Pageable.unpaged())
                .stream()
                .filter(network -> !CollectionUtils.isEmpty(networkIdentifiers) && networkIdentifiers.contains(network.getId()))
                .collect(Collectors.toList());

        // now find out which networks this organisation is actually a coordinator in and then get all organisation ids from those networks
        List<String> organisationIds = validSearchNetworks.stream()
                .filter(network -> network.getOrganisations().stream().anyMatch(org -> org.getOrganisationTkCode().equals(organisationId) && org.getIsCoordinator()))
                .flatMap(network -> network.getOrganisations().stream())
                .map(NetworkOrganisation::getOrganisationTkCode)
                .collect(Collectors.toList());
        
        // not a coordinator in any network. limit to own.
        if(CollectionUtils.isEmpty(organisationIds)) {
            organisationIds.add(organisationId);
        }

        try {
            List<RegistrationReadDTO> results = this.getRepository().findAllByParams(searchParameters.getStudentOid(),
                    searchParameters.getStudentPersonId(), searchParameters.getStudentHomeEppn(),
                    organisationIds, searchParameters.getSendDateTimeStart(),
                    searchParameters.getSendDateTimeEnd(), searchParameters.getRegistrationStatus(),
                    searchParameters.getPageRequest())
                .stream()
                .map(this::toReadDTO)
                .collect(Collectors.toList());

            return new RegistrationSearchResults(results);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public RegistrationAmountSearchResults searchAmounts(String organisationId, RegistrationAmountSearchParameters searchParams) {

        List<String> networkIdentifiers = searchParams.getNetworkIdentifiers();

        // filter out all but the actually valid network identifiers from the search params
        List<NetworkEntity> validSearchNetworks = networkRepository.findAllNetworksByOrganisationId(organisationId, Pageable.unpaged())
                .stream().filter(network -> !CollectionUtils.isEmpty(networkIdentifiers) && networkIdentifiers.contains(network.getId())).collect(Collectors.toList());

        List<String> organisationIds = validSearchNetworks.stream()
                .filter(network -> network.getOrganisations().stream().anyMatch(org -> org.getOrganisationTkCode().equals(organisationId)))
                .flatMap(network -> network.getOrganisations().stream())
                .map(NetworkOrganisation::getOrganisationTkCode)
                .collect(Collectors.toList());
        
        List<RegistrationEntity> registrations = this.getRepository().findAllByParams(null, null, null,
                organisationIds, null, null, null, Pageable.unpaged())
                .stream().collect(Collectors.toList());

        List<RegistrationSelectionIdentifier> distinctSelections = registrations.stream().flatMap(reg -> reg.getSelections().stream()
                .map(sel -> new RegistrationSelectionIdentifier(reg.getReceivingOrganisationTkCode(), sel)))
                .distinct().collect(Collectors.toList());

        if(searchParams.getType() != null) {
            distinctSelections = distinctSelections.stream().filter(ds -> ds.getType() == searchParams.getType()).collect(Collectors.toList());
        }

        List<String> sendingOrganisationTkCodes = registrations.stream().map(RegistrationEntity::getSendingOrganisationTkCode).distinct().collect(Collectors.toList());

        Map<RegistrationSelectionIdentifier, Map<String, Integer>> result = new HashMap<>();

        for(RegistrationSelectionIdentifier id : distinctSelections) {
            Map<String, Integer> amounts = new HashMap<>();
            for(String sendingOrganisationTkCode : sendingOrganisationTkCodes) {
                amounts.put(sendingOrganisationTkCode, findRegsFor(registrations, id, sendingOrganisationTkCode).size());
            }
            result.put(id, amounts);
        }

        return new RegistrationAmountSearchResults(result.entrySet().stream()
                .map(r -> new RegistrationAmountSearchResult(r.getKey(), r.getValue())).collect(Collectors.toList()));
    }

    private List<RegistrationEntity> findRegsFor(List<RegistrationEntity> regs, RegistrationSelectionIdentifier id, String sendingOrganisationTkCode) {
        return regs.stream()
                .filter(reg -> reg.getSelections().stream().
                    anyMatch(sel -> sel.getSelectionItemId().equals(id.getId())
                            && sel.getSelectionItemType() == id.getType()
                            && reg.getReceivingOrganisationTkCode().equals(id.getOrganisationId())))
                .filter(reg -> reg.getSendingOrganisationTkCode().equals(sendingOrganisationTkCode))
                .collect(Collectors.toList());
    }
    
    @Override
    public RegistrationStatusAmountSearchResults searchStatusAmounts(String organisationId, RegistrationStatusAmountSearchParameters searchParams) {

        List<String> networkIdentifiers = searchParams.getNetworkIdentifiers();

        // filter out all but the actually valid network identifiers from the search params
        List<NetworkEntity> validSearchNetworks = networkRepository.findAllNetworksByOrganisationId(organisationId, Pageable.unpaged())
                .stream().filter(network -> !CollectionUtils.isEmpty(networkIdentifiers) && networkIdentifiers.contains(network.getId())).collect(Collectors.toList());

        List<String> organisationIds = validSearchNetworks
                .stream().filter(network -> network.getOrganisations().stream().anyMatch(org -> org.getOrganisationTkCode().equals(organisationId)))
                .flatMap(network -> network.getOrganisations().stream())
                .map(NetworkOrganisation::getOrganisationTkCode)
                .collect(Collectors.toList());

        List<RegistrationReadDTO> registrations = this.getRepository().findAllByParams(null, null, null,
                organisationIds, null, null, null, Pageable.unpaged())
                .stream().map(r -> getModelMapper().map(r, RegistrationReadDTO.class)).collect(Collectors.toList());

        return new RegistrationStatusAmountSearchResults(
                groupByOrganisationTkCodeAndStatus(registrations, SearchDirection.INCOMING),
                groupByOrganisationTkCodeAndStatus(registrations, SearchDirection.OUTGOING));

    }

    private Map<String, Map<RegistrationSelectionItemStatus, Long>> groupByOrganisationTkCodeAndStatus(List<RegistrationReadDTO> registrations, SearchDirection direction) {
        Map<String, List<RegistrationReadDTO>> registrationsByOrganisation = registrations.stream().collect(
                Collectors.groupingBy(registration -> direction == SearchDirection.OUTGOING ? registration.getSendingOrganisationTkCode() : registration.getReceivingOrganisationTkCode()));

        return registrationsByOrganisation.entrySet().stream().collect(Collectors.toMap(value -> value.getKey(), value -> {
            List<RegistrationSelection> registrationSelections = value.getValue().stream().flatMap(reg -> reg.getSelections().stream()).collect(Collectors.toList());
            Map<RegistrationSelectionItemStatus, List<RegistrationSelection>> selectionsByStatus =
                    registrationSelections.stream().collect(Collectors.groupingBy(RegistrationSelection::getSelectionItemStatus));
            return selectionsByStatus.entrySet().stream().collect(Collectors.toMap(val -> val.getKey(),
                    val -> val.getValue().stream().filter(rs -> rs.getSelectionItemStatus() == val.getKey()).count()));
        }));
    }
}
