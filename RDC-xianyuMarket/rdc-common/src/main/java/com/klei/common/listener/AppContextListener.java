package com.klei.common.listener;

import com.klei.common.ioc.IoCContainer;
import com.klei.common.pool.ConnectionPool;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        IoCContainer.scan("com.klei");
        System.out.println("应用启动，连接池已初始化，IoC已扫描");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ConnectionPool.destroy();
        System.out.println("应用关闭，连接池已销毁");
    }
}