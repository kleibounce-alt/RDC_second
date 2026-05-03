package com.klei.common.ioc;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.mapper.MapperProxyFactory;
import com.klei.common.transaction.TransactionalProxy;
import com.klei.common.utils.LogUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IoCContainer {

    private static final Map<Class<?>, Object> beanMap = new ConcurrentHashMap<>();

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

            scanDirectory(dir, basePackage);
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
            Object raw = clazz.getDeclaredConstructor().newInstance();
            Object bean = raw;

            // 伪AOP：有@Transactional且实现了接口，包JDK动态代理
            if (needTransactionalProxy(clazz)) {
                Class<?> iface = findFirstInterface(clazz);
                if (iface != null) {
                    bean = TransactionalProxy.wrap(iface, raw);
                    beanMap.put(iface, bean);
                    LogUtil.info("IoC注册事务代理: " + iface.getName());
                } else {
                    LogUtil.warn("类" + clazz.getName() + " 有@Transactional但无接口，无法创建JDK代理");
                }
            }

            beanMap.put(clazz, bean);
            LogUtil.info("IoC注册Bean: " + clazz.getName());
        } catch (Exception e) {
            LogUtil.error("IoC实例化失败: " + clazz.getName(), e);
        }
    }

    private static boolean needTransactionalProxy(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Transactional.class)) {
            return true;
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Transactional.class)) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> findFirstInterface(Class<?> clazz) {
        Class<?>[] interfaces = clazz.getInterfaces();
        return interfaces.length > 0 ? interfaces[0] : null;
    }

    private static void doInjection() {
        for (Object bean : beanMap.values()) {
            Object target = unwrapProxy(bean);
            Class<?> clazz = target.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                field.setAccessible(true);
                Class<?> fieldType = field.getType();

                Object dependency = findBean(fieldType);

                if (dependency == null && fieldType.isInterface()) {
                    if (fieldType.getSimpleName().endsWith("Mapper")) {
                        try {
                            dependency = MapperProxyFactory.getMapper(fieldType);
                            beanMap.put(fieldType, dependency);
                            LogUtil.info("IoC自动注册Mapper: " + fieldType.getName());
                        } catch (Exception e) {
                            LogUtil.warn("IoC注入跳过，无法创建Mapper代理: " + fieldType.getName());
                            continue;
                        }
                    } else {
                        LogUtil.warn("IoC注入跳过，接口实现类未找到: " + fieldType.getName());
                        continue;
                    }
                }

                if (dependency == null) {
                    LogUtil.warn("IoC注入跳过，未找到Bean: " + fieldType.getName() + " -> " + clazz.getName() + "." + field.getName());
                    continue;
                }

                try {
                    field.set(target, dependency);
                    LogUtil.info("IoC注入: " + clazz.getSimpleName() + "." + field.getName() + " = " + dependency.getClass().getSimpleName());
                } catch (IllegalAccessException e) {
                    LogUtil.error("IoC注入失败: " + clazz.getName() + "." + field.getName(), e);
                }
            }
        }
    }

    private static Object unwrapProxy(Object bean) {
        if (bean == null) {
            return null;
        }
        if (Proxy.isProxyClass(bean.getClass())) {
            InvocationHandler h = Proxy.getInvocationHandler(bean);
            if (h instanceof TransactionalProxy) {
                return ((TransactionalProxy) h).getTarget();
            }
        }
        return bean;
    }

    private static Object findBean(Class<?> type) {
        Object bean = beanMap.get(type);
        if (bean != null) {
            return bean;
        }
        // 按接口/父类匹配
        for (Map.Entry<Class<?>, Object> entry : beanMap.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
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