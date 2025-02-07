package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.RegistrationRepositoryExtended;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

public class RegistrationRepositoryExtendedImpl implements RegistrationRepositoryExtended {

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

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

    private List<RegistrationEntity> findAllByStudentAndSelectionsOrSelectionsReplies(@Nullable StudyRecordStudent student,
                                                                                      String selectionItemId,
                                                                                      String organizingOrganisationId,
                                                                                      String selectionItemType,
                                                                                      String selectionFieldName) {

        Query homeStudyRightQuery = this.getStudentStudyRightIdentifierQuery("homeStudyRight", student.getHomeStudyRightIdentifier());

        // note that receivingOrganisation is the organisation here that registrations are sent _to_, hence it's the organization that's actually organizing the studies
        BoolQuery.Builder query = new BoolQuery.Builder()
            .must(q -> q
                .term(tq -> tq
                    .field("receivingOrganisationTkCode")
                    .value(organizingOrganisationId)))
            .must(homeStudyRightQuery)
            .must(q -> q
                .nested(nq -> nq
                    .path(selectionFieldName)
                    .scoreMode(ChildScoreMode.None)
                    .query(q2 -> q2
                        .bool(bq -> bq
                            .should(getSelectionItemQuery(selectionFieldName, selectionItemId, selectionItemType))
                            .should(getSelectionItemQuery("%s.parent".formatted(selectionFieldName), selectionItemId, selectionItemType))
                            .should(getSelectionItemQuery("%s.parent.parent".formatted(selectionFieldName), selectionItemId, selectionItemType))
                            .should(getSelectionItemQuery("%s.parent.parent.parent".formatted(selectionFieldName), selectionItemId, selectionItemType))
                        )
                    )
                )
            );

        NativeQuery builder = new NativeQueryBuilder()
                .withQuery(query.build()._toQuery())
                .withPageable(Pageable.unpaged())
                .build();

        return elasticsearchTemplate.search(builder, RegistrationEntity.class).get()
                .map(SearchHit::getContent)
                .toList();
    }

    private Query getSelectionItemQuery(String path, String selectionItemId, String selectionItemType) {
        return new BoolQuery.Builder()
            .must(q -> q.term(tq -> tq.field("%s.selectionItemId".formatted(path)).value(selectionItemId)))
            .must(q -> q.term(tq -> tq.field("%s.selectionItemType".formatted(path)).value(selectionItemType)))
            .build()._toQuery();
    }

    private Query getStudentStudyRightIdentifierQuery(String studyRightPropertyName, StudyRightIdentifier studyRightIdentifier) {
        String studyRightPropertyPath = String.format("student.%s", studyRightPropertyName);
        String studyRightIdentifiersPropertyPath = String.format("%s.identifiers", studyRightPropertyPath);

        return new BoolQuery.Builder()
            .must(q -> q.term(tq -> tq.field(String.format("%s.organisationTkCodeReference", studyRightIdentifiersPropertyPath)).value(studyRightIdentifier.getOrganisationTkCodeReference())))
            .must(q -> q.term(tq -> tq.field(String.format("%s.studyRightId", studyRightIdentifiersPropertyPath)).value(studyRightIdentifier.getStudyRightId())))
            .build()._toQuery();
    }

    @Override
    public List<RegistrationEntity> findAllByParams(String studentOid, String studentId, String homeEppn, List<String> organisationIds,
                                                    OffsetDateTime sendDateTimeStart, OffsetDateTime sendDateTimeEnd, RegistrationStatus status,
                                                    Pageable pageable) {

        BoolQuery.Builder query = new BoolQuery.Builder();

        if(StringUtils.hasText(studentOid)) {
            query.must(q -> q.match(mq -> mq.field("student.oid").query(studentOid)));
        }

        if(StringUtils.hasText(studentId)) {
            query.must(q -> q.match(mq -> mq.field("student.personId").query(studentId)));
        }

        if(StringUtils.hasText(homeEppn)) {
            query.must(q -> q.match(mq -> mq.field("student.homeEppn").query(homeEppn)));
        }

        if(!CollectionUtils.isEmpty(organisationIds)) {
            BoolQuery.Builder organisationQuery = new BoolQuery.Builder();
            for(String organisationId : organisationIds) {
                organisationQuery.should(q -> q.match(mq -> mq.field("sendingOrganisationTkCode").query(organisationId)));
            }

            query.must(organisationQuery.build()._toQuery());
        }

        if(sendDateTimeStart != null) {
            query.must(q -> q.range(rq -> rq.field("sendDateTime").from(sendDateTimeStart.toString())));
        }

        if(sendDateTimeEnd != null) {
            query.must(q -> q.range(rq -> rq.field("sendDateTime").to(sendDateTimeEnd.toString())));
        }

        if(status != null) {
            query.must(q -> q.match(mq -> mq.field("status").query(status.name())));
        }

        NativeQuery builder = new NativeQueryBuilder()
                .withQuery(query.build()._toQuery())
                .withPageable(pageable)
                .build();

        return elasticsearchTemplate.search(builder, RegistrationEntity.class).get()
                .map(SearchHit::getContent)
                .toList();
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

        BoolQuery.Builder query = new BoolQuery.Builder();

        query.must(q -> q.term(tq -> tq.field("sendingOrganisationTkCode").value(sendingOrganisationTkCode)));

        if(StringUtils.hasText(studentPersonId) && StringUtils.hasText(studentOid)) {
            query.must(q -> q.bool(bq -> bq
                .should(q2 -> q2.term(tq -> tq.field("student.personId").value(studentPersonId)))
                .should(q2 -> q2.term(tq -> tq.field("student.oid").value(studentOid)))));
        } else {
            if(StringUtils.hasText(studentPersonId)) {
                query.must(q -> q.term(tq -> tq.field("student.personId").value(studentPersonId)));
            }

            if(StringUtils.hasText(studentOid)) {
                query.must(q -> q.term(tq -> tq.field("student.oid").value(studentOid)));
            }
        }

        NativeQuery builder = new NativeQueryBuilder()
            .withQuery(query.build()._toQuery())
            .withPageable(pageable)
            .build();

        return elasticsearchTemplate.search(builder, RegistrationEntity.class).get()
                .map(SearchHit::getContent)
                .toList();
    }
}
