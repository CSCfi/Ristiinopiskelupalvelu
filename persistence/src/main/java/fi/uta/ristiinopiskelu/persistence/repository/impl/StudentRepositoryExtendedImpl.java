package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.persistence.repository.StudentRepositoryExtended;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

public class StudentRepositoryExtendedImpl implements StudentRepositoryExtended {

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public List<StudentEntity> findByOidOrPersonId(@Nullable String oid, @Nullable String personId, Pageable pageable) {
        if(!StringUtils.hasText(oid) && !StringUtils.hasText(personId)) {
            throw new IllegalArgumentException("Either oid or personId must be defined");
        }

        Query query;

        if(StringUtils.hasText(oid) && StringUtils.hasText(personId)) {
            query = QueryBuilders.bool()
                    .must(List.of(
                            QueryBuilders.term(tq -> tq.field("oid").value(oid)),
                            QueryBuilders.term(tq -> tq.field("personId").value(personId))
                    )).build()._toQuery();
        } else if(StringUtils.hasText(oid)) {
            query = QueryBuilders.term(tq -> tq.field("oid").value(oid));
        } else {
            query = QueryBuilders.term(tq -> tq.field("personId").value(personId));
        }

        return elasticsearchTemplate.search(NativeQuery.builder()
                        .withQuery(query)
                        .withPageable(pageable)
                        .build(), StudentEntity.class)
                .map(SearchHit::getContent)
                .toList();
    }
}
