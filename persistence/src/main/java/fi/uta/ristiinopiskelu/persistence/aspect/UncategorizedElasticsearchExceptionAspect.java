package fi.uta.ristiinopiskelu.persistence.aspect;

import fi.uta.ristiinopiskelu.persistence.exception.VerboseUncategorizedElasticsearchException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;

@Aspect
public class UncategorizedElasticsearchExceptionAspect {

    @Around("execution(* fi.uta.ristiinopiskelu.persistence.repository..*(..))")
    public Object wrapExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            if (e instanceof UncategorizedElasticsearchException uncategorizedElasticsearchException) {
                throw new VerboseUncategorizedElasticsearchException(uncategorizedElasticsearchException);
            }

            throw e;
        }
    }
}
