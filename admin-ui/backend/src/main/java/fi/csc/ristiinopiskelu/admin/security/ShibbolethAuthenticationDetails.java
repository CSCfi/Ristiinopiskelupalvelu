package fi.csc.ristiinopiskelu.admin.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public class ShibbolethAuthenticationDetails {

    private final String organisation;
    private final String givenName;
    private final String firstNames;
    private final String surName;
    private final String email;
    private final String fullName;

    public ShibbolethAuthenticationDetails(String organisation, String givenName, String firstNames, String surName,
                                           String email, String fullName) {
        this.organisation = organisation;
        this.givenName = givenName;
        this.firstNames = firstNames;
        this.surName = surName;
        this.email = email;
        this.fullName = fullName;
    }

    public ShibbolethAuthenticationDetails(HttpServletRequest req) {
        Assert.notNull(req, "Request cannot be null");
        this.organisation = extractValue(ShibbolethAttribute.ORGANISATION, req);
        this.givenName = extractValue(ShibbolethAttribute.GIVEN_NAME, req);
        this.firstNames = extractValue(ShibbolethAttribute.FIRST_NAMES, req);
        this.surName = extractValue(ShibbolethAttribute.SURNAME, req);
        this.email = extractValue(ShibbolethAttribute.EMAIL, req);
        this.fullName = extractValue(ShibbolethAttribute.FULL_NAME, req);
    }

    public String getOrganisation() {
        return organisation;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public String getSurName() {
        return surName;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    private String extractValue(ShibbolethAttribute attribute, HttpServletRequest req) {
        Object value = req.getAttribute(attribute.getValue());

        if(value instanceof String stringValue) {
            if(StringUtils.hasText(stringValue)) {
                return new String(stringValue.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    public static ShibbolethAuthenticationDetails getCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated() &&
                authentication.getDetails() instanceof ShibbolethAuthenticationDetails shibbolethAuthenticationDetails) {
            return shibbolethAuthenticationDetails;
        }

        throw new PreAuthenticatedCredentialsNotFoundException("User is not authenticated");
    }
}
