package fi.uta.ristiinopiskelu.messaging.util;

import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MessageTypeCsvPrinter {

    public static void main(String[] args) throws Exception {
        for(MessageType messageType : MessageType.values()) {
            MessageTypeCsvPrinter printer = new MessageTypeCsvPrinter();
            printer.writeToFile(messageType);
        }
    }

    private void writeToFile(MessageType messageType) throws Exception {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(messageType.name() + ".csv"))) {
            List<ObjectHierarchyElement> hierarchyElements = this.walkFields(null, null,
                    messageType.getClazz().getDeclaredFields(), "subElements", "parent");
            writer.write("Kenttä,Tyyppi,Arvot,Kuvaus,Pakollisuus\n");
            for(ObjectHierarchyElement element : hierarchyElements) {
                writer.write(String.format("%s\n", element.toString()));
            }
        }
    }

    private List<ObjectHierarchyElement> walkFields(List<ObjectHierarchyElement> hierarchyElements, String parentName, Field[] fields, String... skipFields) throws Exception {
        if(org.springframework.util.CollectionUtils.isEmpty(hierarchyElements)) {
            hierarchyElements = new ArrayList<>();
        }

        for(Field field : fields) {

            this.addValue(hierarchyElements, parentName, field);

            boolean skipField = false;

            for(String fieldToSkip : skipFields) {
                if (field.getName().equals(fieldToSkip)) {
                    skipField = true;
                }
            }

            if(skipField) {
                continue;
            }

            if(BeanUtils.isSimpleValueType(field.getType()) || field.getType().isAssignableFrom(OffsetDateTime.class)
                    || field.getType().isAssignableFrom(LocalDate.class) || field.getType().isAssignableFrom(LocalDateTime.class)) {
                continue;
            }

            Field[] declaredFields;

            if(field.getType().isAssignableFrom(List.class)) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];

                if(BeanUtils.isSimpleValueType(listClass)) {
                    continue;
                }

                declaredFields = listClass.getDeclaredFields();
            } else if(field.getType().isAssignableFrom(HashMap.class)) {
                continue;
            } else {
                declaredFields = field.getType().getDeclaredFields();
            }

            if(StringUtils.hasText(parentName)) {
                hierarchyElements = this.walkFields(hierarchyElements, String.format("%s.%s", parentName, field.getName()), declaredFields, skipFields);
            } else {
                hierarchyElements = this.walkFields(hierarchyElements, field.getName(), declaredFields, skipFields);
            }
        }

        return hierarchyElements;
    }

    private void addValue(List<ObjectHierarchyElement> hierarchyElements, String parentName, Field field) throws Exception {
        String type;
        String values = "";

        if(field.getType().isAssignableFrom(List.class)) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
            type = String.format("List<%s>", listClass.getSimpleName());
        } else if(field.getType().isAssignableFrom(HashMap.class)) {
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            Class<?> mapClass1 = (Class<?>) listType.getActualTypeArguments()[0];
            Class<?> mapClass2 = (Class<?>) listType.getActualTypeArguments()[1];
            type = String.format("Map<%s,%s>", mapClass1.getSimpleName(), mapClass2.getSimpleName());
        } else if(field.getType().isEnum()) {
            Method method = field.getType().getDeclaredMethod("values");
            Object object = method.invoke(null);
            values = Arrays.toString((Object[]) object);
            type = field.getType().getSimpleName();
        } else {
            type = field.getType().getSimpleName();
        }

        String newParentName;

        if(StringUtils.hasText(parentName)) {
            newParentName = String.format("%s.%s", parentName, field.getName());
        } else {
            newParentName = field.getName();
        }

        ObjectHierarchyElement element = new ObjectHierarchyElement(newParentName, type, values);
        //System.out.println(element.toString());
        hierarchyElements.add(element);

    }

    private class ObjectHierarchyElement {
        private String field;
        private String type;
        private String values;

        public ObjectHierarchyElement(String field, String type) {
            this.field = field;
            this.type = type;
        }

        public ObjectHierarchyElement(String field, String type, String values) {
            this.field = field;
            this.type = type;
            this.values = values;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValues() {
            return values;
        }

        public void setValues(String values) {
            this.values = values;
        }

        @Override
        public String toString() {
            return String.format("\"%s\",\"%s\",\"%s\"", field, type, values);
        }
    }
}
