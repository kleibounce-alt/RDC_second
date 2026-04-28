package com.klei.common.pool;

import com.klei.xianyuMarket.rdc_common.config.DataSourceConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionPool {

    // 最小空闲连接数
    private static final int MIN_IDLE = 5;
    // 最大活跃连接数
    private static final int MAX_ACTIVE = 20;
    // 等待超时 5 秒
    private static final long WAIT_TIMEOUT = 5000;

    private static final LinkedBlockingQueue<Connection> idleQueue = new LinkedBlockingQueue<>();
    private static final Set<Connection> allRealConnections = ConcurrentHashMap.newKeySet();
    private static int activeCount = 0;

    private static final Object lock = new Object();

    static {
        initPool();
    }

    private static void initPool() {
        synchronized (lock) {
            for (int i = 0; i < MIN_IDLE; i++) {
                createAndAddConnection();
            }
        }
    }

    private static void createAndAddConnection() {
        try {
            Connection realConn = DataSourceConfig.newRealConnection();
            allRealConnections.add(realConn);
            idleQueue.offer(realConn);
            activeCount++;
        } catch (SQLException e) {
            throw new RuntimeException("创建数据库连接失败", e);
        }
    }

    private static Connection wrap(Connection realConn) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                new PooledConnectionHandler(realConn)
        );
    }

    public static Connection getConnection() throws SQLException {
        synchronized (lock) {
            // 1. 优先从空闲队列拿
            Connection realConn = idleQueue.poll();
            if (realConn != null) {
                return wrap(realConn);
            }

            // 2. 空闲没了，看能不能新建（还没达到上限）
            if (activeCount < MAX_ACTIVE) {
                createAndAddConnection();
                realConn = idleQueue.poll();
                if (realConn != null) {
                    return wrap(realConn);
                }
            }

            // 3. 达到上限，进入等待
            long deadline = System.currentTimeMillis() + WAIT_TIMEOUT;
            while (true) {
                realConn = idleQueue.poll();
                if (realConn != null) {
                    return wrap(realConn);
                }

                long remain = deadline - System.currentTimeMillis();
                if (remain <= 0) {
                    throw new SQLException("获取连接超时，连接池已满 (active=" + activeCount + ")");
                }

                try {
                    lock.wait(remain);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("获取连接被中断", e);
                }
            }
        }
    }

    static void recycle(Connection realConn) {
        if (realConn == null) {
            return;
        }

        synchronized (lock) {
            if (!allRealConnections.contains(realConn)) {
                return;
            }

            if (idleQueue.size() < MIN_IDLE) {
                idleQueue.offer(realConn);
            } else {
                allRealConnections.remove(realConn);
                activeCount--;
                try {
                    realConn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            lock.notifyAll();
        }
    }

    public static void destroy() {
        synchronized (lock) {
            for (Connection conn : allRealConnections) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            allRealConnections.clear();
            idleQueue.clear();
            activeCount = 0;
        }
    }
}