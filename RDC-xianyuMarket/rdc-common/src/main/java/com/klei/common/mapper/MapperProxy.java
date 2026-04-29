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

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, isSelect ? Statement.NO_GENERATED_KEYS : Statement.RETURN_GENERATED_KEYS)) {

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
                if (method.isAnnotationPresent(Insert.class) && method.getReturnType() == Long.class) {
                    ResultSet keys = ps.getGeneratedKeys();
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
                return rows;
            }
        } catch (SQLException e) {
            LogUtil.error("Mapper 执行失败 [" + sql + "]", e);
            throw new RuntimeException("数据库操作失败: " + e.getMessage(), e);
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
            return list;
        }

        if (returnType.isPrimitive() || ResultSetMapper.isWrapperType(returnType) || returnType == String.class) {
            if (rs.next()) {
                return rs.getObject(1);
            }
            return null;
        }

        if (rs.next()) {
            return ResultSetMapper.mapRow(rs, returnType);
        }
        return null;
    }
}