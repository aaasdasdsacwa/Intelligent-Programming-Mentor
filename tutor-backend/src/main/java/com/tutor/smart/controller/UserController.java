package com.tutor.smart.controller;

import com.tutor.smart.common.BaseResponse;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.common.ResultUtils;
import com.tutor.smart.model.dto.UserLoginRequest;
import com.tutor.smart.model.dto.UserRegisterRequest;
import com.tutor.smart.model.vo.UserVO;
import com.tutor.smart.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制层接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest registerRequest) {
        if (registerRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String checkPassword = registerRequest.getCheckPassword();

        long result = userService.userRegister(username, password, checkPassword);
        return ResultUtils.success(result, "注册成功");
    }

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public BaseResponse<UserVO> login(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {
        if (loginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        UserVO userVO = userService.userLogin(username, password, request);
        return ResultUtils.success(userVO, "登录成功");
    }

    /**
     * 获取当前登录的用户信息
     */
    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest request) {
        UserVO userVO = userService.getLoginUser(request);
        return ResultUtils.success(userVO);
    }

    /**
     * 用户注销接口
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result, "注销成功");
    }
}