package com.xm.myspringmvc.annotation;

import java.lang.annotation.*;


@Documented
@Target(ElementType.TYPE)//作用于类上
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
	public String value();//controller名称
}
