package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.ActivationStatus;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.RedisKeyUtil;
import com.nowcoder.community.utils.ThreadLocalUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.internal.LoadingCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.util.*;
import java.util.List;

@Controller
public class DiscussPostController implements ActivationStatus {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private ThreadLocalUtil threadLocalUtil;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer producer;

    @Autowired
    private RedisTemplate redisTemplate;

    //返回一个视图index.html
    @GetMapping("/index")
    public String getIndexPage(Model model, Page page, @RequestParam(name = "orderModel", defaultValue = "0") int orderModel){
        // 方法调用前，SpringMVC会自动实例化Model和Page,并将Page注入Model.
        // 所以，在thymeleaf中可以直接访问Page对象中的数据.
        page.setPath("/index?orderModel="+orderModel); //在模板中复用的请求路径
        page.setRows(discussPostService.getPostNum(0));

        if(page.getCurrentPage()>page.getTotal()){
            page.setCurrentPage(page.getTotal());
        }

        User myUser = threadLocalUtil.getValue();

        List<Map<String, Object>> discussPosts = new ArrayList<>();
        //得到【帖子列表】
        List<DiscussPost> posts = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), orderModel);
        //获得【用户信息】
        if (posts!=null){
           for(DiscussPost post : posts){
               Map<String, Object> map = new HashMap<>();
               map.put("post", post);
               User user = userService.findUserById(post.getUserId());
               map.put("user", user);
               //点赞数
               Long likeCount = likeService.findLikeCount(ENTITY_TYPE_POST, post.getId());
               map.put("likeCount", likeCount);
               int likeStatus = myUser==null ? 0 : likeService.findLikeStatus(myUser.getId(), ENTITY_TYPE_POST, post.getId());
               map.put("likeStatus", likeStatus);
               discussPosts.add(map);
           }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderModel", orderModel);
        return "index"; //这是返回的视图index.html
    }

    //发布帖子
    @LoginRequired
    @PostMapping("/discuss/add")
    @ResponseBody
    public String addDiscussPost(String title, String content){
        User user = threadLocalUtil.getValue();
        if(user==null){
            return CommunityUtil.toJSONString(403, "请先登陆您的账户");
        }
        if(StringUtils.isBlank(title) || StringUtils.isBlank(content)){
            return CommunityUtil.toJSONString(402, "内容或标题不能为空");
        }
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        int i = discussPostService.addDiscussPost(post);

        //触发发布事件（存入es）
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        producer.fireEvent(event);

        // 存放需要修改score的帖子的id
        String postScoreRefreshKey = RedisKeyUtil.postScoreRefreshKey();
        redisTemplate.opsForSet().add(postScoreRefreshKey, post.getId());

        return CommunityUtil.toJSONString(0, "发布成功！");
    }

    //查看帖子
    @GetMapping("/discuss/detail/{postId}")
    public String findDiscussPostById(@PathVariable("postId") int postId, Model model, Page page){
        User user = threadLocalUtil.getValue();
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        model.addAttribute("post", post);

        //帖子点赞数
        Long likeCount = likeService.findLikeCount(ENTITY_TYPE_POST, postId);
        model.addAttribute("likeCount", likeCount);
        int likeStatus = user==null ? 0 : likeService.findLikeStatus(user.getId(), ENTITY_TYPE_POST, post.getId());
        model.addAttribute("likeStatus", likeStatus);

        //作者信息
        User author = userService.findUserById(post.getUserId());
        model.addAttribute("author", author);

        //评论列表
        page.setRows(post.getCommentCount());
        page.setPath("/discuss/detail/"+postId);
        int offset = page.getOffset();
        int limit = page.getLimit();

        //评论
        List<Comment> comments = commentService.findCommentList(ENTITY_TYPE_POST, postId, offset, limit);
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if(comments!=null){
            for(Comment comment : comments){
                HashMap<String, Object> commentMap = new HashMap<>();
                //主评论
                commentMap.put("postComment", comment);

                //主评论的点赞数
                likeCount = likeService.findLikeCount(ENTITY_TYPE_Replay, comment.getId());
                commentMap.put("likeCount", likeCount);
                likeStatus = user==null ? 0 : likeService.findLikeStatus(user.getId(), ENTITY_TYPE_Replay, comment.getId());
                commentMap.put("likeStatus", likeStatus);

                //评论作者
                commentMap.put("user", userService.findUserById(comment.getUserId()));

                //回复：评论的评论
                List<Comment> replays = commentService.findCommentList(ENTITY_TYPE_Replay, comment.getId(), 0, Integer.MAX_VALUE);
                List<Map<String, Object>> replayVoList = new ArrayList<>();
                if(replays!=null){
                    for(Comment replay : replays){
                        HashMap<String, Object> replayMap = new HashMap<>();
                        //回复
                        replayMap.put("replayComment", replay);
                        //回复的作者
                        replayMap.put("user", userService.findUserById(replay.getUserId()));
                        //评论目标
                        if(replay.getTargetId()!=0){
                            replayMap.put("target", userService.findUserById(replay.getTargetId()));
                        }
                        else replayMap.put("target", null);

                        //回复的赞
                        likeCount = likeService.findLikeCount(ENTITY_TYPE_Replay, replay.getId());
                        replayMap.put("likeCount", likeCount);
                        likeStatus = user==null ? 0 : likeService.findLikeStatus(user.getId(), ENTITY_TYPE_Replay, replay.getId());
                        replayMap.put("likeStatus", likeStatus);

                        replayVoList.add(replayMap);
                    }
                    commentMap.put("replays", replayVoList);
                }
                //回复梳理
                commentMap.put("replayCount", replays.size());
                commentVoList.add(commentMap);
            }
        }
        model.addAttribute("comments", commentVoList);
        return "/site/discuss-detail";
    }

    //置顶
    @PostMapping("/discuss/top")
    @ResponseBody
    public String setTop(int postId){
        discussPostService.updateType(postId, 1);

        //触发发布事件（存入es）
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(threadLocalUtil.getValue().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId);
        producer.fireEvent(event);

        return CommunityUtil.toJSONString(0, null);
    }

    //精华
    @PostMapping("/discuss/nb")
    @ResponseBody
    public String setNb(int postId){
        discussPostService.updateStatus(postId, 1);

        //触发发布事件（存入es）
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(threadLocalUtil.getValue().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId);
        producer.fireEvent(event);

        // 存放需要修改score的帖子的id
        String postScoreRefreshKey = RedisKeyUtil.postScoreRefreshKey();
        redisTemplate.opsForSet().add(postScoreRefreshKey, postId);

        return CommunityUtil.toJSONString(0, null);
    }

    //删除
    @PostMapping("/discuss/delete")
    @ResponseBody
    public String setDelete(int postId){
        discussPostService.updateStatus(postId, 2);

        //触发删除事件（存入es）
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(threadLocalUtil.getValue().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId);
        producer.fireEvent(event);

        return CommunityUtil.toJSONString(0, null);
    }

}
