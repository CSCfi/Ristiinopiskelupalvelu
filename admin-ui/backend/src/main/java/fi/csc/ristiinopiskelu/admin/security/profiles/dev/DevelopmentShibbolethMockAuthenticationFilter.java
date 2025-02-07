package fi.csc.ristiinopiskelu.admin.security.profiles.dev;

import fi.csc.ristiinopiskelu.admin.security.ShibbolethAttribute;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public class DevelopmentShibbolethMockAuthenticationFilter implements Filter {

    /**
     * Mock user data, as in do what Shibboleth does after a successful authentication.
     *
     * NOTE: user roles are defined in DevelopmentApplicationConfig#createBaseData.
     *
     * @see fi.csc.ristiinopiskelu.admin.config.profiles.dev.DevelopmentApplicationConfig
     * @param request  The request to process
     * @param response The response associated with the request
     * @param chain    Provides access to the next filter in the chain for this filter to pass the request and response
     *                     to for further processing
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        
        httpReq.setAttribute(ShibbolethAttribute.EPPN.getValue(), "oo99999@tuni.fi");
        httpReq.setAttribute(ShibbolethAttribute.ORGANISATION.getValue(), "DEV10122"); // TUNI
        httpReq.setAttribute(ShibbolethAttribute.GIVEN_NAME.getValue(), "Olga");
        httpReq.setAttribute(ShibbolethAttribute.FIRST_NAMES.getValue(), "Olga Maria");
        httpReq.setAttribute(ShibbolethAttribute.SURNAME.getValue(), "Opiskelija");
        httpReq.setAttribute(ShibbolethAttribute.EMAIL.getValue(), "olga.opiskelija@student.uta.fi");
        httpReq.setAttribute(ShibbolethAttribute.FULL_NAME.getValue(), "Olga Opiskelija");
        
        chain.doFilter(httpReq, response);
    }
}
