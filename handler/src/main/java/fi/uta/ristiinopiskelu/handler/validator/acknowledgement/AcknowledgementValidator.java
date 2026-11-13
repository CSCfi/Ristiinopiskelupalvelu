package fi.uta.ristiinopiskelu.handler.validator.acknowledgement;

import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import fi.uta.ristiinopiskelu.handler.validator.RequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.acknowledgement.Acknowledgement;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
public class AcknowledgementValidator implements RequestValidator<Acknowledgement> {

    public AcknowledgementValidator () {
    }

    @Override
    public void validateRequest(Acknowledgement acknowledgement, String organisationId) throws ValidationException {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException(MessageHeader.JMS_XUSERID);
        }
        if(StringUtils.isEmpty(acknowledgement.getReceivingOrganisationTkCode())) {
            throw new InvalidMessageBodyException("Receiced acknowledgement did not not have receivingOrganizationTKCOde");
        }
    }
}
