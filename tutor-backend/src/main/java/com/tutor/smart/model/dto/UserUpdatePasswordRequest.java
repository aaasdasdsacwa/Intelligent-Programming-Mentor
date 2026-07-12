package com.tutor.smart.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户修改密码请求入参
 */
@Data
public class UserUpdatePasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前原密码
     */
    private String oldPassword;

    /**
     * 设置新密码
     */
    private String newPassword;
}