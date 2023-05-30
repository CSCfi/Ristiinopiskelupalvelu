package fi.uta.ristiinopiskelu.handler.validator.acknowledgement;

import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.validator.RequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.acknowledgement.Acknowledgement;
import org.modelmapper.ModelMapper;
import org.modelmapper.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.Validator;


@Component
public class AcknowledgementValidator implements RequestValidator<Acknowledgement> {

    @Autowired
    public AcknowledgementValidator () {
    }

    @Override
    public void validateRequest(Acknowledgement acknowledgement, String organisationId) throws ValidationException {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException("Cannot perform create. Organisation Id is missing from header. This should not happen.");
        }
        if(StringUtils.isEmpty(acknowledgement.getReceivingOrganisationTkCode())) {
            throw new InvalidMessageBodyException("Receiced acknowledgement did not not have receivingOrganizationTKCOde");
        }
    }
}
