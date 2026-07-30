package com.youlai.boot.common.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.util.IPUtils;
import com.youlai.boot.framework.security.util.SecurityUtils;
import com.youlai.boot.system.model.entity.SysLog;
import com.youlai.boot.system.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * 操作日志切面
 * <p>@Log 标注的方法执行后异步写入 sys_log 表</p>
 *
 * @author Ray.Hao
 * @since 0.0.1
 */
@Aspect
@Component
@Slf4j
public class LogAspect {

    private static final int MAX_ERROR_MSG_LENGTH = 1000;

    private final LogService logService;
    private final Executor operationLogExecutor;

    public LogAspect(
            LogService logService,
            @Qualifier("operationLogExecutor") Executor operationLogExecutor
    ) {
        this.logService = logService;
        this.operationLogExecutor = operationLogExecutor;
    }

    @Pointcut("@annotation(logAnnotation)")
    public void pointcut(Log logAnnotation) {
    }

    @Around(value = "pointcut(logAnnotation)", argNames = "jp,logAnnotation")
    public Object around(ProceedingJoinPoint jp, Log logAnnotation) throws Throwable {
        long start = System.currentTimeMillis();
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        Throwable failure = null;

        try {
            return jp.proceed();
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            try {
                SysLog entity = buildEntity(logAnnotation, elapsed, failure, userId, username);
                if (entity != null) {
                    operationLogExecutor.execute(() -> {
                        try {
                            logService.save(entity);
                        } catch (Exception e) {
                            log.error("保存操作日志失败 title={} uri={}", entity.getTitle(), entity.getRequestUri(), e);
                        }
                    });
                }
            } catch (Exception ex) {
                log.error("构建操作日志失败", ex);
            }
        }
    }

    private SysLog buildEntity(Log ann, long elapsed, Throwable failure, Long userId, String username) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;

        HttpServletRequest req = attrs.getRequest();
        UserAgent ua = UserAgentUtil.parse(req.getHeader("User-Agent"));
        String ip = IPUtils.getIpAddr(req);

        LogModuleEnum module = ann.module();
        ActionTypeEnum action = ann.value();

        SysLog entity = new SysLog();
        entity.setModule(module);
        entity.setActionType(action);
        entity.setTitle(StrUtil.blankToDefault(ann.title(), module.getLabel() + "-" + action.getLabel()));
        entity.setContent(ann.content());
        entity.setOperatorId(userId);
        entity.setOperatorName(username);
        entity.setRequestUri(req.getRequestURI());
        entity.setRequestMethod(req.getMethod());
        entity.setIp(ip);
        entity.setDevice(getDevice(ua));
        entity.setOs(getOs(ua));
        entity.setBrowser(getBrowser(ua));
        entity.setStatus(failure == null ? 1 : 0);
        entity.setErrorMsg(failure == null ? null : truncate(failure.getMessage(), MAX_ERROR_MSG_LENGTH));
        entity.setExecutionTime((int) Math.min(elapsed, Integer.MAX_VALUE));
        entity.setCreateTime(LocalDateTime.now());

        IpRegion region = parseRegion(ip);
        entity.setProvince(region.province);
        entity.setCity(region.city);

        return entity;
    }

    private IpRegion parseRegion(String ip) {
        String region = IPUtils.getRegion(ip);
        if (StrUtil.isBlank(region)) return IpRegion.EMPTY;
        String[] parts = region.split("\\|");
        if (parts.length < 4) return IpRegion.EMPTY;
        return new IpRegion(StrUtil.blankToDefault(parts[2], null), StrUtil.blankToDefault(parts[3], null));
    }

    private String truncate(String msg, int maxLen) {
        if (msg == null) return null;
        return msg.length() <= maxLen ? msg : msg.substring(0, maxLen);
    }

    private String getOs(UserAgent ua) {
        return Optional.ofNullable(ua).map(UserAgent::getOs).map(os -> os.getName()).orElse(null);
    }

    private String getDevice(UserAgent ua) {
        return Optional.ofNullable(ua).map(UserAgent::getPlatform).map(p -> p.getName()).orElse(null);
    }

    private String getBrowser(UserAgent ua) {
        return Optional.ofNullable(ua).map(UserAgent::getBrowser).map(b -> b.getName()).orElse(null);
    }

    private record IpRegion(String province, String city) {
        static final IpRegion EMPTY = new IpRegion(null, null);
    }
}
