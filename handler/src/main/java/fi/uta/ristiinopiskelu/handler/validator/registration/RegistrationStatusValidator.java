package fi.uta.ristiinopiskelu.handler.validator.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationReplySelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationSelectionItemTypeEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.validator.RequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationReplyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RegistrationStatusValidator extends AbstractRegistrationValidator implements RequestValidator<RegistrationReplyRequest> {

    @Autowired
    private RegistrationService registrationService;

    @Override
    public void validateRequest(RegistrationReplyRequest req, String organisationId) {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException(MessageHeader.JMS_XUSERID);
        }

        if(StringUtils.isEmpty(req.getRegistrationRequestId())) {
            throw new InvalidMessageBodyException("Received registration reply request -message without registration id.");
        }

        RegistrationEntity registrationEntity = registrationService.findById(req.getRegistrationRequestId())
                .orElseThrow(() -> new EntityNotFoundException(RegistrationEntity.class, req.getRegistrationRequestId()));

        // no need to validate further if status is RECEIVED
        if(req.getStatus() == RegistrationStatus.RECEIVED) {
            return;
        }

        for(RegistrationReplySelection replySelection : req.getSelections()) {
            boolean selectionExists = registrationEntity.getSelections().stream()
                    .anyMatch(sel -> sel.getSelectionItemId().equals(replySelection.getSelectionItemId())
                        && sel.getSelectionItemType() == RegistrationSelectionItemTypeEntity.from(replySelection.getSelectionItemType()));
            if(!selectionExists) {
                throw new RegistrationSelectionDoesNotExistValidationException("Unable to process registration reply request." +
                        " Reply selection " + getSelectionIdString(replySelection) +
                        " does not exists for registration " + req.getRegistrationRequestId());
            }
            if (req.getStatus() == RegistrationStatus.REGISTERED &&
                replySelection.getSelectionItemStatus() == RegistrationSelectionItemStatus.ACCEPTED && (
                !StringUtils.hasText(req.getHostStudentNumber()) ||
                req.getHostStudyRight() == null
                )) {
                throw new RegistrationStatusValidationException("Unable to process registration reply request." +
                    " If registration status is REGISTERED and the status of any SelectionItem is ACCEPTED" +
                    " then hostStudentNumber and hostStudyRight are mandatory.");
            }
        }
    }
}
