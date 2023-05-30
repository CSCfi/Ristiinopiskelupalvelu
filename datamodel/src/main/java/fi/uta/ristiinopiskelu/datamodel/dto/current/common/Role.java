package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import org.springframework.util.Assert;

public enum Role {

    ADMIN("ROLE_ADMIN"),
    SUPERUSER("ROLE_SUPERUSER");

    private final String prefixedRole;

    Role(String prefixedRole) {
        Assert.hasText(prefixedRole, "Prefixed must be supplied");
        this.prefixedRole = prefixedRole;
    }

    public String getPrefixedRole() {
        return prefixedRole;
    }

    public static Role from(String prefixedRole) {
        Assert.hasText(prefixedRole, "Prefixed role must be supplied");

        for(Role role : Role.values()) {
            if(prefixedRole.equals(role.getPrefixedRole())) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown role " + prefixedRole);
    }
}
