package com.youlai.boot.framework.web.advice;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.common.exception.TokenInvalidException;
import com.youlai.boot.common.exception.RateLimitException;
import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import tools.jackson.core.JacksonException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 全局系统异常处理器
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(BindException e) {
        String msg = e.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("；"));
        return Result.failed(ResultCode.USER_REQUEST_PARAMETER_ERROR, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("；"));
        return Result.failed(ResultCode.INVALID_USER_INPUT, msg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("；"));
        return Result.failed(ResultCode.INVALID_USER_INPUT, msg);
    }

    /**
     * 参数约束失败返回400，返回值约束失败保留500
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleMethodValidationException(HandlerMethodValidationException e) {
        if (e.isForReturnValue()) {
            log.error("接口返回值校验失败", e);
            return ResponseEntity.status(e.getStatusCode()).body(Result.failed(ResultCode.SYSTEM_ERROR));
        }
        String message = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
        return ResponseEntity.status(e.getStatusCode()).body(Result.failed(ResultCode.INVALID_USER_INPUT, message));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public <T> Result<T> processException(NoHandlerFoundException e) {
        log.error(e.getMessage(), e);
        return Result.failed(ResultCode.INTERFACE_NOT_EXIST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(MissingServletRequestParameterException e) {
        log.error(e.getMessage(), e);
        return Result.failed(ResultCode.REQUEST_REQUIRED_PARAMETER_IS_EMPTY);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(MethodArgumentTypeMismatchException e) {
        log.error(e.getMessage(), e);
        return Result.failed(ResultCode.PARAMETER_FORMAT_MISMATCH, "类型错误");
    }

    @ExceptionHandler(ServletException.class)
    @ResponseStatus(HttpStatus.OK)
    public <T> Result<T> processException(ServletException e) {
        log.error(e.getMessage(), e);
        return Result.failed(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常，异常原因：{}", e.getMessage(), e);
        return Result.failed(e.getMessage());
    }

    @ExceptionHandler(JacksonException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> handleJacksonException(JacksonException e) {
        return Result.failed(ResultCode.PARAMETER_FORMAT_MISMATCH);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(HttpMessageNotReadableException e) {
        String errorMessage = "请求体不可为空";
        Throwable cause = e.getCause();
        if (cause != null) {
            errorMessage = convertMessage(cause);
        }
        return Result.failed(errorMessage);
    }

    @ExceptionHandler(TypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public <T> Result<T> processException(TypeMismatchException e) {
        log.error(e.getMessage(), e);
        return Result.failed(e.getMessage());
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public <T> Result<T> handleBadSqlGrammarException(BadSqlGrammarException e) {
        log.error("SQL执行异常, message={}", e.getMessage(), e);
        if (StrUtil.isNotBlank(e.getMessage()) && e.getMessage().contains("denied to user")) {
            return Result.failed(ResultCode.DATABASE_ACCESS_DENIED);
        }
        return Result.failed(ResultCode.DATABASE_EXECUTION_ERROR);
    }

    @ExceptionHandler(SQLSyntaxErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public <T> Result<T> processSQLSyntaxErrorException(SQLSyntaxErrorException e) {
        log.error(e.getMessage(), e);
        return Result.failed(ResultCode.DATABASE_EXECUTION_SYNTAX_ERROR);
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public <T> Result<T> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        log.error(e.getMessage(), e);
        return Result.failed(ResultCode.INTEGRITY_CONSTRAINT_VIOLATION);
    }

    @ExceptionHandler(TokenInvalidException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public <T> Result<T> handleTokenInvalidException(TokenInvalidException e) {
        return Result.failed(e.getResultCode());
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleAuthenticationServiceException(
            AuthenticationServiceException exception) {
        log.error("认证服务不可用", exception);
        return Result.failed(ResultCode.AUTH_SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException exception) {
        ResultCode code = exception instanceof DisabledException ? ResultCode.ACCOUNT_FROZEN
                : exception instanceof BadCredentialsException ? ResultCode.USER_PASSWORD_ERROR
                : ResultCode.USER_LOGIN_EXCEPTION;
        return Result.failed(code);
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public <T> Result<T> handleRateLimitException(RateLimitException e) {
        return Result.failed(e.getResultCode(), e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public <T> Result<T> handleBizException(BusinessException e) {
        log.error("biz exception", e);
        if (e.getResultCode() != null) {
            return Result.failed(e.getResultCode(), e.getMessage());
        }
        return Result.failed(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public <T> Result<T> handleException(Exception e) throws Exception {
        if (e instanceof AccessDeniedException) {
            throw e;
        }
        log.error("unknown exception", e);
        return Result.failed(ResultCode.SYSTEM_ERROR);
    }

    private String convertMessage(Throwable throwable) {
        String error = throwable.toString();
        String regulation = "\\[\"(.*?)\"]+";
        Pattern pattern = Pattern.compile(regulation);
        Matcher matcher = pattern.matcher(error);
        String group = "";
        if (matcher.find()) {
            String matchString = matcher.group();
            matchString = matchString.replace("[", "").replace("]", "");
            matchString = "%s字段类型错误".formatted(matchString.replaceAll("\"", ""));
            group += matchString;
        }
        return group;
    }
}
