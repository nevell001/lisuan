package com.cashier.model;

import java.util.List;

/**
 * 分页查询结果
 * @param <T> 数据类型
 */
public class PageResult<T> {
    private List<T> data;
    private int pageNum;
    private int pageSize;
    private long total;
    private int pages;

    public PageResult() {
    }

    public PageResult(List<T> data, int pageNum, int pageSize, long total) {
        this.data = data;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = (int) Math.ceil((double) total / pageSize);
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
        if (pageSize > 0) {
            this.pages = (int) Math.ceil((double) total / pageSize);
        }
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    /**
     * 是否有下一页
     */
    public boolean hasNextPage() {
        return pageNum < pages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPreviousPage() {
        return pageNum > 1;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", total=" + total +
                ", pages=" + pages +
                ", dataSize=" + (data != null ? data.size() : 0) +
                '}';
    }
}
