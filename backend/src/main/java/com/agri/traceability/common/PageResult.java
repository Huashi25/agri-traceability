package com.agri.traceability.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long pageNum;
    private long pageSize;
    private long pages;
    private List<T> list;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.total = page.getTotal();
        result.pageNum = page.getCurrent();
        result.pageSize = page.getSize();
        result.pages = page.getPages();
        result.list = page.getRecords();
        return result;
    }
}
