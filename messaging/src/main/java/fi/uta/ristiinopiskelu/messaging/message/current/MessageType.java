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
    CREATE_COURSEUNIT_REQUEST(CreateCourseUnitRequest.class, true, true),
    DELETE_COURSEUNIT_REQUEST(DeleteCourseUnitRequest.class, true, true),
    UPDATE_COURSEUNIT_REQUEST(UpdateCourseUnitRequest.class, true, true),

    CREATE_STUDYMODULE_REQUEST(CreateStudyModuleRequest.class, true, true),
    DELETE_STUDYMODULE_REQUEST(DeleteStudyModuleRequest.class, true, true),
    UPDATE_STUDYMODULE_REQUEST(UpdateStudyModuleRequest.class, true, true),

    CREATE_REGISTRATION_REQUEST(CreateRegistrationRequest.class, true, true),
    FORWARDED_CREATE_REGISTRATION_REQUEST(ForwardedCreateRegistrationRequest.class, true, false),
    REGISTRATION_REPLY_REQUEST(RegistrationReplyRequest.class, true, true),
    FORWARDED_REGISTRATION_REPLY_REQUEST(ForwardedRegistrationReplyRequest.class, true, false),

    CREATE_NETWORK_REQUEST(CreateNetworkRequest.class, false, true),
    UPDATE_NETWORK_REQUEST(UpdateNetworkRequest.class, false, true),

    CREATE_REALISATION_REQUEST(CreateRealisationRequest.class, true, true),
    UPDATE_REALISATION_REQUEST(UpdateRealisationRequest.class, true, true),
    DELETE_REALISATION_REQUEST(DeleteRealisationRequest.class, true, true),

    CREATE_STUDYRECORD_REQUEST(CreateStudyRecordRequest.class, true, true),
    FORWARDED_CREATE_STUDYRECORD_REQUEST(ForwardedCreateStudyRecordRequest.class, true, false),
    STUDYRECORD_REPLY_REQUEST(StudyRecordReplyRequest.class, true, true),
    FORWARDED_STUDYRECORD_REPLY_REQUEST(ForwardedStudyRecordReplyRequest.class, true, false),

    UPDATE_STUDENT_REQUEST(UpdateStudentRequest.class, true, true),
    FORWARDED_UPDATE_STUDENT_REQUEST(ForwardedUpdateStudentRequest.class, true, false),
    UPDATE_STUDENT_REPLY_REQUEST(UpdateStudentReplyRequest.class, true, true),
    FORWARDED_UPDATE_STUDENT_REPLY_REQUEST(ForwardedUpdateStudentReplyRequest.class, true, false),

    UPDATE_STUDENT_STUDYRIGHT_REQUEST(UpdateStudentStudyRightRequest.class, true, true),
    FORWARDED_UPDATE_STUDENT_STUDYRIGHT_REQUEST(ForwardedUpdateStudentStudyRightRequest.class, true, false),
    UPDATE_STUDENT_STUDYRIGHT_REPLY_REQUEST(UpdateStudentStudyRightReplyRequest.class, true, true),
    FORWARDED_UPDATE_STUDENT_STUDYRIGHT_REPLY_REQUEST(ForwardedUpdateStudentStudyRightReplyRequest.class, true, false),

    // Notifications (note the special cases here)
    NETWORK_CREATED_NOTIFICATION(NetworkCreatedNotification.class, true, false),
    NETWORK_UPDATED_NOTIFICATION(NetworkUpdatedNotification.class, true, false),

    COURSEUNIT_CREATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class, true, false),
    COURSEUNIT_UPDATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class, true, false),
    COURSEUNIT_DELETED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class, true, false),

    STUDYMODULE_CREATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class, true, false),
    STUDYMODULE_UPDATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class, true, false),
    STUDYMODULE_DELETED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class, true, false),

    REALISATION_CREATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class, true, false),
    REALISATION_UPDATED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class, true, false),
    REALISATION_DELETED_NOTIFICATION(CompositeIdentifiedEntityModifiedNotification.class, true, false),

    // Common
    DEFAULT_RESPONSE(DefaultResponse.class, true, false),
    REGISTRATION_RESPONSE(RegistrationResponse.class, true, false),
    STUDYRECORD_RESPONSE(StudyRecordResponse.class, true, false),
    STUDENT_RESPONSE(StudentResponse.class, true, false),
    JSON_VALIDATION_FAILED_RESPONSE(JsonValidationFailedResponse.class, true, false),
    AUTHENTICATION_FAILED_RESPONSE(DefaultResponse.class, true, false),
    ACKNOWLEDGEMENT(Acknowledgement.class, true, true);

    private Class<? extends Message> clazz;
    private boolean documented;
    private boolean consumer;

    MessageType(Class<? extends Message> clazz, boolean documented, boolean consumer) {
        this.clazz = clazz;
        this.documented = documented;
        this.consumer = consumer;
    }

    public Class<? extends Message> getClazz() {
        return clazz;
    }

    public boolean isDocumented() {
        return documented;
    }

    public boolean isConsumer() {
        return consumer;
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
