package fi.uta.ristiinopiskelu.persistence.exception;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper for UncategorizedElasticsearchException that produces a bit more unambiguous exception message
 */
public class VerboseUncategorizedElasticsearchException extends RuntimeException {

    private final UncategorizedElasticsearchException e;

    public VerboseUncategorizedElasticsearchException(UncategorizedElasticsearchException e) {
        this.e = e;
    }

    @Override
    public String getMessage() {
        ElasticsearchException cause = (ElasticsearchException) getCause();

        if(cause == null) {
            return e.getMessage();
        }

        if(cause.error() == null) {
            return "%s [status=%s]".formatted(e.getMessage(), cause.status());
        }

        // we don't want to print type and reason again here, it's already in the message
        Map<String, String> errorValueMap = errorCauseToValueMap(cause.error());
        errorValueMap.remove("type");
        errorValueMap.remove("reason");

        return String.format("%s [status=%s, %s]".formatted(e.getMessage(), cause.status(), valueMapToString(errorValueMap)));
    }
    
    private List<String> errorCausesToString(List<ErrorCause> errorCauses) {
        if(CollectionUtils.isEmpty(errorCauses)) {
            return Collections.emptyList();
        }

        return errorCauses.stream()
            .map(this::errorCauseToString)
            .filter(Objects::nonNull)
            .toList();
    }

    private String errorCauseToString(ErrorCause errorCause) {
        if(errorCause == null) {
            return null;
        }

        return valueMapToString(errorCauseToValueMap(errorCause));
    }

    private String valueMapToString(Map<String, String> values) {
        return values.entrySet().stream()
            .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(", "));
    }

    private Map<String, String> errorCauseToValueMap(ErrorCause errorCause) {
        if(errorCause == null) {
            return Collections.emptyMap();
        }

        Map<String, String> values = new HashMap<>();

        if(StringUtils.hasText(errorCause.type())) {
            values.put("type", "\"%s\"".formatted(errorCause.type()));
        }

        if(StringUtils.hasText(errorCause.reason())) {
            values.put("reason", "\"%s\"".formatted(errorCause.reason()));
        }

        if(!CollectionUtils.isEmpty(errorCause.metadata())) {
            values.put("metadata", "{%s}".formatted(errorCause.metadata().entrySet().stream()
                    .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining(", "))));
        }

        if(errorCause.causedBy() != null) {
            values.put("causedBy", "{%s}".formatted(errorCauseToString(errorCause.causedBy())));
        }

        if(!CollectionUtils.isEmpty(errorCause.rootCause())) {
            values.put("rootCause", "[%s]".formatted(StringUtils.collectionToDelimitedString(
                errorCausesToString(errorCause.rootCause()), ", ", "{", "}")));
        }

        if(!CollectionUtils.isEmpty(errorCause.suppressed())) {
            values.put("suppressed", "[%s]".formatted(StringUtils.collectionToDelimitedString(
                errorCausesToString(errorCause.suppressed()), ", ", "{", "}")));
        }

        return values;
    }

    @Override
    public synchronized Throwable getCause() {
        return e.getCause();
    }
}
