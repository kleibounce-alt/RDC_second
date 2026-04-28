package com.klei.common.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PooledConnectionHandler implements InvocationHandler {

    private final Connection realConnection;
    private final AtomicBoolean returned = new AtomicBoolean(false);

    public PooledConnectionHandler(Connection realConnection) {
        this.realConnection = realConnection;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if ("close".equals(methodName)) {
            if (returned.compareAndSet(false, true)) {
                ConnectionPool.recycle(realConnection);
            }
            return null;
        }

        if (returned.get()) {
            throw new SQLException("连接已归还连接池，禁止继续使用");
        }

        return method.invoke(realConnection, args);
    }
}