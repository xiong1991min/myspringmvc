package com.xm.myspringmvc.dao.impl;

import com.xm.myspringmvc.annotation.Repository;
import com.xm.myspringmvc.dao.UserDao;

@Repository("userDaoImpl")
public class UserDaoImpl implements UserDao {
    public void insert() {
        System.out.println("execute userDaoImpl.insert()");
    }
}
