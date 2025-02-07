package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.persistence.repository.CommonStudyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.List;
import java.util.stream.Collectors;

public class CommonStudyRepositoryImpl<T extends CompositeIdentifiedEntity> implements CommonStudyRepository<T> {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

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
        String referenceField;

        if(type.getSuperclass() == StudyElementEntity.class) {
            // studyelement's parent references are in field "parents"
            referenceField = "parents";
        } else {
            referenceField = "studyElementReferences";
        }

        BoolQuery.Builder query = new BoolQuery.Builder()
                .must(q -> q
                    .nested(nq -> nq
                        .path(referenceField)
                        .scoreMode(ChildScoreMode.None)
                        .query(q2 -> q2
                            .bool(bq -> bq
                                .must(q3 -> q3
                                    .term(tq -> tq
                                        .field(referenceField + ".referenceIdentifier")
                                        .value(referenceIdentifier)))
                                .must(q3 -> q3
                                    .term(tq -> tq
                                        .field(referenceField + ".referenceOrganizer")
                                        .value(referenceOrganizer)))))));

        NativeQuery builder = new NativeQueryBuilder()
                .withQuery(query.build()._toQuery())
                .withPageable(Pageable.unpaged())
                .build();

        return elasticsearchTemplate.search(builder, type).get().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
