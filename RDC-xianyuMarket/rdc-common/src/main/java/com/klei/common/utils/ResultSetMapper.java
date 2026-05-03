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
        // 修复：原代码 isAssignableFrom 方向写反，改为 isInstance
        if (targetType.isInstance(value)) {
            return value;
        }

        // Number 统一处理（Integer, Long, BigDecimal, Float, Double 等）
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == Long.class || targetType == long.class) {
                return num.longValue();
            }
            if (targetType == Integer.class || targetType == int.class) {
                return num.intValue();
            }
            if (targetType == Double.class || targetType == double.class) {
                return num.doubleValue();
            }
            if (targetType == Float.class || targetType == float.class) {
                return num.floatValue();
            }
            if (targetType == Short.class || targetType == short.class) {
                return num.shortValue();
            }
            if (targetType == Byte.class || targetType == byte.class) {
                return num.byteValue();
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                return num.intValue() != 0;
            }
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == LocalDateTime.class) {
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime();
            }
            if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate().atStartOfDay();
            }
        }

        // 枚举类型映射（数据库 ENUM/字符串 -> Java Enum）
        if (targetType.isEnum()) {
            if (value instanceof String) {
                String enumStr = ((String) value).trim();
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object enumValue = Enum.valueOf((Class<Enum>) targetType, enumStr);
                    return enumValue;
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("枚举映射失败: " + targetType.getSimpleName() + " 不存在值'" + enumStr + "'", e);
                }
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