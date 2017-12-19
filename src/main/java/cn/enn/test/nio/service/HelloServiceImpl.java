package cn.enn.test.nio.service;

public class HelloServiceImpl implements HelloService {

	@Override
	public String hello(String name) {
		return "hello " + name;
	}
}
