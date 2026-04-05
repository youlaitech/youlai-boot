package com.youlai.boot.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.system.mapper.LogMapper;
import com.youlai.boot.system.model.dto.VisitCountDTO;
import com.youlai.boot.system.model.vo.VisitOverviewVO;
import com.youlai.boot.system.model.entity.SysLog;
import com.youlai.boot.system.model.query.LogQuery;
import com.youlai.boot.system.model.vo.LogPageVO;
import com.youlai.boot.system.model.vo.VisitOverviewVO;
import com.youlai.boot.system.model.vo.VisitTrendVO;
import com.youlai.boot.system.service.LogService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统日志 服务实现类
 *
 * @author Ray.Hao
 * @since 2.10.0
 */
@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, SysLog>
        implements LogService {

    /**
     * 获取日志分页列表
     *
     * @param queryParams 查询参数
     * @return 日志分页列表
     */
    @Override
    public Page<LogPageVO> getLogPage(LogQuery queryParams) {
        return this.baseMapper.getLogPage(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams);
    }

    /**
     * 获取访问趋势
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return
     */
    @Override
    public VisitTrendVO getVisitTrend(LocalDate startDate, LocalDate endDate) {
        VisitTrendVO visitTrend = new VisitTrendVO();
        List<String> dates = new ArrayList<>();

        // 获取日期范围内的日期
        while (!startDate.isAfter(endDate)) {
            dates.add(startDate.toString());
            startDate = startDate.plusDays(1);
        }
        visitTrend.setDates(dates);

        // 获取访问量和访问 IP 数的统计数据
        List<VisitCountDTO> pvCounts = this.baseMapper.getPvCounts(dates.get(0) + " 00:00:00", dates.get(dates.size() - 1) + " 23:59:59");
        List<VisitCountDTO> ipCounts = this.baseMapper.getIpCounts(dates.get(0) + " 00:00:00", dates.get(dates.size() - 1) + " 23:59:59");

        // 将统计数据转换为 Map
        Map<String, Integer> pvMap = pvCounts.stream().collect(Collectors.toMap(VisitCountDTO::getDate, VisitCountDTO::getCount));
        Map<String, Integer> uvMap = ipCounts.stream().collect(Collectors.toMap(VisitCountDTO::getDate, VisitCountDTO::getCount));

        // 匹配日期和访问量/访客数
        List<Integer> pvList = new ArrayList<>();
        List<Integer> uvList = new ArrayList<>();

        for (String date : dates) {
            pvList.add(pvMap.getOrDefault(date, 0));
            uvList.add(uvMap.getOrDefault(date, 0));
        }

        visitTrend.setPvList(pvList);
        visitTrend.setUvList(uvList);

        return visitTrend;
    }

    /**
     * 访问量统计
     */
    @Override
    public VisitOverviewVO getVisitStats() {
        VisitOverviewVO result = new VisitOverviewVO();

        // 访客数统计(UV)
        VisitOverviewVO uvStats = this.baseMapper.getUvStats();
        if(uvStats!=null){
            result.setTodayUvCount(uvStats.getTodayUvCount());
            result.setTotalUvCount(uvStats.getTotalUvCount());
            result.setUvGrowthRate(uvStats.getUvGrowthRate());
        }

        // 浏览量统计(PV)
        VisitOverviewVO pvStats = this.baseMapper.getPvStats();
        if(pvStats!=null){
            result.setTodayPvCount(pvStats.getTodayPvCount());
            result.setTotalPvCount(pvStats.getTotalPvCount());
            result.setPvGrowthRate(pvStats.getPvGrowthRate());
        }

        return result;
    }

}




