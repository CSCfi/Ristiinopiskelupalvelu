package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class StudyElementEntityNotFoundException extends EntityNotFoundException {

    public StudyElementEntityNotFoundException(String message) {
        super(message, ErrorCode.STUDY_ELEMENT_ENTITY_NOT_FOUND);
    }

    public <T extends StudyElementEntity> StudyElementEntityNotFoundException(Class<T> type, String id) {
        super(type, id, ErrorCode.STUDY_ELEMENT_ENTITY_NOT_FOUND);
    }

    public <T extends StudyElementEntity> StudyElementEntityNotFoundException(Class<T> type, String code, String organizingOrganisation) {
        super(type, code, organizingOrganisation, ErrorCode.STUDY_ELEMENT_ENTITY_NOT_FOUND);
    }

    public <T extends StudyElementEntity> StudyElementEntityNotFoundException(Class<T> type, String id, String code, String organizingOrganisation) {
        super(type, id, code, organizingOrganisation, ErrorCode.STUDY_ELEMENT_ENTITY_NOT_FOUND);
    }
}
