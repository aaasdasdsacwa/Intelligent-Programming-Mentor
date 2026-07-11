package com.tutor.smart.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 已登录用户视图 (脱敏)
 */
@Data
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private LocalDateTime createTime;
}