package com.youlai.boot.codegen.util;

import cn.hutool.core.util.StrUtil;

/**
 * 命名转换，代码生成的目录、路由与权限标识使用同一套规则
 *
 * @author Ray.Hao
 * @since 4.14.1
 */
public final class NamingUtils {

    private NamingUtils() {
    }

    /**
     * 转短横线命名，如 MenuDemo → menu-demo
     */
    public static String kebab(String value) {
        return StrUtil.toSymbolCase(value, '-');
    }

    /**
     * 转大写下划线命名，如 MenuDemo → MENU_DEMO
     */
    public static String upperSnake(String value) {
        return StrUtil.toSymbolCase(value, '_').toUpperCase();
    }

    /**
     * 短横线命名转英文复数，如 dict-item → dict-items、category → categories
     */
    public static String pluralKebab(String kebab) {
        if (kebab.matches(".*(s|x|z|ch|sh)$")) {
            return kebab + "es";
        }
        if (kebab.matches(".*[^aeiou]y$")) {
            return kebab.substring(0, kebab.length() - 1) + "ies";
        }
        return kebab + "s";
    }
}
