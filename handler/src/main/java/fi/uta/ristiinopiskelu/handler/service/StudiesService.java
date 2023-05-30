package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;

import java.util.List;

public interface StudiesService {

    List<StudyElementEntity> findAllStudiesByParentReferences(String referenceIdentifier, String referenceOrganizer) throws FindFailedException;

    StudiesSearchResults search(String organisationId, StudiesSearchParameters searchParams) throws FindFailedException;

    AbstractStudyElementReadDTO toRestDTO(StudyElementEntity entity);
}
