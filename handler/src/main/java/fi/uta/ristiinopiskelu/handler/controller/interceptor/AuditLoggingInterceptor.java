package fi.uta.ristiinopiskelu.handler.controller.interceptor;

import com.google.common.base.Joiner;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static net.logstash.logback.marker.Markers.append;

public class AuditLoggingInterceptor extends HandlerInterceptorAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!request.getRequestURI().contains("/api/") || request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }

        String eppn = request.getHeader(MessageHeader.EPPN);
        Assert.hasText(eppn, "End user eduPersonPrincipalName missing, must be supplied in header with key '" + MessageHeader.EPPN + "'");

        String organisationId = request.getHeader(MessageHeader.ORGANISATION_ID);
        Assert.hasText(organisationId, "Organisation id missing, unauthorized organisation/misconfiguration?");

        Map<String, String> requestParamsMap = new HashMap<>();
        if(!CollectionUtils.isEmpty(request.getParameterMap())) {
            requestParamsMap.putAll(request.getParameterMap().entrySet().stream().collect(
                    Collectors.toMap(e -> e.getKey(), e -> (e.getValue() != null && e.getValue().length > 0) ?
                            StringUtils.arrayToDelimitedString(e.getValue(), String.format(", %s=", e.getKey())) : "")));
        }

        String requestURI = request.getRequestURI();
        String requestParams = Joiner.on(", ").withKeyValueSeparator("=").join(requestParamsMap);
        String method = request.getMethod();
        String remoteHost = request.getRemoteHost();

        logger.info(
                append("eppn", eppn)
                        .and(append("organisationId", organisationId))
                        .and(append("requestURI", requestURI))
                        .and(append("requestParams", requestParams))
                        .and(append("method", method))
                        .and(append("remoteHost", remoteHost)),
                "User '{}' at organisation '{}' requested resource '{}' with params '{}'", eppn, organisationId, requestURI, requestParams);

        return true;
    }
}
