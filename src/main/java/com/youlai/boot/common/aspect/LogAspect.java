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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 日志切面
 *
 * @author Ray.Hao
 * @since 2.10.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LogAspect {

    private final LogService logService;

    /**
     * 日志注解切点
     */
    @Pointcut("@annotation(logAnnotation)")
    public void logPointCut(Log logAnnotation) {
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around(value = "logPointCut(logAnnotation)", argNames = "pjp,logAnnotation")
    public Object around(ProceedingJoinPoint pjp, Log logAnnotation) throws Throwable {

        long startTime = System.currentTimeMillis();
        // 在方法执行前获取用户信息，避免 logout 等操作清除 SecurityContext 后无法获取
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        Object result = null;
        Exception exception = null;

        try {
            result = pjp.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            // fallback：登录等场景在 proceed() 前未认证，需在 proceed() 后获取
            if (userId == null) {
                userId = SecurityUtils.getUserId();
                username = SecurityUtils.getUsername();
            }
            try {
                saveLogAsync(logAnnotation, executionTime, exception, userId, username);
            } catch (Exception ex) {
                log.error("保存操作日志失败", ex);
            }
        }
    }

    /**
     * 异步保存日志
     */
    @Async
    public void saveLogAsync(Log logAnnotation, long executionTime, Exception exception, Long userId, String username) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            // 解析 User-Agent
            String userAgentStr = request.getHeader("User-Agent");
            UserAgent userAgent = UserAgentUtil.parse(userAgentStr);

            // 解析 IP 地区
            String ip = IPUtils.getIpAddr(request);
            String region = IPUtils.getRegion(ip);
            String province = null;
            String city = null;
            if (StrUtil.isNotBlank(region)) {
                String[] parts = region.split("\\|");
                if (parts.length >= 3) {
                    province = StrUtil.blankToDefault(parts[2], null);
                    city = StrUtil.blankToDefault(parts[3], null);
                }
            }

            // 构建日志实体
            LogModuleEnum module = logAnnotation.module();
            ActionTypeEnum actionType = logAnnotation.value();
            String title = StrUtil.blankToDefault(logAnnotation.title(),
                    module.getLabel() + "-" + actionType.getLabel());
            String content = logAnnotation.content();

            SysLog logEntity = new SysLog();
            logEntity.setModule(module);
            logEntity.setActionType(actionType);
            logEntity.setTitle(title);
            logEntity.setContent(content);
            logEntity.setOperatorId(userId);
            logEntity.setOperatorName(username);
            logEntity.setRequestUri(request.getRequestURI());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setIp(ip);
            logEntity.setProvince(province);
            logEntity.setCity(city);
            logEntity.setDevice(userAgent.getOs().getName());
            logEntity.setOs(userAgent.getOs().getName());
            logEntity.setBrowser(userAgent.getBrowser().getName());
            logEntity.setStatus(exception == null ? 1 : 0);
            logEntity.setErrorMsg(exception != null ? exception.getMessage() : null);
            logEntity.setExecutionTime((int) executionTime);
            logEntity.setCreateTime(LocalDateTime.now());

            logService.save(logEntity);
        } catch (Exception e) {
            log.error("保存操作日志异常: {}", e.getMessage());
        }
    }
}
