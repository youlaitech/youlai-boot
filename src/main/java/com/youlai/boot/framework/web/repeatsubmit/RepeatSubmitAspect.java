package com.youlai.boot.framework.web.repeatsubmit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.web.util.IPUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 防重复提交切面
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final RedissonClient redissonClient;

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    // 生成请求指纹时排除的参数类型，这些对象序列化结果不稳定或无业务含义
    private static final List<Class<?>> EXCLUDED_PARAM_TYPES =
            List.of(ServletRequest.class, ServletResponse.class, MultipartFile.class, BindingResult.class);

    @Pointcut("@annotation(repeatSubmit)")
    public void repeatSubmitPointCut(RepeatSubmit repeatSubmit) {
    }

    @Around(value = "repeatSubmitPointCut(repeatSubmit)", argNames = "pjp,repeatSubmit")
    public Object handleRepeatSubmit(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        String lockKey = buildLockKey(pjp, repeatSubmit);

        RBucket<String> bucket = redissonClient.getBucket(lockKey);
        boolean acquired = bucket.setIfAbsent("1", Duration.ofSeconds(repeatSubmit.expire()));
        if (!acquired) {
            throw new BusinessException(ResultCode.DUPLICATE_SUBMISSION);
        }
        return pjp.proceed();
    }

    /**
     * 生成防重占位的 key
     */
    private String buildLockKey(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String userIdentifier = getUserIdentifier(request);
        String requestIdentifier = StrUtil.join(":", request.getMethod(), request.getRequestURI());
        String requestFingerprint = StrUtil.isNotBlank(repeatSubmit.key())
                ? DigestUtil.sha256Hex(evaluateKey(pjp, repeatSubmit.key()))
                : DigestUtil.sha256Hex(JSONUtil.toJsonStr(filterArgs(pjp)));
        return StrUtil.format(RedisConstants.Lock.RESUBMIT, userIdentifier, requestIdentifier + ":" + requestFingerprint);
    }

    /**
     * 过滤掉不可稳定序列化的参数后再生成指纹
     */
    private Object[] filterArgs(ProceedingJoinPoint pjp) {
        return Arrays.stream(pjp.getArgs())
                .filter(arg -> arg == null || EXCLUDED_PARAM_TYPES.stream().noneMatch(type -> type.isInstance(arg)))
                .toArray();
    }

    /**
     * 解析注解上的 SpEL key 表达式，方法参数可直接引用
     */
    private String evaluateKey(ProceedingJoinPoint pjp, String spel) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                pjp.getTarget(), method, pjp.getArgs(), PARAMETER_NAME_DISCOVERER);
        Object value = SPEL_PARSER.parseExpression(spel).getValue(context);
        return value != null ? value.toString() : "";
    }

    /**
     * 获取用户唯一标识
     * 1. 从请求头中获取 Token，使用 SHA-256 加密 Token 作为用户唯一标识
     * 2. 如果 Token 为空，使用 IP 作为用户唯一标识
     */
    private String getUserIdentifier(HttpServletRequest request) {
        String userIdentifier;
        String tokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(tokenHeader) && tokenHeader.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            String rawToken = tokenHeader.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());
            userIdentifier = DigestUtil.sha256Hex(rawToken);
        } else {
            userIdentifier = IPUtils.getIpAddr(request);
        }
        return userIdentifier;
    }

}
