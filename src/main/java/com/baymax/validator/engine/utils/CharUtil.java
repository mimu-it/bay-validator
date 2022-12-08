package com.baymax.validator.engine.utils;

/**
 * @author xiao.hu
 * @date 2022-12-08
 * @apiNote
 */
public class CharUtil {

    public static boolean isBlankChar(char c) {
        return isBlankChar((int)c);
    }

    public static boolean isBlankChar(int c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == 65279 || c == 8234 || c == 0;
    }
}
