package com.klei.common.mapper;

import java.lang.reflect.Proxy;

public class MapperProxyFactory {

    @SuppressWarnings("unchecked")
    public static <T> T getMapper(Class<T> mapperInterface) {
        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class[]{mapperInterface},
                new MapperProxy()
        );
    }
}