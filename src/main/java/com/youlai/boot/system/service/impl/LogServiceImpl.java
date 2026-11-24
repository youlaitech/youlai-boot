package com.youlai.boot.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.youlai.boot.system.mapper.LogMapper;
import com.youlai.boot.system.model.dto.VisitCountDTO;
import com.youlai.boot.system.model.entity.SysLog;
import com.youlai.boot.system.model.query.LogQuery;
import com.youlai.boot.system.model.vo.LogPageVO;
import com.youlai.boot.system.model.vo.VisitSummaryVO;
import com.youlai.boot.system.model.vo.VisitTrendPointVO;
import com.youlai.boot.system.model.vo.VisitTrendVO;
import com.youlai.boot.system.service.LogService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统日志服务实现
 *
 * @author Ray.Hao
 * @since 2.10.0
 */
@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, SysLog> implements LogService {

    /**
     * 获取日志分页列表
     *
     * @param queryParams 查询条件
     */
    @Override
    public Page<LogPageVO> getLogPage(LogQuery queryParams) {
        return baseMapper.getLogPage(Page.of(queryParams.getPageNum(), queryParams.getPageSize()), queryParams);
    }

    /**
     * 获取访问趋势
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     */
    @Override
    public VisitTrendVO getVisitTrend(LocalDate startDate, LocalDate endDate) {
        List<String> dates = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            dates.add(cursor.toString());
            cursor = cursor.plusDays(1);
        }

        String startTime = dates.get(0) + " 00:00:00";
        String endTime = dates.get(dates.size() - 1) + " 23:59:59";
        List<VisitCountDTO> pvCounts = baseMapper.getPvCounts(startTime, endTime);
        List<VisitCountDTO> ipCounts = baseMapper.getIpCounts(startTime, endTime);
        Map<String, Integer> pvMap = pvCounts.stream()
                .collect(Collectors.toMap(VisitCountDTO::getDate, VisitCountDTO::getCount));
        Map<String, Integer> uvMap = ipCounts.stream()
                .collect(Collectors.toMap(VisitCountDTO::getDate, VisitCountDTO::getCount));

        List<VisitTrendPointVO> points = new ArrayList<>();
        for (String date : dates) {
            points.add(new VisitTrendPointVO(
                    date,
                    pvMap.getOrDefault(date, 0),
                    uvMap.getOrDefault(date, 0)
            ));
        }

        VisitTrendVO result = new VisitTrendVO();
        result.setStartDate(startDate.toString());
        result.setEndDate(endDate.toString());
        result.setPoints(points);
        return result;
    }

    /**
     * 获取访问汇总
     */
    @Override
    public VisitSummaryVO getVisitSummary() {
        VisitSummaryVO result = new VisitSummaryVO();
        VisitSummaryVO uvStats = baseMapper.getUvStats();
        if (uvStats != null) {
            result.setTodayUv(uvStats.getTodayUv());
            result.setYesterdayUv(uvStats.getYesterdayUv());
            result.setTotalUv(uvStats.getTotalUv());
            result.setUvGrowthRate(uvStats.getUvGrowthRate());
        }
        VisitSummaryVO pvStats = baseMapper.getPvStats();
        if (pvStats != null) {
            result.setTodayPv(pvStats.getTodayPv());
            result.setYesterdayPv(pvStats.getYesterdayPv());
            result.setTotalPv(pvStats.getTotalPv());
            result.setPvGrowthRate(pvStats.getPvGrowthRate());
        }
        return result;
    }
}
