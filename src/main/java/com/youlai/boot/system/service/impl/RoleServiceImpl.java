package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.enums.DataScopeEnum;
import com.youlai.boot.core.exception.BusinessException;
import com.youlai.boot.system.converter.RoleConverter;
import com.youlai.boot.system.mapper.RoleMapper;
import com.youlai.boot.system.model.entity.Role;
import com.youlai.boot.system.model.entity.RoleMenu;
import com.youlai.boot.system.model.form.RoleForm;
import com.youlai.boot.system.model.query.RoleQuery;
import com.youlai.boot.system.model.vo.RolePageVO;
import com.youlai.boot.common.constant.SystemConstants;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.security.util.SecurityUtils;
import com.youlai.boot.system.service.RoleDeptService;
import com.youlai.boot.system.service.RoleMenuService;
import com.youlai.boot.system.service.RoleService;
import com.youlai.boot.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 角色业务实现类
 *
 * @author haoxr
 * @since 2022/6/3
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMenuService roleMenuService;
    private final RoleDeptService roleDeptService;
    private final UserRoleService userRoleService;
    private final RoleConverter roleConverter;

    /**
     * 角色分页列表
     *
     * @param queryParams 角色查询参数
     * @return {@link Page< RolePageVO >} – 角色分页列表
     */
    @Override
    public Page<RolePageVO> getRolePage(RoleQuery queryParams) {
        // 查询参数
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        String keywords = queryParams.getKeywords();

        // 查询数据
        Page<Role> rolePage = this.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Role>()
                        .and(StrUtil.isNotBlank(keywords),
                                wrapper ->
                                        wrapper.like(Role::getName, keywords)
                                                .or()
                                                .like(Role::getCode, keywords)
                        )
                        .ne(!SecurityUtils.isRoot(), Role::getCode, SystemConstants.ROOT_ROLE_CODE) // 非超级管理员不显示超级管理员角色
                        .orderByAsc(Role::getSort).orderByDesc(Role::getCreateTime).orderByDesc(Role::getUpdateTime)
        );

        // 实体转换
        return roleConverter.toPageVo(rolePage);
    }

    /**
     * 角色下拉列表
     *
     * @return {@link List<Option>} – 角色下拉列表
     */
    @Override
    public List<Option<Long>> listRoleOptions() {
        // 查询数据
        List<Role> roleList = this.list(new LambdaQueryWrapper<Role>()
                .ne(!SecurityUtils.isRoot(), Role::getCode, SystemConstants.ROOT_ROLE_CODE)
                .select(Role::getId, Role::getName)
                .orderByAsc(Role::getSort)
        );

        // 实体转换
        return roleConverter.toOptions(roleList);
    }

    /**
     * 保存角色
     *
     * @param roleForm 角色表单数据
     * @return {@link Boolean}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRole(RoleForm roleForm) {

        Long roleId = roleForm.getId();

        // 编辑角色时，判断角色是否存在
        Role oldRole = null;
        if (roleId != null) {
            oldRole = this.getById(roleId);
            Assert.isTrue(oldRole != null, "角色不存在");
        }

        String roleCode = roleForm.getCode();
        long count = this.count(new LambdaQueryWrapper<Role>()
                .ne(roleId != null, Role::getId, roleId)
                .and(wrapper ->
                        wrapper.eq(Role::getCode, roleCode).or().eq(Role::getName, roleForm.getName())
                ));
        Assert.isTrue(count == 0, "角色名称或角色编码已存在，请修改后重试！");

        // 实体转换
        Role role = roleConverter.toEntity(roleForm);

        boolean result = this.saveOrUpdate(role);
        if (result) {
            // 保存自定义数据权限部门
            Long savedRoleId = role.getId();
            if (DataScopeEnum.CUSTOM.getValue().equals(roleForm.getDataScope())) {
                // 自定义数据权限时，保存角色部门关联
                roleDeptService.saveRoleDepts(savedRoleId, roleForm.getDeptIds());
            } else {
                // 非自定义数据权限时，删除原有部门关联
                roleDeptService.deleteByRoleId(savedRoleId);
            }

            // 判断角色编码或状态是否修改，修改了则刷新权限缓存
            if (oldRole != null
                    && (
                    !StrUtil.equals(oldRole.getCode(), roleCode) ||
                            !ObjectUtil.equals(oldRole.getStatus(), roleForm.getStatus())
            )) {
                roleMenuService.refreshRolePermsCache(oldRole.getCode(), roleCode);
            }
        }
        return result;
    }

    /**
     * 获取角色表单数据
     *
     * @param roleId 角色ID
     * @return {@link RoleForm} – 角色表单数据
     */
    @Override
    public RoleForm getRoleForm(Long roleId) {
        Role entity = this.getById(roleId);
        RoleForm roleForm = roleConverter.toForm(entity);
        // 如果是自定义数据权限，查询关联的部门ID列表
        if (roleForm != null && DataScopeEnum.CUSTOM.getValue().equals(roleForm.getDataScope())) {
            List<Long> deptIds = roleDeptService.getDeptIdsByRoleId(roleId);
            roleForm.setDeptIds(deptIds);
        }
        return roleForm;
    }

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态(1:启用；0:禁用)
     * @return {@link Boolean}
     */
    @Override
    public boolean updateRoleStatus(Long roleId, Integer status) {

        Role role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        role.setStatus(status);
        boolean result = this.updateById(role);
        if (result) {
            // 刷新角色的权限缓存
            roleMenuService.refreshRolePermsCache(role.getCode());
        }
        return result;
    }

    /**
     * 批量删除角色
     *
     * @param ids 角色ID，多个使用英文逗号(,)分割
     */
    @Override
    public void deleteRoles(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的角色ID不能为空");
        List<Long> roleIds = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();

        for (Long roleId : roleIds) {
            Role role = this.getById(roleId);
            Assert.isTrue(role != null, "角色不存在");

            // 判断角色是否被用户关联
            boolean isRoleAssigned = userRoleService.hasAssignedUsers(roleId);
            Assert.isTrue(!isRoleAssigned, "角色【{}】已分配用户，请先解除关联后删除", role.getName());

            boolean deleteResult = this.removeById(roleId);
            if (deleteResult) {
                // 删除成功，刷新权限缓存
                roleMenuService.refreshRolePermsCache(role.getCode());
            }
        }
    }

    /**
     * 获取角色的菜单ID集合
     *
     * @param roleId 角色ID
     * @return 菜单ID集合(包括按钮权限ID)
     */
    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuService.listMenuIdsByRoleId(roleId);
    }

    /**
     * 修改角色的资源权限
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID集合
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "menu", key = "'routes'")
    public void assignMenusToRole(Long roleId, List<Long> menuIds) {
        Role role = this.getById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        // 删除角色菜单
        roleMenuService.remove(
                new LambdaQueryWrapper<RoleMenu>()
                        .eq(RoleMenu::getRoleId, roleId)
        );
        // 新增角色菜单
        if (CollectionUtil.isNotEmpty(menuIds)) {
            List<RoleMenu> roleMenus = menuIds
                    .stream()
                    .map(menuId -> new RoleMenu(roleId, menuId))
                    .toList();
            roleMenuService.saveBatch(roleMenus);
        }

        // 刷新角色的权限缓存
        roleMenuService.refreshRolePermsCache(role.getCode());
    }

    /**
     * 获取最大范围的数据权限
     *
     * @param roles 角色编码集合
     * @return {@link Integer} – 数据权限范围
     */
    @Override
    public Integer getMaximumDataScope(Set<String> roles) {
        Integer dataScope = this.baseMapper.getMaximumDataScope(roles);
        return dataScope;
    }

    /**
     * 获取角色的部门ID列表（自定义数据权限）
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    @Override
    public List<Long> getRoleDeptIds(Long roleId) {
        return roleDeptService.getDeptIdsByRoleId(roleId);
    }

    /**
     * 获取用户所有角色的数据权限列表
     * <p>
     * 用于实现多角色数据权限合并（并集策略）
     *
     * @param roleCodes 角色编码集合
     * @return 角色数据权限列表
     */
    @Override
    public List<RoleDataScope> getRoleDataScopes(Set<String> roleCodes) {
        if (CollectionUtil.isEmpty(roleCodes)) {
            return List.of();
        }

        // 获取角色的数据权限信息
        List<Map<String, Object>> roleDataScopeList = this.baseMapper.getRoleDataScopeList(roleCodes);
        if (CollectionUtil.isEmpty(roleDataScopeList)) {
            return List.of();
        }

        // 获取所有自定义数据权限的角色编码
        List<String> customRoleCodes = roleDataScopeList.stream()
                .filter(map -> DataScopeEnum.CUSTOM.getValue().equals(map.get("data_scope")))
                .map(map -> (String) map.get("code"))
                .toList();

        // 批量获取自定义角色的部门ID
        Map<String, List<Long>> customDeptIdsMap = new java.util.HashMap<>();
        if (CollectionUtil.isNotEmpty(customRoleCodes)) {
            // 查询每个角色关联的部门ID
            for (String roleCode : customRoleCodes) {
                // 根据角色编码获取角色ID
                Role role = this.getOne(new LambdaQueryWrapper<Role>()
                        .eq(Role::getCode, roleCode)
                        .select(Role::getId));
                if (role != null) {
                    List<Long> deptIds = roleDeptService.getDeptIdsByRoleId(role.getId());
                    customDeptIdsMap.put(roleCode, deptIds);
                }
            }
        }

        // 构建角色数据权限列表
        return roleDataScopeList.stream()
                .map(map -> {
                    String code = (String) map.get("code");
                    Integer dataScope = (Integer) map.get("data_scope");
                    if (DataScopeEnum.CUSTOM.getValue().equals(dataScope)) {
                        return RoleDataScope.custom(code, customDeptIdsMap.getOrDefault(code, List.of()));
                    }
                    return new RoleDataScope(code, dataScope, null);
                })
                .toList();
    }

}
