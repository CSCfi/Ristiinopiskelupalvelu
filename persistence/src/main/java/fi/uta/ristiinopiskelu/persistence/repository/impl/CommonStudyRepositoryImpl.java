package fi.uta.ristiinopiskelu.persistence.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.persistence.repository.CommonStudyRepository;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class CommonStudyRepositoryImpl<T extends CompositeIdentifiedEntity> implements CommonStudyRepository<T> {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected ElasticsearchRestTemplate elasticsearchTemplate;

    private final String HISTORY_SUFFIX = "-history";

    @Override
    public void deleteHistoryById(String id, Class<T> clazz) {
        elasticsearchTemplate.delete(id, clazz);
    }

    @Override
    public <C extends CompositeIdentifiedEntity> String saveHistory(T original, Class<C> type) {
        C copy;

        try {
            copy = objectMapper.readValue(objectMapper.writeValueAsString(original), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Entity serialisation failed", e);
        }
        
        copy.setId(null); // null id, so elasticsearch will generate new one and will not update same history document always
        copy.setVersion(null);
        String indexName = ((Document) original.getClass().getAnnotations()[0]).indexName();
        String updIndexName = indexName + HISTORY_SUFFIX;
        return elasticsearchTemplate.save(copy, IndexCoordinates.of(updIndexName)).getId();
    }

    @Override
    public List<T> findByStudyElementReference(String referenceIdentifier, String referenceOrganizer, Class<T> type) {
        String referenceField = "studyElementReferences";
        if(type.getSuperclass() == StudyElementEntity.class) {
            // studyelement's parent references are in field "parents"
            referenceField = "parents";
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.nestedQuery(referenceField, QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery(referenceField + ".referenceIdentifier", referenceIdentifier))
                .must(QueryBuilders.matchQuery(referenceField + ".referenceOrganizer", referenceOrganizer)), ScoreMode.None));

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .build();

        return elasticsearchTemplate.search(builder, type).get().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
