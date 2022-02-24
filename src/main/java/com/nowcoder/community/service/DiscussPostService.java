package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.utils.RedisKeyUtil;
import com.nowcoder.community.utils.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-second}")
    private long expireTime;

    @Autowired
    private RedisTemplate redisTemplate;

    // 帖子列表的缓存
    private LoadingCache<String, List<DiscussPost>> postsCache;

    // 帖子总数的缓存
    private LoadingCache<Integer, Integer> postNumCache;

    @PostConstruct
    public void init(){
        // 初始化帖子列表的缓存
        postsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireTime, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public @Nullable List<DiscussPost> load(@NonNull String key) throws Exception {
                        if(key==null){
                            throw new IllegalArgumentException("参数错误");
                        }

                        String[] params = key.split(":");
                        if(params==null || params.length!=2){
                            throw new IllegalArgumentException("参数错误");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // redis -> mysql
                        String redisKey = RedisKeyUtil.getCacheHotPostListKey(offset, limit);
                        List<DiscussPost> list = (List<DiscussPost>) redisTemplate.opsForValue().get(redisKey);
                        if(list!=null){
                            logger.info("find hot posts from redis");
                            return list;
                        }

                        list = discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                        // 把mysql的数据放入redis
                        redisTemplate.opsForValue().set(redisKey, list, expireTime, TimeUnit.SECONDS);
                        logger.info("find hot posts from DB");
                        return list;
                    }
                });

        // 初始化帖子总数的缓存
        postNumCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireTime, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(@NonNull Integer key) throws Exception {
                        if(key==null){
                            throw new IllegalArgumentException("参数错误");
                        }
                        // redis -> mysql
                        String redisKey = RedisKeyUtil.getCachePostCount(key);
                        Integer count = (Integer) redisTemplate.opsForValue().get(redisKey);
                        if(count!=null){
                            logger.info("find post count from redis");
                            return count;
                        }
                        logger.info("find post count from DB");
                        count = discussPostMapper.selectPostNum(0);
                        redisTemplate.opsForValue().set(redisKey, count, expireTime, TimeUnit.SECONDS);
                        return count;
                    }
                });
    }

    //【帖子】的分页查询
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int oderModel){
        // 从缓存中查热门帖子
        if(userId==0 && oderModel==1){
            return postsCache.get(offset+":"+limit);
        }
        logger.info("find hot posts from DB");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, oderModel);
    }

    //【帖子总数】
    public int getPostNum(int userId){
        // 从缓存中查帖子总数
        if(userId==0){
            return postNumCache.get(userId);
        }
        logger.info("find post count from DB");
        return discussPostMapper.selectPostNum(userId);
    }

    //发布帖子
    public int addDiscussPost(DiscussPost post){
        if(post==null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        //转移字符串中的html标签
        String title = HtmlUtils.htmlEscape(post.getTitle());
        String content = HtmlUtils.htmlEscape(post.getContent());
        //敏感词过滤
        title = sensitiveFilter.replaceSentimentWords(title);
        content = sensitiveFilter.replaceSentimentWords(content);
        //更新数据
        post.setTitle(title);
        post.setContent(content);
        //存入数据库
        return discussPostMapper.insertDiscussPost(post);
    }

    //查看帖子
    public DiscussPost findDiscussPostById(int postId){
        return discussPostMapper.selectDiscussPostById(postId);
    }

    //更新评论总数
    public int updateCommentCount(int postId, int count){
        return discussPostMapper.updateCommentCount(postId, count);
    }

    //置顶:0-普通; 1-置顶;
    public int updateType(int postId, int type){
        return discussPostMapper.updateType(postId, type);
    };

    //修改status:0-正常; 1-精华; 2-拉黑;
    public int updateStatus(int postId, int status){
        return discussPostMapper.updateStatus(postId, status);
    };

    public int updateScore(int postId, double score) {
        return discussPostMapper.updateScore(postId, score);
    }

}
