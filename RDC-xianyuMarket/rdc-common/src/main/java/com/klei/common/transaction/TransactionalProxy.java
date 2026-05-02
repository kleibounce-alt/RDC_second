package com.klei.common.transaction;

import com.klei.common.annotation.Transactional;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TransactionalProxy implements InvocationHandler {

    private final Object target;

    public TransactionalProxy(Object target) {
        this.target = target;
    }

    public Object getTarget() {
        return target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean needTx = method.isAnnotationPresent(Transactional.class)
                || target.getClass().isAnnotationPresent(Transactional.class);

        if (!needTx) {
            return method.invoke(target, args);
        }

        try {
            TransactionManager.begin();
            Object result = method.invoke(target, args);
            TransactionManager.commit();
            return result;
        } catch (InvocationTargetException e) {
            TransactionManager.rollback();
            throw e.getTargetException();
        } catch (Exception e) {
            TransactionManager.rollback();
            throw e;
        } finally {
            TransactionManager.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrap(Class<?> interfaceClass, Object target) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new TransactionalProxy(target)
        );
    }
}