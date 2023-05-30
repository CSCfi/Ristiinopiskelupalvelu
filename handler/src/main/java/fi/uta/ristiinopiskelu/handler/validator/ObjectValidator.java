package fi.uta.ristiinopiskelu.handler.validator;

import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;

import java.util.List;

public interface ObjectValidator<T> {

    void validateObject(T object, String organisationId) throws ValidationException;

    void validateObject(List<T> object, String organisationId) throws ValidationException;
}
