package com.youlai.boot.system.converter;

import com.youlai.boot.framework.web.log.OperationLog;
import com.youlai.boot.system.model.entity.SysLog;
import org.mapstruct.Mapper;

/**
 * 操作日志转换器
 *
 * @author Ray.Hao
 * @since 4.7.0
 */
@Mapper(componentModel = "spring")
public interface LogConverter {

    /**
     * 操作日志载荷转为日志实体
     *
     * @param operationLog 操作日志载荷
     * @return 日志实体
     */
    SysLog toEntity(OperationLog operationLog);
}
