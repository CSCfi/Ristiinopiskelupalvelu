package fi.uta.ristiinopiskelu.handler.validator;

import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class AbstractObjectValidator<T> implements ObjectValidator<T> {

    private Validator beanValidator;

    protected AbstractObjectValidator() {}

    public AbstractObjectValidator(Validator beanValidator) {
        this.beanValidator = beanValidator;
    }

    @Override
    public void validateObject(T object, String organisationId) throws ValidationException {
        this.runBeanValidation(object);
    }

    @Override
    public void validateObject(List<T> objects, String organisationId) throws ValidationException {
        this.runBeanValidation(objects);
    }

    protected void runBeanValidation(T object) {
        this.runBeanValidation(Collections.singletonList(object));
    }

    protected void runBeanValidation(List<T> objects) {
        if(CollectionUtils.isEmpty(objects)) {
            return;
        }

        for(T object : objects) {
            Set<ConstraintViolation<Object>> violations = this.beanValidator.validate(object);

            if(!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder();

                Iterator<ConstraintViolation<Object>> it = violations.iterator();
                while(it.hasNext()) {
                    ConstraintViolation<Object> violation = it.next();
                    sb.append(violation.getMessage());
                    if(it.hasNext()) {
                        sb.append(", ");
                    }
                }

                throw new ValidationException(sb.toString());
            }
        }
    }
}
