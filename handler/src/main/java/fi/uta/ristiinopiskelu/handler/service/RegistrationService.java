package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.registration.RegistrationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.registration.RegistrationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface RegistrationService extends Service<RegistrationWriteDTO, RegistrationEntity, RegistrationReadDTO> {

    List<RegistrationEntity> findByStudentAndSelectionsReplies(StudyRecordStudent student, String selectionItemId,
                                                               String selectionItemOrganisation, String selectionItemType) throws FindFailedException;

    List<RegistrationEntity> findByStudentAndSelections(StudyRecordStudent student, String selectionItemId,
                                                               String selectionItemOrganisation, String selectionItemType) throws FindFailedException;
    
    Map<OrganisationEntity, List<RegistrationEntity>> findAllStudentRegistrationsPerOrganisation(String organisationId, String personId, String personOid, Pageable pageable) throws FindFailedException;

    Map<OrganisationEntity, List<RegistrationEntity>> findAllRegistrationsWithValidStudyRightPerOrganisation(String studyRightId, String organisation) throws FindFailedException;

    RegistrationSearchResults search(String organisationId, RegistrationSearchParameters searchParameters) throws FindFailedException;

    RegistrationStatusAmountSearchResults searchStatusAmounts(String ssl_client_s_dn_o, RegistrationStatusAmountSearchParameters searchParams);

    RegistrationAmountSearchResults searchAmounts(String ssl_client_s_dn_o, RegistrationAmountSearchParameters searchParams);
}
