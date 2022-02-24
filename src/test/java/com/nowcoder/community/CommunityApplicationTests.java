package com.nowcoder.community;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@SpringBootTest
@RunWith(SpringRunner.class)
//指定配置springboot类
@ContextConfiguration(classes = CommunityApplication.class)
public class CommunityApplicationTests{

	@Autowired
	private UserMapper userMapper;

	@Test
	public void selectByIdTest(){
		User user = userMapper.selectById(1);
		Integer[] arr = {1,5,6,0};
		Arrays.sort(arr, (a, b)->(b-a));
		PriorityQueue<Map.Entry<Integer, Integer>> queue = new PriorityQueue<>((o1, o2)->(o1.getValue()));
		Map<Integer, Integer> map = new HashMap<>();
		Set<Map.Entry<Integer, Integer>> entries = map.entrySet();
		List<Integer> list = new ArrayList<>();
		int a = 7;
		double sqrt = Math.sqrt(a + 0.0);
		list.toString();
		System.out.println(user);
	}
}
