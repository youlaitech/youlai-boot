package com.youlai.boot.framework.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.youlai.boot.framework.security.util.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充
 *
 * <p>自动填充 createTime/updateTime（时间戳）和 createBy/updateBy（操作人ID）</p>
 * <p>实体字段需标注 {@code @TableField(fill = FieldFill.INSERT)} 或 {@code FieldFill.INSERT_UPDATE} 才会触发</p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Component
public class MetaFieldFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "createBy", SecurityUtils::getUserId, Long.class);
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updateBy", SecurityUtils::getUserId, Long.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updateBy", SecurityUtils::getUserId, Long.class);
    }
}
