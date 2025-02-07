package fi.csc.ristiinopiskelu.admin.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import static net.logstash.logback.marker.Markers.append;

public class ShibbolethAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static Logger logger = LoggerFactory.getLogger(ShibbolethAuthenticationFailureHandler.class);
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        String remoteHost = request.getRemoteHost();

        logger.info(
                append("remoteHost", remoteHost),
                "Failed login attempt from '{}'", remoteHost);
    }
}
