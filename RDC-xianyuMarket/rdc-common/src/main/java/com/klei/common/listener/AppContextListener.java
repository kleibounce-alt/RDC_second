package com.klei.common.listener;

import com.klei.common.ioc.IoCContainer;
import com.klei.common.mq.RabbitMQConfig;
import com.klei.common.pool.ConnectionPool;
import com.klei.common.utils.LogUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String scanPackage = sce.getServletContext().getInitParameter("scanPackage");
        if (scanPackage == null || scanPackage.isEmpty()) {
            scanPackage = "com.klei";
        }
        IoCContainer.scan(scanPackage);
        System.out.println("应用启动，连接池已初始化，IoC已扫描 " + scanPackage);

        // RabbitMQ 消费者启动（如果当前模块包含 message-service 的类）
        try {
            Class<?> consumerClass = Class.forName("com.klei.message.mq.MessageConsumer");
            Object consumer = IoCContainer.getBean(consumerClass);
            if (consumer != null) {
                consumerClass.getMethod("start").invoke(consumer);
                System.out.println("RabbitMQ 消费者已启动");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("当前模块无 MessageConsumer，跳过 RabbitMQ 消费者启动");
        } catch (Exception e) {
            LogUtil.error("RabbitMQ 消费者启动异常", e);
        }

        // 敏感词热加载到 Redis
        try {
            Class<?> swInitClass = Class.forName("com.klei.message.init.SensitiveWordInit");
            Object swInit = IoCContainer.getBean(swInitClass);
            if (swInit != null) {
                swInitClass.getMethod("load").invoke(swInit);
                System.out.println("敏感词已加载到 Redis");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("当前模块无 SensitiveWordInit，跳过敏感词加载");
        } catch (Exception e) {
            LogUtil.error("敏感词加载异常", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        RabbitMQConfig.close();
        ConnectionPool.destroy();
        System.out.println("应用关闭，连接池已销毁");
    }
}