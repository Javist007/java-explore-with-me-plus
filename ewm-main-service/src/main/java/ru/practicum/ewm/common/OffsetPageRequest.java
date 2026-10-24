package ru.practicum.ewm.common;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Пагинация по смещению: from означает, сколько элементов пропустить, а не номер страницы.
 */
public final class OffsetPageRequest implements Pageable {

    private final long offset;
    private final int size;
    private final Sort sort;

    private OffsetPageRequest(long offset, int size, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("from не может быть отрицательным");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size должен быть больше 0");
        }
        this.offset = offset;
        this.size = size;
        this.sort = sort;
    }

    public static OffsetPageRequest of(int from, int size, Sort sort) {
        return new OffsetPageRequest(from, size, sort);
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / size);
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + size, size, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageRequest(Math.max(0, offset - size), size, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, size, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest((long) pageNumber * size, size, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}