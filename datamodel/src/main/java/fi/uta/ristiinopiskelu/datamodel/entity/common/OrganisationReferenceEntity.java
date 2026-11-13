package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganisationReferenceEntity {

  private BigDecimal percent;

  @Field(type = FieldType.Integer)
  private OrganisationRoleEntity organisationRole;

  private OrganisationDataEntity target;
  private LocalisedStringEntity description;

  public static OrganisationReferenceEntity from(OrganisationReference source) {
    if (source == null) {
      return null;
    }
    OrganisationReferenceEntity target = new OrganisationReferenceEntity();
    target.setPercent(source.getPercent());
    target.setOrganisationRole(source.getOrganisationRole() == null ? null : OrganisationRoleEntity.valueOf(source.getOrganisationRole().name()));
    target.setTarget(OrganisationDataEntity.from(source.getTarget()));
    target.setDescription(LocalisedStringEntity.from(source.getDescription()));
    return target;
  }

  public static List<OrganisationReferenceEntity> from(List<OrganisationReference> source) {
    if (source == null) {
      return null;
    }
    return source.stream().map(OrganisationReferenceEntity::from).toList();
  }
}
