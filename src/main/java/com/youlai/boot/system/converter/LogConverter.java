package com.youlai.boot.system.converter;

import com.youlai.boot.framework.web.log.OperationLog;
import com.youlai.boot.system.model.entity.SysLog;
import org.mapstruct.Mapper;

/**
 * 系统日志对象转换器
 *
 * @author Ray.Hao
 * @since 0.0.1
 */
@Mapper(componentModel = "spring")
public interface LogConverter {

    SysLog toEntity(OperationLog operationLog);
}
