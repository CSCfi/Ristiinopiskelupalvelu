package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.SearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.SearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.handler.exception.*;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.service.Service;
import fi.uta.ristiinopiskelu.handler.utils.ExtendedBeanUtils;
import fi.uta.ristiinopiskelu.persistence.repository.ExtendedRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractService<D, T extends GenericEntity, R> implements Service<D, T, R> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractService.class);

    private final Class<D> writeDtoClass;
    private final Class<T> entityClass;
    private final Class<R> restDtoClass;

    public Class<D> getWriteDtoClass() {
        return writeDtoClass;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public Class<R> getReadDtoClass() {
        return restDtoClass;
    }

    protected abstract ExtendedRepository<T, String> getRepository();

    protected abstract ModelMapper getModelMapper();

    protected boolean isValidateId() {
        return true;
    }

    // Empty constructor for mocking in tests do not use outside of tests
    public AbstractService() {
        this.writeDtoClass = null;
        this.entityClass = null;
        this.restDtoClass = null;
    }

    public AbstractService(Class<D> dtoClass, Class<T> entityClass, Class<R> restDtoClass) {
        Assert.notNull(dtoClass, "DTO class cannot be null");
        Assert.notNull(entityClass, "Entity class cannot be null");
        Assert.notNull(restDtoClass, "REST DTO class cannot be null");
        this.writeDtoClass = dtoClass;
        this.entityClass = entityClass;
        this.restDtoClass = restDtoClass;
    }

    @Override
    public T create(T entity) throws CreateFailedException {
        Assert.notNull(entity, "Entity cannot be null");
        if(isValidateId()) {
            Assert.isNull(entity.getId(), "Id should be null, but was " + entity.getId());
        }
        try {
            return this.getRepository().create(entity);
        } catch (Exception e) {
            throw new CreateFailedException(getEntityClass(), e);
        }
    }

    @Override
    public List<T> createAll(List<T> entities) throws CreateFailedException {
        Assert.notEmpty(entities, "Entities cannot be null or empty");

        List<T> createdEntities = new ArrayList<>();

        for (T entity : entities) {
            if(isValidateId()) {
                Assert.isNull(entity.getId(), "Id should be null, but was " + entity.getId());
            }
            try {
                createdEntities.add(this.getRepository().create(entity));
            } catch (Exception e) {
                logger.error("Error while persisting entity {}, attempting to delete {} already persisted from index", entity, createdEntities.size(), e);

                for (T created : createdEntities) {
                    try {
                        getRepository().deleteById(created.getId());
                        logger.info("Successfully deleted entity with id {}", created.getId());
                    } catch (Exception e1) {
                        logger.error("Error while deleting already persisted entity {}", created.getId(), e1);
                    }
                }

                throw new CreateFailedException(getEntityClass(), e);
            }
        }

        return createdEntities;
    }

    @Override
    public T update(T entity) throws UpdateFailedException {
        Assert.notNull(entity, "Entity cannot be null");
        try {
            return this.getRepository().update(entity);
        } catch (Exception e) {
            throw new UpdateFailedException(getEntityClass(), entity.getId(), e);
        }
    }

    @Override
    public T deleteById(String id) throws DeleteFailedException {
        Assert.hasText(id, "Entity id cannot be empty");
        try {
            T original = this.getRepository().findById(id).orElseThrow(() -> new EntityNotFoundException(this.entityClass, id));
            this.getRepository().delete(original);
            return original;
        } catch (Exception e) {
            throw new DeleteFailedException(getEntityClass(), id, e);
        }
    }

    @Override
    public Optional<T> findById(String id) throws FindFailedException {
        Assert.hasLength(id, "Id cannot be empty");
        try {
            return this.getRepository().findById(id);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), id, e);
        }
    }

    @Override
    public List<T> findByIds(List<String> ids) throws FindFailedException {
        Assert.notEmpty(ids, "Ids cannot be empty");
        try {
            return StreamSupport.stream(this.getRepository().findAllById(ids).spliterator(), false).collect(Collectors.toList());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), ids, e);
        }
    }

    @Override
    public List<T> findAll(Pageable pageable) throws FindFailedException {
        try {
            return StreamSupport.stream(this.getRepository().findAll(pageable).spliterator(), false).collect(Collectors.toList());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }
    
    @Override
    public SearchResults<R> search(String organisationId, SearchParameters<T> searchParameters) throws FindFailedException, InvalidSearchParametersException {
        Assert.notNull(searchParameters, "Search parameters cannot be null");
        try {
            return new ListSearchResults<>(StreamSupport.stream(this.getRepository().findAll().spliterator(), false)
                .map(this::toReadDTO)
                .collect(Collectors.toList()));
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public R toReadDTO(T entity) {
        Assert.notNull(entity, "Entity cannot be null");
        return this.getModelMapper().map(entity, getReadDtoClass());
    }

    @Override
    public <A> A fillMissingValues(A object, T entity) {
        Assert.notNull(entity, "Entity cannot be null");
        ExtendedBeanUtils.copyProperties(entity, object, true);
        return object;
    }

    @Override
    public <A> A fillMissingValuesById(A object, String id) {
        T entity = this.findById(id).orElseThrow(() -> new EntityNotFoundException(this.entityClass, id));
        return this.fillMissingValues(object, entity);
    }
}
