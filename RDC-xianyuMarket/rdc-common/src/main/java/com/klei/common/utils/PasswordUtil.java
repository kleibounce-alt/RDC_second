package com.klei.common.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // 生成加盐哈希
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt());
    }

    // 校验
    public static boolean check(String plain, String hashed) {
        return BCrypt.checkpw(plain, hashed);
    }
}