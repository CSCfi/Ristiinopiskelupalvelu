package fi.uta.ristiinopiskelu.datamodel.entity.common;

import org.springframework.util.Assert;

public enum RoleEntity {

    ADMIN("ROLE_ADMIN"),
    SUPERUSER("ROLE_SUPERUSER");

    private final String prefixedRole;

    RoleEntity(String prefixedRole) {
        Assert.hasText(prefixedRole, "Prefixed must be supplied");
        this.prefixedRole = prefixedRole;
    }

    public String getPrefixedRole() {
        return prefixedRole;
    }

    public static RoleEntity from(String prefixedRole) {
        Assert.hasText(prefixedRole, "Prefixed role must be supplied");

        for(RoleEntity role : RoleEntity.values()) {
            if(prefixedRole.equals(role.getPrefixedRole())) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown role " + prefixedRole);
    }
}
