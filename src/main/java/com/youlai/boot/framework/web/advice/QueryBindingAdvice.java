package com.youlai.boot.framework.web.advice;

import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * 保留重复排序参数中的字段与方向
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@ControllerAdvice
public class QueryBindingAdvice {

    /**
     * 将每个 sort 参数作为完整规则，避免按逗号拆成两个数组元素
     */
    @InitBinder
    public void bindSort(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, "sort", new StringArrayPropertyEditor(null));
    }
}
