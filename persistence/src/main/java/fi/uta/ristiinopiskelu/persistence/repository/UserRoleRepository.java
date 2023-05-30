package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.UserRoleEntity;

import java.util.List;

public interface UserRoleRepository extends ExtendedRepository<UserRoleEntity, String> {

    List<UserRoleEntity> findByEppn(String eppn);
}
