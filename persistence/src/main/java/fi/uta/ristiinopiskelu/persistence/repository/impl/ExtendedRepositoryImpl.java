package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.persistence.repository.ExtendedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.SimpleElasticsearchRepository;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.List;

public class ExtendedRepositoryImpl<T extends GenericEntity>
        extends SimpleElasticsearchRepository<T, String> implements ExtendedRepository<T, String> {

    public static final Logger logger = LoggerFactory.getLogger(ExtendedRepositoryImpl.class);

    private final ElasticsearchTemplate elasticsearchRestTemplate;

    public ExtendedRepositoryImpl(ElasticsearchEntityInformation<T, java.lang.String> entityInformation,
                                  ElasticsearchTemplate elasticsearchRestTemplate) {
        super(entityInformation, elasticsearchRestTemplate);
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    @Override
    public T update(T instance){
        instance.setVersion(instance.getVersion() + 1);

        if(instance instanceof RealisationEntity) {
            ((RealisationEntity) instance).setUpdateTime(OffsetDateTime.now());
        }

        if(instance instanceof StudyElementEntity) {
            ((StudyElementEntity) instance).setUpdateTime(OffsetDateTime.now());
        }

        return this.save(instance);
    }

    @Override
    public T create(T instance) {
        return this.create(instance, IndexQuery.OpType.INDEX);
    }

    @Override
    public T create(T instance, IndexQuery.OpType opType) {
        if(instance instanceof RealisationEntity) {
            ((RealisationEntity) instance).setCreatedTime(OffsetDateTime.now());
        }

        if(instance instanceof StudyElementEntity) {
            ((StudyElementEntity) instance).setCreatedTime(OffsetDateTime.now());
        }

        return this.save(instance, opType);
    }

    /**
     * Added for convenience since a similar one seems to have been removed from Repository apis.
     * 
     * @see SimpleElasticsearchRepository#findAll(Pageable)
     * @param query
     * @param pageable
     * @return
     */
    @Override
    public List<T> search(Query query, Pageable pageable) {
        NativeQueryBuilder nativeSearchQueryBuilder = new NativeQueryBuilder()
            .withQuery(query);

        if(pageable != null) {
            nativeSearchQueryBuilder.withPageable(pageable);
        } else {
            nativeSearchQueryBuilder.withPageable(Pageable.unpaged());
        }

        NativeQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

        SearchHits<T> searchHits = elasticsearchRestTemplate.search(nativeSearchQuery, super.entityClass);
        SearchPage<T> page = SearchHitSupport.searchPageFor(searchHits, nativeSearchQuery.getPageable());
        return ((Page<T>) SearchHitSupport.unwrapSearchHits(page)).getContent();
    }

    @Override
    public List<T> search(Query query) {
        return search(query, null);
    }

    private <S extends T> S save(S entity, IndexQuery.OpType opType) {

        Assert.notNull(entity, "Cannot save 'null' entity.");

        IndexCoordinates indexCoordinates = elasticsearchRestTemplate.getIndexCoordinatesFor(super.entityClass);
        elasticsearchRestTemplate.doIndex(createIndexQuery(entity, opType), indexCoordinates);
        elasticsearchRestTemplate.indexOps(indexCoordinates).refresh();
          
        return entity;
    }

    private IndexQuery createIndexQuery(T entity, IndexQuery.OpType opType) {
        IndexQueryBuilder indexQueryBuilder = new IndexQueryBuilder()
            .withId(elasticsearchRestTemplate.stringIdRepresentation(super.extractIdFromBean(entity)))
            .withRouting(elasticsearchRestTemplate.getEntityRouting(entity))
            .withObject(entity)
            .withVersion(super.entityInformation.getVersion(entity))
            .withOpType(opType);
        return indexQueryBuilder.build();
    }
}
