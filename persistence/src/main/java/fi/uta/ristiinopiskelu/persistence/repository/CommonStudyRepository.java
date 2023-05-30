package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;

import java.util.List;

public interface CommonStudyRepository<T extends CompositeIdentifiedEntity> {

    void deleteHistoryById(String id, Class<T> clazz);

    <C extends CompositeIdentifiedEntity> String saveHistory(T original, Class<C> type);

    List<T> findByStudyElementReference(String referenceIdentifier, String referenceOrganizer, Class<T> type);
}
