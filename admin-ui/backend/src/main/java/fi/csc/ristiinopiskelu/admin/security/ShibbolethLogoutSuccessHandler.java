package fi.csc.ristiinopiskelu.admin.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import java.io.IOException;

import static net.logstash.logback.marker.Markers.append;

public class ShibbolethLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(ShibbolethLogoutSuccessHandler.class);

    public ShibbolethLogoutSuccessHandler(String targetUrl) {
        super();
        setAlwaysUseDefaultTargetUrl(true);
        setDefaultTargetUrl(targetUrl);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException, ServletException {
        boolean isAuthenticated = auth != null && auth.isAuthenticated();
        boolean hasHakaUserDetails = isAuthenticated && (auth.getPrincipal() != null && (auth.getPrincipal() instanceof ShibbolethUserDetails));

        if (!hasHakaUserDetails) {
            super.onLogoutSuccess(request, response, auth);
            return;
        }

        String userEppn = ((ShibbolethUserDetails) auth.getPrincipal()).getEppn();

        logger.info(
                append("eppn", userEppn)
                        .and(append("remoteHost", request.getRemoteHost())),
                "User '{}' logged out", userEppn);

        super.onLogoutSuccess(request, response, auth);
    }
}
