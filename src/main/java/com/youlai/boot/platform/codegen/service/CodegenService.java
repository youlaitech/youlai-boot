package com.youlai.boot.platform.codegen.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.platform.codegen.model.query.TablePageQuery;
import com.youlai.boot.platform.codegen.model.vo.CodegenPreviewVo;
import com.youlai.boot.platform.codegen.model.vo.TablePageVo;

import java.util.List;

/**
 * 代码生成配置接口
 *
 * @author Ray
 * @since 2.10.0
 */
public interface CodegenService {

    /**
     * 获取数据表分页列表
     *
     * @param queryParams 查询参数
     * @return
     */
    Page<TablePageVo> getTablePage(TablePageQuery queryParams);

    /**
     * 获取预览生成代码
     *
     * @param tableName 表名
     * @return
     */
    List<CodegenPreviewVo> getCodegenPreviewData(String tableName, String pageType);

    /**
     * 下载代码
     * @param tableNames 表名
     * @return
     */
    byte[] downloadCode(String[] tableNames, String pageType);
}
