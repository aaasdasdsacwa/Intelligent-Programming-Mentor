package com.tutor.smart.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String checkPassword; // 确认密码
}