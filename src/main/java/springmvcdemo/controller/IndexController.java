package springmvcdemo.controller;


import springmvcdemo.annotation.ExtController;
import springmvcdemo.annotation.ExtRequestMapping;
import springmvcdemo.annotation.ExtResponseBody;

@ExtController
@ExtRequestMapping("/ext")
public class IndexController {

	// ext/test/?name=122&age=6440654
	@ExtRequestMapping("/test")
	@ExtResponseBody
	public String test() {
		System.out.println("手写springmvc框架...");
//		String res = "Name:" + name + ", Age:" + age;
		return "Hello World!";
	}


	@ExtRequestMapping("/page")
	public String pageTest() {
		System.out.println("手写springmvc框架...");
		return "index";
	}

}
