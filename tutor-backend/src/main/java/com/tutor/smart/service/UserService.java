package com.tutor.smart.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tutor.smart.model.entity.User;
import com.tutor.smart.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录态常量键
     */
    String USER_LOGIN_STATE = "user_login_state";

    /**
     * 用户注册
     * @return 新增用户ID
     */
    long userRegister(String username, String password, String checkPassword);

    /**
     * 用户登录
     * @return 脱敏后的用户信息
     */
    UserVO userLogin(String username, String password, HttpServletRequest request);

    /**
     * 获取当前登录用户 (如果未登录则抛出异常)
     */
    UserVO getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);
}