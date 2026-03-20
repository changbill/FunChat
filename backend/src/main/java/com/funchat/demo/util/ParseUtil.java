package com.funchat.demo.util;

import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;

public class ParseUtil {
    public static Long parseLong(String word, BusinessException exception) {
        try{
            return Long.parseLong(word);
        } catch (NumberFormatException e) {
            throw exception;
        }
    }
}
