package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.system.model.entity.UserNotice;
import com.youlai.boot.system.model.query.NoticeQuery;
import com.youlai.boot.system.model.vo.NoticePageVO;
import com.youlai.boot.system.model.vo.UserNoticePageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Options;
import java.util.List;


/**
 * 用户公告状态
 *
 * @author youlaitech
 * @since 2024-08-28 16:56
 */
@Mapper
public interface UserNoticeMapper extends BaseMapper<UserNotice> {
    /**
     * 按用户 ID 游标分批查询公告接收人
     */
    @Select("SELECT DISTINCT user_id FROM sys_user_notice WHERE notice_id = #{noticeId} "
            + "AND is_deleted = 0 AND user_id > #{afterUserId} ORDER BY user_id LIMIT #{limit}")
    @Options(timeout = 5)
    List<Long> findRecipientsAfter(@Param("noticeId") Long noticeId, @Param("afterUserId") long afterUserId,
                                   @Param("limit") int limit);

    /**
     * 分页获取我的通知公告
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return 通知公告分页列表
     */
    IPage<UserNoticePageVO> getMyNoticePage(Page<NoticePageVO> page, @Param("queryParams") NoticeQuery queryParams);
}
