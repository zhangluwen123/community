package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.utils.ActivationStatus;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.MailClient;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements ActivationStatus {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    @Value("${community.path.domain}")
    private String path;

    @Value("${server.servlet.context-path}")
    private String webContext;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    public User findUserById(int userId){
        User user = getUserCache(userId);
        if(user==null){
            user = initCache(userId);
        }
        return user;
    }

    //注册
    //返回【错误信息】，用map封装
    public Map<String, Object> register(User user){
        Map<String, Object> map = new HashMap<>();
        if (user==null){
            //参数传递错误
            throw new IllegalArgumentException("注册参数错误！");
        }
        //判断字段的合法性
        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg", "用户名不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg", "邮箱不能为空");
            return map;
        }

        //判断是存在
        User name = userMapper.selectByName(user.getUsername());
        if(name!=null){
            map.put("usernameMsg", "用户名已存在");
            return map;
        }
        User email = userMapper.selectByEmail(user.getEmail());
        if(email!=null){
            map.put("emailMsg", "该邮箱已被激活");
            return map;
        }

        //信息合法 -》 存入数据库
        user.setSalt(CommunityUtil.getUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.getUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateDate(new Date());
        userMapper.insertUser(user);

        int id = user.getId();

        //发送激活邮件
        Context context = new Context();
        context.setVariable("username", user.getUsername());
        //url: http://localhost:8080/community/activation/userId/激活码
        String url = path + webContext + "/activation" + "/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);

        mailClient.sendEmail(user.getEmail(), "激活邮件", content);

        return null;
    }

    //激活账户
    public int activationAccount(int userId, String activationCode){
        User user = userMapper.selectById(userId);
        int status = user.getStatus();
        if (activationCode.equals(user.getActivationCode())){
            if(status==0){
                userMapper.updateStatus(userId, 1);
                return ACTIVATION_SUCCESS;
            }
            else{
                return ACTIVATION_REPEAT;
            }
        }
        else{
            return ACTIVATION_FAULT;
        }
    }

    //登陆
    public Map<String, Object> login(String username, String password, int expiredSeconds){
        Map<String, Object> map = new HashMap<>();
        //判断空值（code在controller中判断）
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg", "用户名不能为空");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg", "密码不能为空");
            return map;
        }

        //判断值的合法性
        User user = userMapper.selectByName(username);
        if(user==null){
            map.put("usernameMsg", "用户名不存在");
            return map;
        }
        password = CommunityUtil.md5(password+user.getSalt());
        if(!password.equals(user.getPassword())){
            map.put("passwordMsg", "密码错误");
            return map;
        }

        //生成凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.getUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000L));
//        loginTicketMapper.insertTicket(loginTicket);
        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    //退出登陆
    public void logout(String ticket){
//        loginTicketMapper.updateTicketStatus(ticket, 1);
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
    }

    //查找登陆凭证
    public LoginTicket getLoginTicket(String ticket){
//        return loginTicketMapper.selectTicket(ticket);
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
    }

    //修改headerUrl
    public int updateHeaderUrl(int userId, String headerUrl){
        int i = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return i;
    }

    //修改密码
    public int updatePassword(int userId, String password){
        int i = userMapper.updatePassword(userId, password);
        clearCache(userId);
        return i;
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    //得到缓存中的用户信息
    private User getUserCache(int userId){
        String userKey = RedisKeyUtil.getUserCache(userId);
        User user = (User) redisTemplate.opsForValue().get(userKey);
        return user;
    }

    //初始化缓存中的用户信息
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserCache(userId);
        redisTemplate.opsForValue().set(userKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    //数据变更时删除缓存数据
    private void clearCache(int userId){
        String userKey = RedisKeyUtil.getUserCache(userId);
        redisTemplate.delete(userKey);
    }

    //得到用户的权限
    public Collection< ? extends GrantedAuthority> getAuthorities(int userId){
        User user = findUserById(userId);
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                int type = user.getType();
                switch (type){
                    case 1: return AUTHORITY_ADMIN;
                    case 2: return AUTHORITY_MODERATOR;
                    default: return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

}
