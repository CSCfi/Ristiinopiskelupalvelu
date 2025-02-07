package fi.uta.ristiinopiskelu.denormalizer;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;

public interface Denormalizer<T extends GenericEntity> {

    void denormalize();
}
