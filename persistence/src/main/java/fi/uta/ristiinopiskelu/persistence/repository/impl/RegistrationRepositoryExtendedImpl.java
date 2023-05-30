package fi.uta.ristiinopiskelu.persistence.repository.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.RegistrationRepositoryExtended;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RegistrationRepositoryExtendedImpl implements RegistrationRepositoryExtended {

    @Autowired
    protected ElasticsearchRestTemplate elasticsearchTemplate;

    @Override
    public List<RegistrationEntity> findAllByStudentAndSelectionsReplies(StudyRecordStudent student, String selectionItemId,
                                                                         String organizingOrganisationId, String selectionItemType) {
        return findAllByStudentAndSelectionsOrSelectionsReplies(student, selectionItemId, organizingOrganisationId, selectionItemType, "selectionsReplies");
    }

    @Override
    public List<RegistrationEntity> findAllByStudentAndSelections(StudyRecordStudent student, String selectionItemId,
                                                                  String organizingOrganisationId, String selectionItemType) {
        return findAllByStudentAndSelectionsOrSelectionsReplies(student, selectionItemId, organizingOrganisationId, selectionItemType, "selections");
    }

    private List<RegistrationEntity> findAllByStudentAndSelectionsOrSelectionsReplies(StudyRecordStudent student, String selectionItemId,
                                                                                      String organizingOrganisationId, String selectionItemType,
                                                                                      String selectionFieldName) {
        QueryBuilder matchQuery;

        if(StringUtils.hasText(student.getOid()) && StringUtils.hasText(student.getPersonId())) {
            matchQuery = QueryBuilders.boolQuery()
                .should(QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("student.oid", student.getOid()))
                    .must(QueryBuilders.termQuery("student.personId", student.getPersonId())))
                .should(QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("student.oid", student.getOid()))
                    .mustNot(QueryBuilders.existsQuery("student.personId")))
                .should(QueryBuilders.boolQuery()
                    .mustNot(QueryBuilders.existsQuery("student.oid"))
                    .must(QueryBuilders.termQuery("student.personId", student.getPersonId())));
        } else if(StringUtils.hasText(student.getOid())) {
            matchQuery = QueryBuilders.termQuery("student.oid", student.getOid());
        } else if(StringUtils.hasText(student.getPersonId())) {
            matchQuery = QueryBuilders.termQuery("student.personId", student.getPersonId());
        } else {
            QueryBuilder hostStudyRightQuery = this.getStudentStudyRightIdentifierQuery("hostStudyRight", student.getHostStudyRightIdentifier());
            QueryBuilder homeStudyRightQuery = this.getStudentStudyRightIdentifierQuery("homeStudyRight", student.getHomeStudyRightIdentifier());

            matchQuery = QueryBuilders.boolQuery().must(hostStudyRightQuery).must(homeStudyRightQuery);
        }

        // note that receivingOrganisation is the organisation here that registrations are sent _to_, hence it's the organization that's actually organizing the studies
        BoolQueryBuilder query = QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery("receivingOrganisationTkCode", organizingOrganisationId))
            .must(matchQuery)
            .must(QueryBuilders.nestedQuery(selectionFieldName, QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(String.format("%s.selectionItemId", selectionFieldName), selectionItemId))
                .must(QueryBuilders.termQuery(String.format("%s.selectionItemType", selectionFieldName), selectionItemType)), ScoreMode.None));

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
            .withQuery(query)
            .build();

        return elasticsearchTemplate.search(builder, RegistrationEntity.class).get()
            .map(SearchHit::getContent).collect(Collectors.toList());
    }

    private QueryBuilder getStudentStudyRightIdentifierQuery(String studyRightPropertyName, StudyRightIdentifier studyRightIdentifier) {
        String studyRightPropertyPath = String.format("student.%s", studyRightPropertyName);
        String studyRightIdentifiersPropertyPath = String.format("%s.identifiers", studyRightPropertyPath);

        return QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery(String.format("%s.organisationTkCodeReference", studyRightIdentifiersPropertyPath), studyRightIdentifier.getOrganisationTkCodeReference()))
            .must(QueryBuilders.termQuery(String.format("%s.studyRightId", studyRightIdentifiersPropertyPath), studyRightIdentifier.getStudyRightId()));
    }

    @Override
    public List<RegistrationEntity> findAllByParams(String studentOid, String studentId, String homeEppn, List<String> organisationIds,
                                                    OffsetDateTime sendDateTimeStart, OffsetDateTime sendDateTimeEnd, RegistrationStatus status,
                                                    Pageable pageable) {

        BoolQueryBuilder query = QueryBuilders.boolQuery();

        if(!StringUtils.isEmpty(studentOid)) {
            query.must(QueryBuilders.matchQuery("student.oid", studentOid));
        }

        if(!StringUtils.isEmpty(studentId)) {
            query.must(QueryBuilders.matchQuery("student.personId", studentId));
        }

        if(!StringUtils.isEmpty(homeEppn)) {
            query.must(QueryBuilders.matchQuery("student.homeEppn", homeEppn));
        }

        if(!CollectionUtils.isEmpty(organisationIds)) {
            BoolQueryBuilder organisationQuery = QueryBuilders.boolQuery();
            for(String organisationId : organisationIds) {
                organisationQuery.should(QueryBuilders.matchQuery("sendingOrganisationTkCode", organisationId));
            }

            query.must(organisationQuery);
        }

        if(sendDateTimeStart != null) {
            query.must(QueryBuilders.rangeQuery("sendDateTime").from(sendDateTimeStart));
        }

        if(sendDateTimeEnd != null) {
            query.must(QueryBuilders.rangeQuery("sendDateTime").to(sendDateTimeEnd));
        }

        if(status != null) {
            query.must(QueryBuilders.matchQuery("status", status.name()));
        }

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .withPageable(pageable)
                .build();

        return elasticsearchTemplate.search(builder, RegistrationEntity.class).get().map(SearchHit::getContent).collect(Collectors.toList());
    }

    @Override
    public List<RegistrationEntity> findAllBySendingOrganisationTkCodeAndStudentPersonIdOrStudentOid(String sendingOrganisationTkCode, String studentPersonId, String studentOid, Pageable pageable) {
        Assert.hasText(sendingOrganisationTkCode,  "sendingOrganisationTkCode cannot be empty");

        if(!StringUtils.hasText(studentOid) && !StringUtils.hasText(studentPersonId)) {
            throw new IllegalArgumentException("Either studentOid or studentPersonId must be specified");
        }

        if(pageable == null) {
            pageable = Pageable.unpaged();
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();

        query.must(QueryBuilders.termQuery("sendingOrganisationTkCode", sendingOrganisationTkCode));

        if(StringUtils.hasText(studentPersonId) && StringUtils.hasText(studentOid)) {
            query.must(QueryBuilders.boolQuery()
                .should(QueryBuilders.termQuery("student.personId", studentPersonId))
                .should(QueryBuilders.termQuery("student.oid", studentOid)));
        } else {
            if(StringUtils.hasText(studentPersonId)) {
                query.must(QueryBuilders.termQuery("student.personId", studentPersonId));
            }

            if(StringUtils.hasText(studentOid)) {
                query.must(QueryBuilders.termQuery("student.oid", studentOid));
            }
        }

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
            .withQuery(query)
            .withPageable(pageable)
            .build();

        return elasticsearchTemplate.search(builder, RegistrationEntity.class).get()
            .map(SearchHit::getContent).collect(Collectors.toList());
    }
}
