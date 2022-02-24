package com.nowcoder.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

//本质上是一个配置类
@SpringBootApplication
public class CommunityApplication {

	//初始化方法
	//解决netty启动冲突的问题
	@PostConstruct
	public void init(){
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}
