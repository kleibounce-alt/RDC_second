package com.klei.common.utils;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class ResultSetMapper {

    public static <T> T mapRow(ResultSet rs, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            for (int i = 1; i <= colCount; i++) {
                String colName = meta.getColumnLabel(i);
                if (colName == null || colName.isEmpty()) {
                    colName = meta.getColumnName(i);
                }
                Object value = rs.getObject(i);

                String fieldName = toCamelCase(colName);
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    if (value != null) {
                        field.set(instance, convertType(value, field.getType()));
                    }
                } catch (NoSuchFieldException ignored) {
                    // 字段不匹配则跳过
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("ResultSet 映射失败: " + clazz.getName(), e);
        }
    }

    public static String toCamelCase(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static Object convertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        }
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }
        if (targetType == String.class) {
            return value.toString();
        }
        if (targetType == LocalDateTime.class) {
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime();
            }
        }
        return value;
    }

    public static boolean isWrapperType(Class<?> clazz) {
        return clazz == Long.class || clazz == Integer.class || clazz == Double.class
                || clazz == Float.class || clazz == Boolean.class || clazz == Byte.class
                || clazz == Short.class || clazz == Character.class;
    }
}