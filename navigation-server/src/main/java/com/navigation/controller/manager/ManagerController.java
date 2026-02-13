package com.navigation.controller.manager;

import com.navigation.entity.Manager;
import com.navigation.service.ManagerService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Api(tags = "管理员管理") // 为该控制器添加 Swagger 分组名称
@RequestMapping("/manager") // 统一管理接口路径
public class ManagerController {

    @Autowired
    private ManagerService managerService;

    /**
     * 管理员登录接口
     *
     * @param userName 管理员用户名
     * @param password 管理员密码
     * @return 登录结果
     */
        @PostMapping("/login")
        @ApiOperation(value = "管理员登录", notes = "管理员使用用户名和密码进行登录")
        @ApiResponses({
                @ApiResponse(code = 200, message = "登录成功"),
                @ApiResponse(code = 400, message = "用户名或密码错误")
        })
        public String managerLogin(
                @ApiParam(value = "管理员用户名", required = true, example = "admin") @RequestParam String userName,
                @ApiParam(value = "管理员密码", required = true, example = "123456") @RequestParam String password) {

            Manager manager = managerService.managerlogin(userName, password);
            if (manager != null) {
                return "登录成功";
            } else {
                return "用户名或密码错误";
            }
        }
    }