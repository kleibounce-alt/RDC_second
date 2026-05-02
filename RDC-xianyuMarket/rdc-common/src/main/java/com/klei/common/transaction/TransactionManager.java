package com.klei.common.transaction;

import com.klei.common.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager {

    private static final ThreadLocal<Connection> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Integer> COUNTER = new ThreadLocal<>();

    public static void begin() throws SQLException {
        Integer count = COUNTER.get();
        if (count == null) {
            COUNTER.set(1);
            Connection conn = ConnectionPool.getConnection();
            conn.setAutoCommit(false);
            HOLDER.set(conn);
        } else {
            // 嵌套事务，复用连接
            COUNTER.set(count + 1);
        }
    }

    public static void commit() {
        Integer count = COUNTER.get();
        if (count == null) {
            return;
        }
        if (count == 1) {
            Connection conn = HOLDER.get();
            if (conn != null) {
                try {
                    conn.commit();
                } catch (SQLException e) {
                    throw new RuntimeException("事务提交失败", e);
                }
            }
            COUNTER.remove();
        } else {
            COUNTER.set(count - 1);
        }
    }

    public static void rollback() {
        Integer count = COUNTER.get();
        if (count == null) {
            return;
        }
        if (count == 1) {
            Connection conn = HOLDER.get();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    throw new RuntimeException("事务回滚失败", e);
                }
            }
            COUNTER.remove();
        } else {
            COUNTER.set(count - 1);
        }
    }

    public static void close() {
        Integer count = COUNTER.get();
        if (count == null) {
            return;
        }
        if (count == 1) {
            Connection conn = HOLDER.get();
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    throw new RuntimeException("关闭事务连接失败", e);
                } finally {
                    HOLDER.remove();
                    COUNTER.remove();
                }
            }
        } else {
            COUNTER.set(count - 1);
        }
    }

    public static Connection getCurrentConnection() {
        return HOLDER.get();
    }
}