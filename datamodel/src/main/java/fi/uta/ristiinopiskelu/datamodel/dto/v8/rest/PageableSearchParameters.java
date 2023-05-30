package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class PageableSearchParameters<T> implements SearchParameters<T> {

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
        return pageSize <= 0 ? PAGESIZE_DEFAULT : pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
