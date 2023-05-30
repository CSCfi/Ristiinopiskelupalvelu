package fi.uta.ristiinopiskelu.handler.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public abstract class ExtendedBeanUtils extends BeanUtils {

    /**
     * Variation of Spring's BeanUtils#copyProperties that is capable of copying only values from target that are empty in source.
     * @see BeanUtils#copyProperties(Object, Object)
     */
    public static void copyProperties(Object source, Object target, boolean copyOnlyValuesEmptyInTarget, @Nullable String... ignoreProperties) {

        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");

        Class<?> actualEditable = target.getClass();
        PropertyDescriptor[] targetPds = BeanUtils.getPropertyDescriptors(actualEditable);
        List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);

        for (PropertyDescriptor targetPd : targetPds) {
            if(copyOnlyValuesEmptyInTarget) {
                Method targetReadMethod = targetPd.getReadMethod();
                try {
                    if (!Modifier.isPublic(targetReadMethod.getDeclaringClass().getModifiers())) {
                        targetReadMethod.setAccessible(true);
                    }

                    Object targetExistingValue = targetReadMethod.invoke(target);
                    if (((targetExistingValue instanceof String) && !StringUtils.isEmpty(targetExistingValue))
                            || targetExistingValue != null) {
                        continue;
                    }
                } catch (Throwable ex) {
                    throw new FatalBeanException(
                            "Could not read property '" + targetPd.getName() + "' from target", ex);
                }
            }

            Method writeMethod = targetPd.getWriteMethod();
            if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
                PropertyDescriptor sourcePd = BeanUtils.getPropertyDescriptor(source.getClass(), targetPd.getName());
                if (sourcePd != null) {
                    Method readMethod = sourcePd.getReadMethod();
                    if (readMethod != null &&
                            ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                        try {
                            if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                readMethod.setAccessible(true);
                            }
                            Object value = readMethod.invoke(source);
                            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                writeMethod.setAccessible(true);
                            }
                            writeMethod.invoke(target, value);
                        }
                        catch (Throwable ex) {
                            throw new FatalBeanException(
                                    "Could not copy property '" + targetPd.getName() + "' from source to target", ex);
                        }
                    }
                }
            }
        }
    }

}
