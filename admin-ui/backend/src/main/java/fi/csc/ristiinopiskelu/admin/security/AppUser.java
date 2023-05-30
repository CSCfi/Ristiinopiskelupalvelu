package fi.csc.ristiinopiskelu.admin.security;

import org.springframework.security.core.GrantedAuthority;

public interface AppUser {

    String getEppn();
    String getGivenName();
    String getFirstnames();
    String getSurname();
    String getEmail();
    String getFullname();
    boolean hasAuthority(GrantedAuthority authority);
}
