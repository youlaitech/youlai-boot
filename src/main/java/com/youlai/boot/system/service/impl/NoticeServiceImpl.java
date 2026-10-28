package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.framework.security.util.SecurityUtils;
import com.youlai.boot.support.realtime.RealtimeService;
import com.youlai.boot.system.event.NoticeChangedEvent;
import com.youlai.boot.system.mapper.UserNoticeMapper;
import com.youlai.boot.system.converter.NoticeConverter;
import com.youlai.boot.system.enums.NoticePublishStatusEnum;
import com.youlai.boot.system.enums.NoticeTargetEnum;
import com.youlai.boot.system.mapper.NoticeMapper;
import com.youlai.boot.system.model.vo.NoticePageVO;
import com.youlai.boot.system.model.vo.UserNoticePageVO;
import com.youlai.boot.system.model.vo.NoticeDetailVO;
import com.youlai.boot.system.model.entity.Notice;
import com.youlai.boot.system.model.entity.UserNotice;
import com.youlai.boot.system.model.entity.SysUser;
import com.youlai.boot.system.model.form.NoticeForm;
import com.youlai.boot.system.model.query.NoticeQuery;
import com.youlai.boot.system.service.NoticeService;
import com.youlai.boot.system.service.UserNoticeService;
import com.youlai.boot.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 通知公告服务实现类
 *
 * @author Theo
 * @since 2024-08-27 10:31
 */
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    private final NoticeConverter noticeConverter;
    private final UserNoticeService userNoticeService;
    private final UserService userService;
    private final ApplicationEventPublisher events;
    private final RealtimeService realtime;
    private final UserNoticeMapper userNoticeMapper;

    /**
     * 获取通知公告分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<NoticePageVO>} 通知公告分页列表
     */
    @Override
    public IPage<NoticePageVO> getNoticePage(NoticeQuery queryParams) {
        return this.baseMapper.getNoticePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
    }

    /**
     * 获取通知公告表单数据
     *
     * @param id 通知公告ID
     * @return {@link NoticeForm} 通知公告表单对象
     */
    @Override
    public NoticeForm getNoticeFormData(Long id) {
        Notice entity = this.getById(id);
        return noticeConverter.toForm(entity);
    }

    /**
     * 新增通知公告
     *
     * @param formData 通知公告表单对象
     * @return {@link Boolean} 是否新增成功
     */
    @Override
    public boolean saveNotice(NoticeForm formData) {

        if (NoticeTargetEnum.SPECIFIED.getValue().equals(formData.getTargetType())) {
            List<String> targetUserIdList = formData.getTargetUserIds();
            if (CollectionUtil.isEmpty(targetUserIdList)) {
                throw new BusinessException("推送指定用户不能为空");
            }
        }
        Notice entity = noticeConverter.toEntity(formData);
        return this.save(entity);
    }

    /**
     * 更新通知公告
     *
     * @param id       通知公告ID
     * @param formData 通知公告表单对象
     * @return {@link Boolean} 是否更新成功
     */
    @Override
    public boolean updateNotice(Long id, NoticeForm formData) {
        if (NoticeTargetEnum.SPECIFIED.getValue().equals(formData.getTargetType())) {
            List<String> targetUserIdList = formData.getTargetUserIds();
            if (CollectionUtil.isEmpty(targetUserIdList)) {
                throw new BusinessException("推送指定用户不能为空");
            }
        }

        Notice entity = noticeConverter.toEntity(formData);
        return this.updateById(entity);
    }

    /**
     * 删除通知公告
     *
     * @param ids 通知公告ID，多个以英文逗号(,)分割
     * @return {@link Boolean} 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteNotices(String ids) {
        if (StrUtil.isBlank(ids)) {
            throw new BusinessException("删除的通知公告数据为空");
        }

        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        boolean isRemoved = this.removeByIds(idList);
        if (isRemoved) {
            // 删除通知公告的同时，需要删除通知公告对应的用户通知状态
            userNoticeService.remove(new LambdaQueryWrapper<UserNotice>().in(UserNotice::getNoticeId, idList));
        }
        return isRemoved;
    }

    /**
     * 发布通知公告
     *
     * @param id 通知公告ID
     * @return 是否发布成功
     */
    @Override
    @Transactional
    public boolean publishNotice(Long id) {
        Notice notice = this.getById(id);
        if (notice == null) {
            throw new BusinessException("通知公告不存在");
        }

        if (NoticePublishStatusEnum.PUBLISHED.getValue().equals(notice.getPublishStatus())) {
            throw new BusinessException("通知公告已发布");
        }

        Integer targetType = notice.getTargetType();
        String targetUserIds = notice.getTargetUserIds();
        if (NoticeTargetEnum.SPECIFIED.getValue().equals(targetType)
                && StrUtil.isBlank(targetUserIds)) {
            throw new BusinessException("推送指定用户不能为空");
        }

        notice.setPublishStatus(NoticePublishStatusEnum.PUBLISHED.getValue());
        notice.setPublisherId(SecurityUtils.getUserId());
        notice.setPublishTime(LocalDateTime.now());
        boolean publishResult = this.updateById(notice);

        if (publishResult) {
            // 发布通知公告的同时，删除该通告之前的用户通知数据，因为可能是重新发布
            userNoticeService.remove(
                    new LambdaQueryWrapper<UserNotice>().eq(UserNotice::getNoticeId, id)
            );

            // 添加新的用户通知数据
            List<String> targetUserIdList = null;
            if (NoticeTargetEnum.SPECIFIED.getValue().equals(targetType)) {
                targetUserIdList = Arrays.asList(targetUserIds.split(","));
            }

            List<SysUser> targetUserList = userService.list(
                    new LambdaQueryWrapper<SysUser>()
                            // 如果是指定用户，则筛选出指定用户
                            .in(
                                    NoticeTargetEnum.SPECIFIED.getValue().equals(targetType),
                                    SysUser::getId,
                                    targetUserIdList
                            )
            );

            List<UserNotice> userNoticeList = targetUserList.stream().map(user -> {
                UserNotice userNotice = new UserNotice();
                userNotice.setNoticeId(id);
                userNotice.setUserId(user.getId());
                userNotice.setIsRead(0);
                return userNotice;
            }).toList();

            if (CollectionUtil.isNotEmpty(userNoticeList)) {
                userNoticeService.saveBatch(userNoticeList);
            }

            events.publishEvent(new NoticeChangedEvent(id, "published"));
        }
        return publishResult;
    }

    /**
     * 撤回通知公告
     *
     * @param id 通知公告ID
     * @return 是否撤回成功
     */
    @Override
    @Transactional
    public boolean revokeNotice(Long id) {
        Notice notice = this.getById(id);
        if (notice == null) {
            throw new BusinessException("通知公告不存在");
        }

        if (!NoticePublishStatusEnum.PUBLISHED.getValue().equals(notice.getPublishStatus())) {
            throw new BusinessException("通知公告未发布或已撤回");
        }

        notice.setPublishStatus(NoticePublishStatusEnum.REVOKED.getValue());
        notice.setRevokeTime(LocalDateTime.now());

        boolean revokeResult = this.updateById(notice);

        if (revokeResult) {
            // 保留受众关系，以便定向通知撤回；查询接口按发布状态屏蔽正文
            events.publishEvent(new NoticeChangedEvent(id, "revoked"));
        }
        return revokeResult;
    }

    /**
     *
     * @param id 通知公告ID
     * @return NoticeDetailVO 通知公告详情
     */
    @Override
    public NoticeDetailVO getNoticeDetail(Long id) {
        Long userId = SecurityUtils.getUserId();
        NoticeDetailVO detail = this.baseMapper.getNoticeDetail(id, userId);
        if (detail == null) throw new BusinessException("通知不存在或无权查看");
        // 只有已发布通知的接收者可以读取正文和修改阅读状态
        userNoticeService.update(new LambdaUpdateWrapper<UserNotice>()
                .eq(UserNotice::getNoticeId, id)
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getIsRead, 0)
                .set(UserNotice::getIsRead, 1)
        );
        return detail;
    }

    /**
     * 事务提交后再通知收件人，避免客户端补拉到尚未提交的状态
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNoticeChanged(NoticeChangedEvent event) {
        realtime.sendBatches("notice.changed", Map.of("noticeId", event.noticeId().toString(), "action", event.action()),
                (afterUserId, limit) -> userNoticeMapper.findRecipientsAfter(event.noticeId(), afterUserId, limit));
    }

    @Override
    public NoticeDetailVO getNoticePreview(Long id) {
        NoticeDetailVO detail = this.baseMapper.getNoticePreview(id);
        if (detail == null) throw new BusinessException("通知不存在");
        return detail;
    }

    /**
     * 获取当前登录用户的通知公告列表
     *
     * @param queryParams 查询参数
     * @return 通知公告分页列表
     */
    @Override
    public IPage<UserNoticePageVO> getMyNoticePage(
            NoticeQuery queryParams
    ) {
        Long userId = SecurityUtils.getUserId();
        queryParams.setUserId(userId);
        return userNoticeService.getMyNoticePage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
    }

}
