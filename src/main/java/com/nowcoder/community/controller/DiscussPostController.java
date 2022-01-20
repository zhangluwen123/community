package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DiscussPostController {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    //返回一个视图index.html
    @GetMapping("/index")
    public String getIndexPage(Model model, Page page){
        // 方法调用前，SpringMVC会自动实例化Model和Page,并将Page注入Model.
        // 所以，在thymeleaf中可以直接访问Page对象中的数据.
        page.setPath("/index"); //在模板中复用的请求路径
        page.setRows(discussPostService.getPostNum(0));

        if(page.getCurrentPage()>page.getTotal()){
            page.setCurrentPage(page.getTotal());
        }

        List<Map<String, Object>> discussPosts = new ArrayList<>();
        //得到【帖子列表】
        List<DiscussPost> posts = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        //获得【用户信息】
        if (posts!=null){
           for(DiscussPost post : posts){
               Map<String, Object> map = new HashMap<>();
               map.put("post", post);
               User user = userService.findUserById(post.getUserId());
               map.put("user", user);
               discussPosts.add(map);
           }
        }
        //
        model.addAttribute("discussPosts", discussPosts);
        return "index"; //这是返回的视图index.html
    }
}
