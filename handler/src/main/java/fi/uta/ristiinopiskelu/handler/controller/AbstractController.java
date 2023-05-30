package fi.uta.ristiinopiskelu.handler.controller;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractController {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private MessageSchemaService messageSchemaService;

    public <T, C> T mapToDto(C currentDto, Class<T> targetDtoClazz) {
        return this.messageSchemaService.convertObject(currentDto, targetDtoClazz);
    }

    public <T, C> List<T> mapToDto(List<C> currentDtos, Class<T> targetDtoClazz) {
        if(CollectionUtils.isEmpty(currentDtos)) {
            return Collections.emptyList();
        }

        return currentDtos.stream().map(dto -> this.mapToDto(dto, targetDtoClazz)).collect(Collectors.toList());
    }

    public <T, C, E extends GenericEntity> T mapToDto(E entity, Class<T> targetDtoClazz, Class<C> currentDtoClazz) {
        if(currentDtoClazz.isAssignableFrom(targetDtoClazz)) {
            return this.modelMapper.map(entity, targetDtoClazz);
        }

        C currentDto = this.modelMapper.map(entity, currentDtoClazz);
        return this.mapToDto(currentDto, targetDtoClazz);
    }

    public <T, C, E extends GenericEntity> List<T> mapToDto(List<E> entities, Class<T> targetDtoClazz, Class<C> currentDtoClazz) {
        if(CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }

        return entities.stream().map(entity -> this.mapToDto(entity, targetDtoClazz, currentDtoClazz)).collect(Collectors.toList());
    }

    public <T, C> ListSearchResults<T> mapSearchResults(ListSearchResults<C> currentSearchResults, Class<T> targetResultType) {
        if(currentSearchResults != null && !CollectionUtils.isEmpty(currentSearchResults.getResults())) {
            List<C> currentResults = currentSearchResults.getResults();

            List<T> converted = currentResults.stream()
                .map(sr -> this.messageSchemaService.convertObject(sr, targetResultType))
                .collect(Collectors.toList());

            return new ListSearchResults<>(converted);
        }

        return new ListSearchResults<>(Collections.emptyList());
    }

    public <T, C> List<T> mapSearchResultsToList(ListSearchResults<C> currentSearchResults, Class<T> targetResultType) {
        return this.mapSearchResults(currentSearchResults, targetResultType).getResults();
    }
}
