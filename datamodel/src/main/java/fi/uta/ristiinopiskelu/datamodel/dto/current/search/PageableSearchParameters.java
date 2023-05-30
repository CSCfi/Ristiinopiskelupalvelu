package fi.uta.ristiinopiskelu.datamodel.dto.current.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public abstract class PageableSearchParameters<T extends GenericEntity> implements SearchParameters<T> {

    private static final int PAGESIZE_DEFAULT = 100;

    @Schema(description = "Oletusarvo: 0")
    private int page = 0;

    @Schema(description = "Oletusarvo: " + PAGESIZE_DEFAULT)
    private int pageSize = PAGESIZE_DEFAULT;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @JsonIgnore
    public PageRequest getPageRequest() {
        return getPageRequest(Sort.unsorted());
    }

    @JsonIgnore
    public PageRequest getPageRequest(Sort sort) {
        return PageRequest.of(page, pageSize, sort);
    }
}
