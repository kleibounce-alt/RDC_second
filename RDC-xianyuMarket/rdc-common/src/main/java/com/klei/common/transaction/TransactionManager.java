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
        }
        // 修复：不在 rollback 里 remove，统一交给 close 清理
    }

    public static void close() {
        Integer count = COUNTER.get();
        if (count == null) {
            // 修复：即使 count 被 rollback 清掉了，也要强制释放连接
            forceClose();
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

    private static void forceClose() {
        Connection conn = HOLDER.get();
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException("强制关闭事务连接失败", e);
            } finally {
                HOLDER.remove();
            }
        }
    }

    public static Connection getCurrentConnection() {
        return HOLDER.get();
    }
}