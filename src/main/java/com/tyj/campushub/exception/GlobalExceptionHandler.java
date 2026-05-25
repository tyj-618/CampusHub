package com.tyj.campushub.exception;

import com.tyj.campushub.common.ApiResponse;
import com.tyj.campushub.common.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.fail(exception.getErrorCode().code(), exception.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ApiResponse<Void> handleValidationException(Exception exception) {
        return ApiResponse.fail(ErrorCode.PARAM_ERROR.code(), ErrorCode.PARAM_ERROR.message());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.fail(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message());
    }
}
