package com.funchat.demo.util;

import com.funchat.demo.global.exception.BusinessException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParseUtil {

    public static Long parseLong(Optional<String> word, BusinessException exception) {
        return parseLong(word.orElseThrow(() -> exception), exception);
    }

    public static Long parseLong(String word, BusinessException exception) {
        try{
            return Long.parseLong(word);
        } catch (NumberFormatException e) {
            throw exception;
        }
    }
}
