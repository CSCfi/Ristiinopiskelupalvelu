package fi.csc.ristiinopiskelu.admin.security.profiles.dev;

import fi.csc.ristiinopiskelu.admin.security.ShibbolethAuthenticationFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DevelopmentShibbolethAuthenticationFilter extends ShibbolethAuthenticationFilter {

    public DevelopmentShibbolethAuthenticationFilter(String defaultFilterProcessesUrl) {
        super(defaultFilterProcessesUrl);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest httpReq, HttpServletResponse httpResp) throws AuthenticationException, IOException, ServletException {
        // testaamista varten erilaisia attribuuttijoukkoja
        httpReq.setAttribute("eppn", "oo99999@uta.fi");
        httpReq.setAttribute("sn", "Opiskelija");
        httpReq.setAttribute("funetEduPersonGivenNames", "Olga Maria");
        httpReq.setAttribute("givenName", "Olga");
        httpReq.setAttribute("mail", "olga.opiskelija@student.uta.fi");
        //httpReq.setAttribute("organisation", "TUNI");
        httpReq.setAttribute("cn", "Olga Opiskelija");
        return super.attemptAuthentication(httpReq, httpResp);
    }
}
