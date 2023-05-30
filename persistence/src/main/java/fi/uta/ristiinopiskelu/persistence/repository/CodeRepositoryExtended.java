package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSetKeyWithCodeCount;

import java.util.List;

public interface CodeRepositoryExtended {
    
    List<CodeSetKeyWithCodeCount> findCodeSetKeysWithCodeCount();
}
