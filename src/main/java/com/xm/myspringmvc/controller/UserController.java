package com.xm.myspringmvc.controller;

import com.xm.myspringmvc.annotation.Controller;
import com.xm.myspringmvc.annotation.Qualifier;
import com.xm.myspringmvc.annotation.RequestMapping;
import com.xm.myspringmvc.service.UserService;

@Controller("userController")
@RequestMapping("/user")
public class UserController {

    @Qualifier("userServiceImpl")
    private UserService userService;

    @RequestMapping("/insert")
    public void insert(){
        userService.insert();
    }
}
