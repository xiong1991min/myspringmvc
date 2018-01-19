package com.xm.myspringmvc.servlet;

import com.xm.myspringmvc.annotation.*;
import com.xm.myspringmvc.controller.UserController;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "dispatcherServlet",urlPatterns = "/*",loadOnStartup = 1,initParams = {@WebInitParam(name="base-package",value= "com.xm.myspringmvc")})
public class DispatcherServlet extends HttpServlet {
    private String basePackage = "";//扫描的基包
    private List<String> packageNames = new ArrayList<String>();//基包下面所有的带包路径权限定类名
    private Map<String,Object> instanceMap = new HashMap<String,Object>();//注解实例化，注释上的名称，实例化对象
    private Map<String,Object> nameMap = new HashMap<String,Object>();//带包路径的权限定名称：注解上的名称
    private Map<String,Method> urlMethodMap = new HashMap<String, Method>();//URL地址和方法的映射关系
    private Map<Method,String> methodPackageMap = new HashMap<Method, String>();//methdo和权限定类名映射关系，主要为了通过method找到该方法的对象利用反射执行

    @Override
    public void init(ServletConfig config) throws ServletException {
        basePackage = config.getInitParameter("base-package");
        try{
            //1.扫描基包得到全部的带包路径的权限定名
            scanBasePackage(basePackage);
            //2.把带有@controller@service@repository的类实例化放入map中，key为注解上的名称
            instance(packageNames);
            //3.springIOC注入
            springIOC();
            //4.完成URL地址与方法的映射关系
            handlerUrlMethodMap();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 扫描包
     * @param basePackage
     */
    private void scanBasePackage(String basePackage){
        String aaa = basePackage.replaceAll("\\.","/");
        URL url = this.getClass().getClassLoader().getResource(aaa);
        File basePackageFile = new File(url.getPath());
        System.out.println("scan:" + basePackageFile);
        File[] childFiles = basePackageFile.listFiles();
        for(File file :childFiles){
            if(file.isDirectory()){//目录继续递归扫描
                scanBasePackage(basePackage+"."+file.getName());
            }else if(file.isFile()){
                packageNames.add(basePackage+"."+file.getName().split("\\.")[0]);
            }
        }
    }

    /**
     * 实例化
     */
    private void instance(List<String> packageNames) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        if(packageNames.size()<1){
            return;
        }
        for(String string : packageNames){
            Class c = Class.forName(string);
            if(c.isAnnotationPresent(Controller.class)){//isAnnotationPresent判断该类上是否有某注解
                Controller controller = (Controller) c.getAnnotation(Controller.class);
                String controllerName = controller.value();
                instanceMap.put(controllerName,c.newInstance());
                nameMap.put(string,controllerName);
                System.out.println("Controller:" + string + ",value:" + controller.value());
            }else if(c.isAnnotationPresent(Service.class)){
                Service service = (Service) c.getAnnotation(Service.class);
                String serviceName = service.value();
                instanceMap.put(serviceName,c.newInstance());
                nameMap.put(string,serviceName);
                System.out.println("Service:" + string + ",value:" + service.value());
            }else if(c.isAnnotationPresent(Repository.class)){
                Repository repository = (Repository) c.getAnnotation(Repository.class);
                String repositoryName = repository.value();
                instanceMap.put(repositoryName,c.newInstance());
                nameMap.put(string,repositoryName);
                System.out.println("Repository:" + string + ",value:" + repository.value());
            }
        }
    }

    /**
     * 依赖注入
     * @throws IllegalAccessException
     */
    private void springIOC() throws IllegalAccessException {
        for(Map.Entry<String,Object> entry : instanceMap.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field : fields){
                if(field.isAnnotationPresent(Qualifier.class)){
                    String name = field.getAnnotation(Qualifier.class).value();
                    field.setAccessible(true);
                    field.set(entry.getValue(),instanceMap.get(name));//将entry.getValue()对象的field属性设置值为instanceMap.get(name)
                }
            }
        }
    }

    /**
     * URL映射处理
     * @throws ClassNotFoundException
     */
    private void handlerUrlMethodMap() throws ClassNotFoundException {
        if(packageNames.size()<1){
            return;
        }
        for (String string : packageNames){
            Class c = Class.forName(string);
            if(c.isAnnotationPresent(Controller.class)){
                Method[] methods = c.getMethods();
                StringBuffer baseUrl = new StringBuffer();
                if(c.isAnnotationPresent(RequestMapping.class)){
                    RequestMapping requestMapping = (RequestMapping)c.getAnnotation(RequestMapping.class);
                    baseUrl.append(requestMapping.value());
                }
                for(Method method : methods){
                    if(method.isAnnotationPresent(RequestMapping.class)){
                        RequestMapping requestMapping = (RequestMapping)method.getAnnotation(RequestMapping.class);
                        baseUrl.append(requestMapping.value());

                        urlMethodMap.put(baseUrl.toString(),method);
                        methodPackageMap.put(method,string);
                    }
                }
            }
        }
    }

    /**
     * 继承HttpServlet的service方法（核心处理请求的方法）,处理请求
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod();
        if("GET".equalsIgnoreCase(method)){
            doGet(req,resp);
        }else if("POST".equalsIgnoreCase(method)){
            doPost(req,resp);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp){
        doPost(req,resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp){
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = url.replaceAll(contextPath,"");
        Method method = urlMethodMap.get(path);
        if(method != null){
            //通过method拿到controller对象，准备反射执行
            String packageName = methodPackageMap.get(method);
            String controllerName = (String) nameMap.get(packageName);

            //拿到controller对象
            UserController userController = (UserController) instanceMap.get(controllerName);
            try{
                method.setAccessible(true);
                method.invoke(userController);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}

