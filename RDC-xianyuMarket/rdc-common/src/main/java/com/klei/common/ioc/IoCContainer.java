package com.klei.common.ioc;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.xianyuMarket.rdc_common.utils.LogUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IoCContainer {

    private static final Map<Class<?>, Object> beanMap = new ConcurrentHashMap<>();

    /**
     * 扫描包路径，实例化所有@Component类，并完成字段注入
     */
    public static void scan(String basePackage) {
        String packagePath = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(packagePath);

        if (resource == null) {
            LogUtil.warn("IoC扫描路径不存在: " + basePackage);
            return;
        }

        try {
            String filePath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
            File dir = new File(filePath);
            if (!dir.exists() || !dir.isDirectory()) {
                LogUtil.warn("IoC扫描目录无效: " + filePath);
                return;
            }

            // 阶段1：扫描并实例化所有@Component
            scanDirectory(dir, basePackage);

            // 阶段2：统一注入@Autowired字段
            doInjection();

            LogUtil.info("IoC容器初始化完成，共注册 " + beanMap.size() + " 个Bean");
        } catch (Exception e) {
            LogUtil.error("IoC容器启动失败", e);
            throw new RuntimeException("IoC容器启动失败", e);
        }
    }

    private static void scanDirectory(File dir, String packageName) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                //递归扫描
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Component.class)) {
                        instantiate(clazz);
                    }
                } catch (Exception e) {
                    LogUtil.warn("IoC扫描类失败: " + className + " | " + e.getMessage());
                }
            }
        }
    }

    private static void instantiate(Class<?> clazz) {
        if (clazz.isInterface()) {
            return;
        }
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            beanMap.put(clazz, instance);
            LogUtil.info("IoC注册Bean: " + clazz.getName());
        } catch (Exception e) {
            LogUtil.error("IoC实例化失败: " + clazz.getName(), e);
        }
    }

    private static void doInjection() {
        for (Object bean : beanMap.values()) {
            Class<?> clazz = bean.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                field.setAccessible(true);
                Class<?> fieldType = field.getType();

                Object dependency = beanMap.get(fieldType);
                if (dependency == null) {
                    LogUtil.warn("IoC注入跳过，未找到Bean: " + fieldType.getName() + " -> " + clazz.getName() + "." + field.getName());
                    continue;
                }

                try {
                    field.set(bean, dependency);
                    LogUtil.info("IoC注入: " + clazz.getSimpleName() + "." + field.getName() + " = " + fieldType.getSimpleName());
                } catch (IllegalAccessException e) {
                    LogUtil.error("IoC注入失败: " + clazz.getName() + "." + field.getName(), e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> clazz) {
        return (T) beanMap.get(clazz);
    }

    public static <T> void register(Class<T> clazz, T instance) {
        beanMap.put(clazz, instance);
        LogUtil.info("IoC手动注册Bean: " + clazz.getName());
    }

    public static void injectAll() {
        doInjection();
        LogUtil.info("IoC重新注入完成");
    }
}