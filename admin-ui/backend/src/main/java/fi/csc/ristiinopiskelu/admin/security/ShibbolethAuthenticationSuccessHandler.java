package fi.csc.ristiinopiskelu.admin.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import static net.logstash.logback.marker.Markers.append;

public class ShibbolethAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static Logger logger = LoggerFactory.getLogger(ShibbolethAuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String eppn = ((ShibbolethUserDetails) authentication.getPrincipal()).getEppn();
        String remoteHost = request.getRemoteHost();

        logger.info(
                append("eppn", eppn)
                        .and(append("remoteHost", remoteHost)),
                "User '{}' logged in", eppn);
    }
}
