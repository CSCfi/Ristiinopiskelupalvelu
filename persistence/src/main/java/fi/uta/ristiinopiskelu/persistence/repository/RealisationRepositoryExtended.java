package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;

import java.util.List;

public interface RealisationRepositoryExtended {
    List<RealisationEntity> findByAssessmentItemReference(String referenceIdentifier, String referenceOrganizer, String referenceAssessmentItemId);
}
