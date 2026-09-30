package com.youlai.boot.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色部门关联实体
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@TableName("sys_role_dept")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDept {

    // 角色ID
    private Long roleId;

    // 部门ID
    private Long deptId;

}
