package com.cashier.controller;

import com.cashier.model.User;

/**
 * 收银视图宿主接口
 *
 * <p>传统 {@code CartController} 与触屏版 {@code TouchCartController} 的公共契约：
 * 退出登录前的空车确认、搜索框聚焦、注入当前用户。两个收银视图各自实现，行为保持一致。</p>
 */
public interface CartViewHost {

    /** 购物车是否为空（退出登录前用于提示确认） */
    boolean isCartEmpty();

    /** 聚焦到搜索框 */
    void focusSearchField();

    /** 注入当前登录用户（用于结账审计、交接班等） */
    void setCurrentUser(User user);
}
