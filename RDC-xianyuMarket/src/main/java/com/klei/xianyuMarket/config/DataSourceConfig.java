package com.klei.xianyuMarket.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSourceConfig {

    public static final String URL = "jdbc:mysql://localhost:3306/goodfish?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true";
    public static final String USERNAME = "root";
    public static final String PASSWORD = "112302kxt";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL驱动加载失败", e);
        }
    }

    public static Connection newRealConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}