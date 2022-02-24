package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    //查询【帖子】列表（分页查询）
    public List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int oderModel);

    //查询【帖子的总数】
    //@Param用于给参数取别名
    //如果方法只有一个参数，且会在<if>中使用，则必须用@Param给它取别名
    public int selectPostNum(@Param("userId") int userId);

    //发布帖子
    public int insertDiscussPost(DiscussPost discussPost);

    //查看帖子
    public DiscussPost selectDiscussPostById(int id);

    //更新评论数量
    int updateCommentCount(int id, int count);

    //置顶:0-普通; 1-置顶;
    int updateType(int postId, int type);

    //修改status:0-正常; 1-精华; 2-拉黑;
    int updateStatus(int postId, int status);

    int updateScore(int postId, double score);
}
