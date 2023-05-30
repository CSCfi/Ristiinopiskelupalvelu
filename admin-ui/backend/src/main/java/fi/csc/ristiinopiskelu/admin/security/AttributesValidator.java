package fi.csc.ristiinopiskelu.admin.security;

import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.Map;

public class AttributesValidator {

    public static Errors validate(ShibbolethAuthenticationToken token) {
        Map<String, String> attrs = token.getAttributes();
        Errors errors = new MapBindingResult(attrs, "attrs");

        rejectNullOrEmpty(attrs, "eppn", errors);
        //rejectNullOrEmpty(attrs, "sn", errors);
        //rejectNullOrEmpty(attrs, "funetEduPersonGivenNames", errors);
        //rejectNullOrEmpty(attrs, "givenName", errors);
        //rejectNullOrEmpty(attrs, "mail", errors);
        
        return errors;
    }

    /**
     * Tarkistaa, onko annettu Map:n arvo null tai tyhjä ja jos, niin hylkää sen.
     *
     * @param attrs Map, josta arvo otetaan
     * @param attr arvon avain
     * @param errors Errors-olio, johon virheet kirjataan
     */
    private static void rejectNullOrEmpty(Map<String, String> attrs, String attr, Errors errors) {
        String value = attrs.get(attr);
        if (value == null) {
            errors.rejectValue(attr, "null");
        } else if (value.isEmpty()) {
            errors.rejectValue(attr, "empty");
        }
    }
}
