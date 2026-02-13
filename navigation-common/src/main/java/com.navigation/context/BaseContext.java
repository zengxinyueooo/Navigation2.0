package com.navigation.context;

//存储和管理一些基础的上下文信息
//存储用户信息 方便取出



public class BaseContext {

    private static final ThreadLocal<Integer> userThreadLocal = new ThreadLocal<>();

    public static void saveUserId(Integer userId) {
        userThreadLocal.set(userId);
    }

    public static Integer getUserId() {
        return userThreadLocal.get();
    }

    public static void remove() {
        userThreadLocal.remove();
    }

}
