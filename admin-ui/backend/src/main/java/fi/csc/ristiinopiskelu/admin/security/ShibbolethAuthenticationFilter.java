package fi.csc.ristiinopiskelu.admin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

import static net.logstash.logback.marker.Markers.append;

public class ShibbolethAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static Logger logger = LoggerFactory.getLogger(ShibbolethAuthenticationFilter.class);

    @Override
    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

    public ShibbolethAuthenticationFilter(String defaultFilterProcessesUrl) {
        super(defaultFilterProcessesUrl);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest httpReq, HttpServletResponse httpResp)
            throws AuthenticationException, IOException, ServletException {
        
        // collect predefined attributes from environment
        final String[] shibAttrNames = { "eppn", "sn", "funetEduPersonGivenNames", "givenName", "mail", "organisation", "cn" };
        
        HashMap<String, String> attributes = new HashMap<>();
        for (String attrName : shibAttrNames) {
            String value = (String) httpReq.getAttribute(attrName);
            if (value != null) {
                value = new String(value.getBytes("ISO-8859-1"), "UTF-8");
            } else {
                value = "";
            }
            attributes.put(attrName, value);
        }

        logger.info("Logging in with info: {}", attributes);

        String eppn = attributes.get("eppn");
        String remoteHost = httpReq.getRemoteHost();

        if(StringUtils.isEmpty(eppn)) {
            logger.info(append("remoteHost", remoteHost),
                    "Failed login attempt from '{}'", remoteHost);
            throw new UsernameNotFoundException("EPPN is missing!");
        }

        logger.info(
                append("eppn", eppn)
                        .and(append("remoteHost", remoteHost)),
                "User '{}' logged in", eppn);

        ShibbolethAuthenticationToken token = new ShibbolethAuthenticationToken(eppn, attributes);
        return this.getAuthenticationManager().authenticate(token);
    }
}
