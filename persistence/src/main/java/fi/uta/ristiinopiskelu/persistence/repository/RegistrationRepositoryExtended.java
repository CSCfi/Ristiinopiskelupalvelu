package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

public interface RegistrationRepositoryExtended {

    List<RegistrationEntity> findAllByStudentAndSelectionsReplies(StudyRecordStudent student, String selectionItemId,
                                                                  String organizingOrganisationId, String selectionItemType);

    List<RegistrationEntity> findAllByStudentAndSelections(StudyRecordStudent student, String selectionItemId,
                                                           String organizingOrganisationId, String selectionItemType);

    List<RegistrationEntity> findAllByParams(String studentOid,
                                             String studentId,
                                             String homeEppn,
                                             List<String> networkSearchParams,
                                             OffsetDateTime sendDateTimeStart,
                                             OffsetDateTime sendDateTimeEnd,
                                             RegistrationStatus status,
                                             Pageable pageable);

    List<RegistrationEntity> findAllBySendingOrganisationTkCodeAndStudentPersonIdOrStudentOid(
        String sendingOrganisationTkCode, String studentPersonId, String studentOid, Pageable pageable);
}
