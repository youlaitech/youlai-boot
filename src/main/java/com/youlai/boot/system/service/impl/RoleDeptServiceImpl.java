package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.system.mapper.RoleDeptMapper;
import com.youlai.boot.system.model.entity.RoleDept;
import com.youlai.boot.system.service.RoleDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 角色部门关联服务实现
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Service
@RequiredArgsConstructor
public class RoleDeptServiceImpl extends ServiceImpl<RoleDeptMapper, RoleDept> implements RoleDeptService {

    @Override
    public List<Long> getDeptIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return this.baseMapper.getDeptIdsByRoleId(roleId);
    }

    @Override
    public List<Long> getDeptIdsByRoleCodes(List<String> roleCodes) {
        if (CollectionUtil.isEmpty(roleCodes)) {
            return Collections.emptyList();
        }
        return this.baseMapper.getDeptIdsByRoleCodes(roleCodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleDepts(Long roleId, List<Long> deptIds) {
        if (roleId == null || CollectionUtil.isEmpty(deptIds)) {
            return;
        }
        // 先删除原有关联
        this.remove(new LambdaQueryWrapper<RoleDept>().eq(RoleDept::getRoleId, roleId));
        // 批量插入新关联
        List<RoleDept> roleDepts = deptIds.stream()
                .map(deptId -> new RoleDept(roleId, deptId))
                .toList();
        this.saveBatch(roleDepts);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        this.remove(new LambdaQueryWrapper<RoleDept>().eq(RoleDept::getRoleId, roleId));
    }

}
