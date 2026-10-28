package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.system.model.entity.Notice;
import com.youlai.boot.system.model.query.NoticeQuery;
import com.youlai.boot.system.model.vo.NoticePageVO;
import com.youlai.boot.system.model.vo.NoticeDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 通知公告Mapper接口
 *
 * @author youlaitech
 * @since 2024-08-27 10:31
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    /**
     * 获取通知公告分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return 通知公告分页数据
     */
    Page<NoticePageVO> getNoticePage(Page<NoticePageVO> page, NoticeQuery queryParams);

    /**
     * 获取阅读时通知公告详情
     *
     * @param id 通知公告ID
     * @return 通知公告详情
     */
    NoticeDetailVO getNoticeDetail(@Param("id") Long id, @Param("userId") Long userId);

    NoticeDetailVO getNoticePreview(@Param("id") Long id);
}
