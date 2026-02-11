package com.youlai.boot.core.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应结构体
 *
 * @author Ray.Hao
 * @since 2022/2/18
 */
@Data
public class PageResult<T> implements Serializable {

    private String code;

    private String msg;

    private PageData<T> data;

    /**
     * 构建分页结果（MyBatis-Plus {@link IPage}）。
     *
     * <p>data 为当前页记录列表；page 提供分页元信息。</p>
     */
    public static <T> PageResult<T> success(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());

        List<T> records =
                (page == null || page.getRecords() == null)
                        ? Collections.emptyList()
                        : page.getRecords();
        PageData<T> pageData = new PageData<>();
        pageData.setList(records);
        pageData.setTotal(page != null ? page.getTotal() : 0L);
        result.setData(pageData);

        return result;
    }

    /**
     * 构建列表结果（无分页）。
     *
     * <p>page 置为 null，用于与分页返回区分。</p>
     */
    public static <T> PageResult<T> success(List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        PageData<T> pageData = new PageData<>();
        pageData.setList(list != null ? list : Collections.emptyList());
        pageData.setTotal(0L);
        result.setData(pageData);
        return result;
    }

    @Data
    public static class PageData<T> {

        private List<T> list;

        private long total;
    }

}
