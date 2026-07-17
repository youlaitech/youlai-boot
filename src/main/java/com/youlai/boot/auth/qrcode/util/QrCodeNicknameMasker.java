package com.youlai.boot.auth.qrcode.util;

/**
 * 昵称脱敏：保留首尾各一个字符，中间用 * 替换。
 * <p>
 * 长度 1：原样返回
 * 长度 2：首字 + *
 * 长度 ≥3：首字 + (n-2) 个 * + 末字
 */
public final class QrCodeNicknameMasker {

    private QrCodeNicknameMasker() {
    }

    public static String mask(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "";
        }
        int len = nickname.length();
        if (len == 1) {
            return nickname;
        }
        if (len == 2) {
            return nickname.charAt(0) + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(nickname.charAt(0));
        for (int i = 0; i < len - 2; i++) {
            sb.append('*');
        }
        sb.append(nickname.charAt(len - 1));
        return sb.toString();
    }
}
