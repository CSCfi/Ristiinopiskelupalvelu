package fi.uta.ristiinopiskelu.messaging.message.current;

import fi.uta.ristiinopiskelu.messaging.VersionedMessageType;
import fi.uta.ristiinopiskelu.messaging.message.Message;
import fi.uta.ristiinopiskelu.messaging.message.current.acknowledgement.Acknowledgement;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.CreateCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.DeleteCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.UpdateCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.network.CreateNetworkRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.network.UpdateNetworkRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.CompositeIdentifiedEntityModifiedNotification;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.NetworkCreatedNotification;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.NetworkUpdatedNotification;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.CreateRealisationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.DeleteRealisationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.UpdateRealisationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.*;
import fi.uta.ristiinopiskelu.messaging.message.current.student.*;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.CreateStudyModuleRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.DeleteStudyModuleRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.UpdateStudyModuleRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.*;

public enum MessageType implements VersionedMessageType {

    // Requests
    CREATE_COURSEUNIT_REQUEST(CreateCourseUnitRequest.class),
    DELETE_COURSEUNIT_REQUEST(DeleteCourseUnitRequest.class),
    UPDATE_COURSEUNIT_REQUEST(UpdateCourseUnitRequest.class),

    CREATE_STUDYMODULE_REQUEST(CreateStudyModuleRequest.class),
    DELETE_STUDYMODULE_REQUEST(DeleteStudyModuleRequest.class),
    UPDATE_STUDYMODULE_REQUEST(UpdateStudyModuleRequest.class),

    CREATE_REGISTRATION_REQUEST(CreateRegistrationRequest.class),
    FORWARDED_CREATE_REGISTRATION_REQUEST(ForwardedCreateRegistrationRequest.class),
    REGISTRATION_REPLY_REQUEST(RegistrationReplyRequest.class),
    FORWARDED_REGISTRATION_REPLY_REQUEST(ForwardedRegistrationReplyRequest.class),

    CREATE_NETWORK_REQUEST(CreateNetworkRequest.class),
    UPDATE_NETWORK_REQUEST(UpdateNetworkRequest.class),

    CREATE_REALISATION_REQUEST(CreateRealisationRequest.class),
    UPDATE_REALISATION_REQUEST(UpdateRealisationRequest.class),
    DELETE_REALISATION_REQUEST(DeleteRealisationRequest.class),

    CREATE_STUDYRECORD_REQUEST(CreateStudyRecordRequest.class),
    FORWARDED_CREATE_STUDYRECORD_REQUEST(ForwardedCreateStudyRecordRequest.class),
    STUDYRECORD_REPLY_REQUEST(StudyRecordReplyRequest.class),
    FORWARDED_STUDYRECORD_REPLY_REQUEST(ForwardedStudyRecordReplyRequest.class),

    UPDATE_STUDENT_REQUEST(UpdateStudentRequest.class),
    FORWARDED_UPDATE_STUDENT_REQUEST(ForwardedUpdateStudentRequest.class),
    UPDATE_STUDENT_REPLY_REQUEST(UpdateStudentReplyRequest.class),
    FORWARDED_UPDATE_STUDENT_REPLY_REQUEST(ForwardedUpdateStudentReplyRequest.class),

    UPDATE_STUDENT_STUDYRIGHT_REQUEST(UpdateStudentStudyRightRequest.class),
    FORWARDED_UPDATE_STUDENT_STUDYRIGHT_REQUEST(ForwardedUpdateStudentStudyRightRequest.class),
    UPDATE_STUDENT_STUDYRIGHT_REPLY_REQUEST(UpdateStudentStudyRightReplyRequest.class),
    FORWARDED_UPDATE_STUDENT_STUDYRIGHT_REPLY_REQUEST(ForwardedUpdateStudentStudyRightReplyRequest.class),

    // Notifications (note the special cases here)
    NETWORK_CREATED_NOTIFICATION(NetworkCreatedNotification.class),
    NETWORK_UPDATED_NOTIFICATION(NetworkUpdatedNotification.class),

    COURSEUNIT_CREATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class),
    COURSEUNIT_UPDATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class),
    COURSEUNIT_DELETED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class),

    STUDYMODULE_CREATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class),
    STUDYMODULE_UPDATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class),
    STUDYMODULE_DELETED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class),

    REALISATION_CREATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class),
    REALISATION_UPDATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class),
    REALISATION_DELETED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class),

    // Common
    DEFAULT_RESPONSE(DefaultResponse.class),
    REGISTRATION_RESPONSE(RegistrationResponse.class),
    STUDYRECORD_RESPONSE(StudyRecordResponse.class),
    STUDENT_RESPONSE(StudentResponse.class),
    JSON_VALIDATION_FAILED_RESPONSE(JsonValidationFailedResponse.class),
    AUTHENTICATION_FAILED_RESPONSE(DefaultResponse.class),
    ACKNOWLEDGEMENT(Acknowledgement.class);

    private Class<? extends Message> clazz;

    MessageType(Class<? extends Message> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends Message> getClazz() {
        return clazz;
    }

    public static MessageGroup getMessageGroup(MessageType messageType) {
        for (MessageGroup messageGroup : MessageGroup.values()) {
            if (messageGroup.getMessageTypeNames().contains(messageType.name())) {
                return messageGroup;
            }
        }

        throw new IllegalArgumentException("MessageType " + messageType + " does not belong to any MessageGroup");
    }
}
