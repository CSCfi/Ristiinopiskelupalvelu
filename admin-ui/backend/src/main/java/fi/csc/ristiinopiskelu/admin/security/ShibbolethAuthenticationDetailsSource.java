package fi.csc.ristiinopiskelu.admin.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;

public class ShibbolethAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, ShibbolethAuthenticationDetails> {
    
    @Override
    public ShibbolethAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new ShibbolethAuthenticationDetails(context);
    }
}
