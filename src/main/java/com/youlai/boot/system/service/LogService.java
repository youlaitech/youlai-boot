package com.youlai.boot.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.system.model.entity.SysLog;
import com.baomidou.mybatisplus.spring.service.IService;
import com.youlai.boot.system.model.query.LogQuery;
import com.youlai.boot.system.model.vo.LogPageVO;
import com.youlai.boot.system.model.vo.VisitSummaryVO;
import com.youlai.boot.system.model.vo.VisitTrendVO;

import java.time.LocalDate;

/**
 * 系统日志 服务接口
 *
 * @author Ray.Hao
 * @since 2.10.0
 */
public interface LogService extends IService<SysLog> {

    /**
     * 获取日志分页列表
     */
    Page<LogPageVO> getLogPage(LogQuery queryParams);

    /**
     * 获取访问趋势
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     */
    VisitTrendVO getVisitTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 获取访问汇总
     */
    VisitSummaryVO getVisitSummary();
}
