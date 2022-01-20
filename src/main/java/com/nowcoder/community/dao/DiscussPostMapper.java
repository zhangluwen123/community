package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    //查询【帖子】列表（分页查询）
    public List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    //查询【帖子的总数】
    //@Param用于给参数取别名
    //如果方法只有一个参数，且会在<if>中使用，则必须用@Param给它取别名
    public int selectPostNum(@Param("userId") int userId);
}
