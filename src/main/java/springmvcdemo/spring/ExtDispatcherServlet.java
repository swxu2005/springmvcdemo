package springmvcdemo.spring;

import springmvcdemo.annotation.ExtController;
import springmvcdemo.annotation.ExtRequestMapping;
import springmvcdemo.annotation.ExtResponseBody;
import springmvcdemo.utils.ClassUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 自定义前端控制器<br>
 * 手写springmvc 原理分析<br>
 * 1.创建一个前端控制器（）ExtDispatcherServlet 拦截所有请求(springmvc 基于servlet实现)<br>
 * ####2.初始化操作 重写servlet init 方法<br>
 * #######2.1 将扫包范围所有的类,注入到springmvc容器里面，存放在Map集合中 key为默认类名小写，value 对象<br>
 * #######2.2 将url映射和方法进行关联 <br>
 * ##########2.2.1 判断类上是否有注解,使用java反射机制循环遍历方法 ,判断方法上是否存在注解，进行封装url和方法对应存入集合中<br>
 * ####3.处理请求 重写Get或者是Post方法 <br>
 * ##########3.1
 * 获取请求url,从urlBeans集合获取实例对象,获取成功实例对象后,调用urlMethods集合获取方法名称,使用反射机制执行 2. 作者:
 * 每特教育-余胜军<br>
 * 联系方式:QQ644064779|WWW.itmayiedu.com<br>
 * 作者: 每特教育-余胜军<br>
 * 联系方式:QQ644064779|WWW.itmayiedu.com<br>
 */
@WebServlet(name = "dispatcherServlet", urlPatterns = "/*")
public class ExtDispatcherServlet extends HttpServlet {
	// springmvc 容器对象 key:类名id,value:对象
	private ConcurrentHashMap<String, Object> springmvcBeans = new ConcurrentHashMap<>();
	// springmvc 容器对象 key:请求地址,value:类
	private ConcurrentHashMap<String, Object> urlBeans = new ConcurrentHashMap<>();
	// springmvc 容器对象 key:请求地址,value:方法名称
	private ConcurrentHashMap<String, String> urlMethods = new ConcurrentHashMap<>();
	// springmvc 容器对象 key:请求地址,value:是否返回JSON数据
	private ConcurrentHashMap<String, Boolean> urlReturnJson = new ConcurrentHashMap<>();

	@Override
	public void init() throws ServletException {
		// 1.获取当前包下的所有的类
		List<Class<?>> classes = ClassUtil.getClasses("springmvcdemo.controller");
		// 2.将扫包范围所有的类,注入到springmvc容器里面，存放在Map集合中 key为默认类名小写，value 对象
		try {
			findClassMVCAnnotation(classes);
		} catch (Exception e) {
			// TODO: handle exception
		}
		// 3.将url映射和方法进行关联
		handlerMapping();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// #################处理请求####################
		// 1.获取请求url地址
		String requestURI = req.getRequestURI();
		if (requestURI == null || requestURI.equals("")) {
			return;
		}
		// 2.从Map集合中获取控制对象
		Object object = urlBeans.get(requestURI);
		if (object == null) {
			resp.getWriter().println(" not found 404  url");
			return;
		}
		// 3.使用url地址获取方法
		String methodName = urlMethods.get(requestURI);
		if (methodName == null || methodName.equals("")) {
			resp.getWriter().println(" not found method");
		}
		// 4.使用java的反射机制调用方法
		String result = (String) methodInvoke(object, methodName);
		// 5.调用视图转换器渲染给页面展示
		if (urlReturnJson.get(requestURI)) {
			outputJsonContent(result, req, resp);
		} else {
			extResourceViewResolver(result, req, resp);
		}
	}

	private void outputJsonContent(String result, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter out = resp.getWriter();
		out.print(result);
	}

	private void extResourceViewResolver(String pageName, HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		// 根路径
		String prefix = "/";
		String suffix = ".jsp";
		req.getRequestDispatcher(prefix + pageName + suffix).forward(req, res);
	}


	private Object methodInvoke(Object object, String methodName) {
		try {
			Class<? extends Object> classInfo = object.getClass();
			for (Method method : classInfo.getMethods()) {
				if (method.getName().equals(methodName)) {
					Object result = method.invoke(object);
					return result;
				}
			}
//			Method method = classInfo.getMethod(methodName);
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	// 2.将扫包范围所有的类,注入到springmvc容器里面，存放在Map集合中 key为默认类名小写，value 对象
	public void findClassMVCAnnotation(List<Class<?>> classes)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		for (Class<?> classInfo : classes) {
			// 判断类上是否有加上注解
			ExtController extController = classInfo.getDeclaredAnnotation(ExtController.class);
			if (extController != null) {
				// 默认类名是小写
				String beanId = ClassUtil.toLowerCaseFirstOne(classInfo.getSimpleName());
				// 实例化对象
				Object object = ClassUtil.newInstance(classInfo);
				springmvcBeans.put(beanId, object);
			}
		}
	}

	// 3.将url映射和方法进行关联
	public void handlerMapping() {
		// 1.遍历springmvc bean容器 判断类上属否有url映射注解
		for (Map.Entry<String, Object> mvcBean : springmvcBeans.entrySet()) {
			// 2.遍历所有的方法上是否有url映射注解
			// 获取bean的对象
			Object object = mvcBean.getValue();
			// 3.判断类上是否有加url映射注解
			Class<? extends Object> classInfo = object.getClass();
			ExtRequestMapping declaredAnnotation = classInfo.getDeclaredAnnotation(ExtRequestMapping.class);
			String baseUrl = "";
			if (declaredAnnotation != null) {
				// 获取类上的url映射地址
				baseUrl = declaredAnnotation.value();
			}
			// 4.判断方法上是否有加url映射地址
			Method[] declaredMethods = classInfo.getDeclaredMethods();
			for (Method method : declaredMethods) {
				// 判断方法上是否有加url映射注解
				ExtRequestMapping methodExtRequestMapping = method.getDeclaredAnnotation(ExtRequestMapping.class);
				if (methodExtRequestMapping != null) {
					String methodUrl = baseUrl + methodExtRequestMapping.value();
					// springmvc 容器对象 keya:请求地址 ,vlue类
					urlBeans.put(methodUrl, object);
					// springmvc 容器对象 key:请求地址 ,value 方法名称
					urlMethods.put(methodUrl, method.getName());
					// 判断方法返回JSON，还是进行JSP页面渲染
					urlReturnJson.put(methodUrl, method.isAnnotationPresent(ExtResponseBody.class));
				}
			}
		}

	}

}
