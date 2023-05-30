package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;

public class StudyElementEntityNotFoundException extends EntityNotFoundException {

    public StudyElementEntityNotFoundException(String message) {
        super(message);
    }

    public <T extends StudyElementEntity> StudyElementEntityNotFoundException(Class<T> type, String id) {
        super(type, id);
    }

    public <T extends StudyElementEntity> StudyElementEntityNotFoundException(Class<T> type, String code, String organizingOrganisation) {
        super(type, code, organizingOrganisation);
    }

    public <T extends StudyElementEntity> StudyElementEntityNotFoundException(Class<T> type, String id, String code, String organizingOrganisation) {
        super(type, id, code, organizingOrganisation);
    }
}
