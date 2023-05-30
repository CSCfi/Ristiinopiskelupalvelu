package fi.uta.ristiinopiskelu.handler.utils;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.collections4.comparators.FixedOrderComparator;
import org.apache.commons.lang3.ArrayUtils;

import java.io.StringWriter;
import java.util.List;

public class CsvUtils {

    public static <T> String generateCsvContent(List<T> csvDtos, Class<T> csvDtoClazz, String... fixedColumnOrder) throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        StringWriter stringWriter = new StringWriter();

        HeaderColumnNameMappingStrategy<T> mappingStrategy = new HeaderColumnNameMappingStrategy<>();
        mappingStrategy.setType(csvDtoClazz);

        // workaround to make sure we get specified fields first in the CSV. fields not specified in fixedOrder will be written after the ones explicitly specified
        if(!ArrayUtils.isEmpty(fixedColumnOrder)) {
            FixedOrderComparator<String> comparator = new FixedOrderComparator<>(fixedColumnOrder);
            comparator.setUnknownObjectBehavior(FixedOrderComparator.UnknownObjectBehavior.AFTER);
            mappingStrategy.setColumnOrderOnWrite(comparator);
        }

        StatefulBeanToCsv<T> csvWriter = new StatefulBeanToCsvBuilder<T>(stringWriter)
            .withMappingStrategy(mappingStrategy)
            .build();

        csvWriter.write(csvDtos);

        return stringWriter.toString();
    }
}
