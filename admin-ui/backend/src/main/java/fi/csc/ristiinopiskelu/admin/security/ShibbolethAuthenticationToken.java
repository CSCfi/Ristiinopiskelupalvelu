package fi.csc.ristiinopiskelu.admin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ShibbolethAuthenticationToken extends AbstractAuthenticationToken implements Serializable {

    public static Logger logger = LoggerFactory.getLogger(ShibbolethAuthenticationToken.class);
    private Object principal;
    private Map<String, String> attributes = new HashMap<>();

    /**
     * Constructor used by ShibbolethAuthenticationFilter.
     */
    public ShibbolethAuthenticationToken(String eppn, Map<String, String> attributes) {
        super(null);
        logger.debug("Instantiation by ShibbolethAuthenticationFilter.");
        this.attributes.putAll(attributes);
        principal = eppn;
    }

    /**
     * Constructor used by ShibbolethAuthenticationProvider.
     */
    public ShibbolethAuthenticationToken(UserDetails userDetails, Collection<GrantedAuthority> authorities) {
        super(authorities);
        logger.debug("Instantiation by ShibbolethAuthenticationProvider.");
        principal = userDetails;
        setAuthenticated(true);
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
