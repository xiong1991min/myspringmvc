package com.xm.myspringmvc.service.impl;

import com.xm.myspringmvc.annotation.Qualifier;
import com.xm.myspringmvc.annotation.Service;
import com.xm.myspringmvc.dao.UserDao;
import com.xm.myspringmvc.service.UserService;

@Service("userServiceImpl")
public class UserServiceImpl implements UserService {

    @Qualifier("userDaoImpl")
    private UserDao userDao;

    public void insert() {
        System.out.println("userServiceImpl.insert() start");
        userDao.insert();
        System.out.println("userServiceImpl.insert() end");
    }
}
