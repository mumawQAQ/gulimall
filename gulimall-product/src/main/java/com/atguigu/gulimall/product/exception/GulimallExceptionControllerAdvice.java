package com.atguigu.gulimall.product.exception;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

@Slf4j
@ResponseBody
@RestControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")
public class GulimallExceptionControllerAdvice {
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e) {
      log.error("【系统异常】{} --- {}", e.getMessage(),e.getClass());;
        BindingResult bindingResult = e.getBindingResult();

        HashMap<String, String> stringStringHashMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach(err -> {
            String defaultMessage = err.getDefaultMessage();
            String field = err.getField();
            stringStringHashMap.put(field, defaultMessage);
        });
        return R.error(BizCodeEnum.VAILD_EXCEPTION.getCode(),BizCodeEnum.VAILD_EXCEPTION.getMsg()).put("data",stringStringHashMap);

    }
    @ExceptionHandler(value = Exception.class)
    public R handleException(Exception e) {
        log.error("【系统异常】{} --- {}", e.getMessage(),e.getClass());;
        return R.error(BizCodeEnum.UNKNOWN_EXCEPTION.getCode(), BizCodeEnum.UNKNOWN_EXCEPTION.getMsg());
    }
}
