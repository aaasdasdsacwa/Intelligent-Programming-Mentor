package com.tutor.smart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.mapper.UserMapper;
import com.tutor.smart.model.entity.User;
import com.tutor.smart.model.vo.UserVO;
import com.tutor.smart.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import com.tutor.smart.model.dto.UserUpdatePasswordRequest;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值：混淆密码，防止被轻易破解
     */
    private static final String SALT = "tutor_smart_oj_salt_9527";

    @Override
    public long userRegister(String username, String password, String checkPassword) {
        // 1. 基础非空校验
        if (StringUtils.isAnyBlank(username, password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (username.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (password.length() < 6 || checkPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 2. 数据库重复性校验
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该账号已被注册");
        }

        // 3. 密码加盐 MD5 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());

        // 4. 插入新数据
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword);
        user.setNickname("学生_" + username); // 默认昵称
        user.setRole("student"); // 默认角色为学生

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库异常");
        }
        return user.getId();
    }

    @Override
    public UserVO userLogin(String username, String password, HttpServletRequest request) {
        // 1. 基础校验
        if (StringUtils.isAnyBlank(username, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        if (username.length() < 4 || password.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码长度不正确");
        }

        // 2. 比对加密密码
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        queryWrapper.eq("password", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);

        // 3. 用户不存在或密码错误
        if (user == null) {
            log.info("user login failed, username: {}", username);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }

        // 4. 登录成功，将用户信息存入 Session 保持状态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        // 5. 组装脱敏 VO 返回
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public UserVO getLoginUser(HttpServletRequest request) {
        // 1. 从 Session 中获取用户
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 2. 从数据库重查一次，防止用户信息被管理员封禁或修改
        User user = this.getById(currentUser.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在或已被封禁");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录，无需注销");
        }
        // 清理 Session
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public boolean updatePassword(UserUpdatePasswordRequest updateRequest, HttpServletRequest request) {
        // 1. 验证用户登录状态，获取当前用户 ID
        UserVO loginUser = this.getLoginUser(request);
        Long userId = loginUser.getId();

        String oldPassword = updateRequest.getOldPassword();
        String newPassword = updateRequest.getNewPassword();

        if (StringUtils.isAnyBlank(oldPassword, newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码与新密码均不能为空");
        }

        if (newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能少于 6 位");
        }

        // 2. 查询数据库中该用户的真实密文
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "当前用户状态异常");
        }

        // 3. 对输入的旧密码进行加盐 MD5 加密比对
        String encryptOldPassword = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes());

        if (!user.getPassword().equals(encryptOldPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码输入错误，校验失败");
        }

        // 4. 对新密码进行同样的加盐 MD5 加密并更新存入数据库
        String encryptNewPassword = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());
        user.setPassword(encryptNewPassword);

        return this.updateById(user);
    }
}