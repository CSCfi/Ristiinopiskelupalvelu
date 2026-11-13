package fi.uta.ristiinopiskelu.messaging.message;

import lombok.Getter;

public enum ErrorCodeCategory {
    /**
     * Authentication and authorization failures before the message is routed to a message-specific processor.
     */
    AUTHENTICATION(1000, 1099),

    /**
     * Missing, malformed, or otherwise invalid AMQ/JMS message headers.
     */
    MESSAGE_HEADER(1100, 1199),

    /**
     * Invalid message payloads, including JSON schema validation, body validation, and parsing failures.
     */
    MESSAGE_BODY(1200, 1299),

    /**
     * Unsupported or disallowed protocol-level message metadata, such as schema versions and message types.
     */
    MESSAGE_PROTOCOL(1300, 1399),

    /**
     * Business and domain validation failures after the message contract itself is valid.
     */
    VALIDATION(2000, 2999),

    /**
     * Failures while forwarding or relaying messages to other organisations or queues.
     */
    MESSAGING(3000, 3999),

    /**
     * Service and persistence layer failures while creating, updating, deleting, finding, or converting entities.
     */
    SERVICE(4000, 4999),

    /**
     * Unexpected internal failures that should not expose implementation details to clients.
     */
    INTERNAL(9000, 9999);

    @Getter
    private final int minCode;

    @Getter
    private final int maxCode;

    ErrorCodeCategory(int minCode, int maxCode) {
        this.minCode = minCode;
        this.maxCode = maxCode;
    }

    public boolean contains(int code) {
        return code >= minCode && code <= maxCode;
    }
}
