package fi.csc.ristiinopiskelu.admin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        boolean hasHakaUserDetails = isAuthenticated && (auth.getPrincipal() != null && (auth.getPrincipal() instanceof AppUserDetails));

        if (!hasHakaUserDetails) {
            super.onLogoutSuccess(request, response, auth);
            return;
        }

        AppUserDetails user = (AppUserDetails) auth.getPrincipal();

        logger.info(
                append("eppn", user.getEppn())
                        .and(append("remoteHost", request.getRemoteHost())),
                "User '{}' logged out", user.getEppn());

        super.onLogoutSuccess(request, response, auth);
    }
}
