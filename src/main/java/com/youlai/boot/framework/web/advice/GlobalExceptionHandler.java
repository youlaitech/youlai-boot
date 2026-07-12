package com.youlai.boot.framework.web.advice;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.security.exception.TokenInvalidException;
import com.youlai.boot.framework.web.exception.RateLimitException;
import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
    @ResponseStatus(HttpStatus.OK)
    public <T> Result<T> processException(BindException e) {
        log.error("BindException:{}", e.getMessage());
        String msg = e.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("；"));
        return Result.failed(ResultCode.USER_REQUEST_PARAMETER_ERROR, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public <T> Result<T> processException(ConstraintViolationException e) {
        log.error("ConstraintViolationException:{}", e.getMessage());
        String msg = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("；"));
        return Result.failed(ResultCode.INVALID_USER_INPUT, msg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public <T> Result<T> processException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException:{}", e.getMessage());
        String msg = e.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("；"));
        return Result.failed(ResultCode.INVALID_USER_INPUT, msg);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public <T> Result<T> processException(NoHandlerFoundException e) {
        log.error(e.getMessage(), e);
        return Result.failed(ResultCode.INTERFACE_NOT_EXIST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public <T> Result<T> processException(MissingServletRequestParameterException e) {
        log.error(e.getMessage(), e);
        return Result.failed(ResultCode.REQUEST_REQUIRED_PARAMETER_IS_EMPTY);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.OK)
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
    @ResponseStatus(HttpStatus.OK)
    public <T> Result<T> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常，异常原因：{}", e.getMessage(), e);
        return Result.failed(e.getMessage());
    }

    @ExceptionHandler(JacksonException.class)
    @ResponseStatus(HttpStatus.OK)
    public <T> Result<T> handleJacksonException(JacksonException e) {
        log.error("Json转换异常，异常原因：{}", e.getMessage(), e);
        return Result.failed(e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public <T> Result<T> processException(HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e);
        String errorMessage = "请求体不可为空";
        Throwable cause = e.getCause();
        if (cause != null) {
            errorMessage = convertMessage(cause);
        }
        return Result.failed(errorMessage);
    }

    @ExceptionHandler(TypeMismatchException.class)
    @ResponseStatus(HttpStatus.OK)
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
        if (e instanceof AccessDeniedException
                || e instanceof AuthenticationException) {
            throw e;
        }
        log.error("unknown exception", e);
        return Result.failed(e.getLocalizedMessage());
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
