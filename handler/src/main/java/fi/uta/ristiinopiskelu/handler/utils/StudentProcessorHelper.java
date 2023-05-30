package fi.uta.ristiinopiskelu.handler.utils;

import fi.uta.ristiinopiskelu.messaging.message.current.student.StudentReplyRequest;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.UpdateStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *   Helper class for common methods for Student processors
 */
public class StudentProcessorHelper {

    public static List<UpdateStatus> getUpdateStatuses(StudentEntity entity, String organisationId, StudentReplyRequest request) {
        List<UpdateStatus> updateStatuses = new ArrayList<>();

        if(!CollectionUtils.isEmpty(entity.getStatuses())) {
            updateStatuses.addAll(entity.getStatuses());
            updateStatuses.removeIf(us -> us.getOrganisationId().equals(organisationId));
        }
        updateStatuses.add(new UpdateStatus(organisationId, request.getStatus(), request.getRejectionReason()));
        return updateStatuses;
    }
}
