package fi.csc.ristiinopiskelu.admin.controller.interceptor;

import com.google.common.base.Joiner;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import static net.logstash.logback.marker.Markers.append;

public class AuditLoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        ShibbolethUserDetails details = null;

        if(authentication != null && authentication.getPrincipal() != null && authentication.getPrincipal() instanceof ShibbolethUserDetails shibbolethUserDetails) {
            details = shibbolethUserDetails;
        }

        String eppn = details != null ? details.getEppn() : null;
        String requestURI = request.getRequestURI();
        String requestParams = CollectionUtils.isEmpty(request.getParameterMap()) ? Joiner.on(",").withKeyValueSeparator("=").join(request.getParameterMap()) : null;
        String method = request.getMethod();
        String remoteHost = request.getRemoteHost();

        logger.info(
                append("eppn", eppn)
                        .and(append("requestURI", requestURI))
                        .and(append("requestParams", requestParams))
                        .and(append("method", method))
                        .and(append("remoteHost", remoteHost)),
                "User '{}' requested resource '{}' with params '{}'", eppn, requestURI, requestParams);

        return true;
    }
}
