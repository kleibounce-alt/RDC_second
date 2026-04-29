package com.klei.common.mapper;

import com.klei.common.annotation.*;
import com.klei.common.utils.ResultSetMapper;
import com.klei.common.pool.ConnectionPool;
import com.klei.common.utils.LogUtil;

import java.lang.reflect.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MapperProxy implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        String sql = null;
        boolean isSelect = false;

        if (method.isAnnotationPresent(Select.class)) {
            sql = method.getAnnotation(Select.class).value();
            isSelect = true;
        } else if (method.isAnnotationPresent(Insert.class)) {
            sql = method.getAnnotation(Insert.class).value();
        } else if (method.isAnnotationPresent(Update.class)) {
            sql = method.getAnnotation(Update.class).value();
        } else if (method.isAnnotationPresent(Delete.class)) {
            sql = method.getAnnotation(Delete.class).value();
        } else {
            throw new RuntimeException("Mapper 方法缺少 SQL 注解: " + method.getName());
        }

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getConnection();
            ps = conn.prepareStatement(sql, isSelect ? Statement.NO_GENERATED_KEYS : Statement.RETURN_GENERATED_KEYS);

            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
            }

            if (isSelect) {
                ResultSet rs = ps.executeQuery();
                return handleQuery(rs, method);
            } else {
                int rows = ps.executeUpdate();
                // 修复：支持 Long 包装类和 long 基本类型返回主键
                if (method.isAnnotationPresent(Insert.class)) {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == Long.class || returnType == long.class) {
                        ResultSet keys = ps.getGeneratedKeys();
                        if (keys.next()) {
                            long key = keys.getLong(1);
                            keys.close();
                            return returnType == long.class ? key : Long.valueOf(key);
                        }
                    }
                }
                return rows;
            }
        } catch (SQLException e) {
            LogUtil.error("Mapper 执行失败 [" + sql + "]", e);
            throw new RuntimeException("数据库操作失败: " + e.getMessage(), e);
        } finally {
            // 确保连接归还
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private Object handleQuery(ResultSet rs, Method method) throws SQLException {
        Class<?> returnType = method.getReturnType();

        if (List.class.isAssignableFrom(returnType)) {
            Type genericReturnType = method.getGenericReturnType();
            Class<?> elementType = Object.class;
            if (genericReturnType instanceof ParameterizedType pt) {
                Type[] actualTypes = pt.getActualTypeArguments();
                if (actualTypes.length > 0 && actualTypes[0] instanceof Class) {
                    elementType = (Class<?>) actualTypes[0];
                }
            }
            List<Object> list = new ArrayList<>();
            while (rs.next()) {
                list.add(ResultSetMapper.mapRow(rs, elementType));
            }
            rs.close();
            return list;
        }

        if (returnType.isPrimitive() || ResultSetMapper.isWrapperType(returnType) || returnType == String.class) {
            Object result = null;
            if (rs.next()) {
                result = rs.getObject(1);
            }
            rs.close();
            return result;
        }

        Object result = null;
        if (rs.next()) {
            result = ResultSetMapper.mapRow(rs, returnType);
        }
        rs.close();
        return result;
    }
}