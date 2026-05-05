package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.api.middleware.AuthMiddleware;
import com.cashier.dao.UserDAO;
import com.cashier.model.User;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理 REST API
 */
public class UserApiController {
    private static final Logger logger = LoggerFactory.getLogger(UserApiController.class);
    
    /**
     * 检查管理员权限
     */
    private static boolean checkAdmin(Context ctx) {
        User user = ctx.attribute("currentUser");
        if (user == null) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(Map.of("success", false, "message", "未登录"));
            return false;
        }
        if (!"管理员".equals(user.role)) {
            ctx.status(HttpStatus.FORBIDDEN)
               .json(Map.of("success", false, "message", "权限不足"));
            return false;
        }
        return true;
    }
    
    /**
     * 用户列表
     * GET /api/users
     */
    public static void list(Context ctx) {
        if (!checkAdmin(ctx)) return;
        
        try {
            List<User> users = UserDAO.findAll();
            
            // 移除密码字段
            users.forEach(u -> u.password = null);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", users);
            result.put("total", users.size());
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取用户列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 用户详情
     * GET /api/users/:id
     */
    public static void get(Context ctx) {
        if (!checkAdmin(ctx)) return;
        
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        
        try {
            User user = UserDAO.findById(id);
            if (user == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "用户不存在"));
                return;
            }
            
            user.password = null;
            ctx.json(Map.of("success", true, "data", user));
        } catch (Exception e) {
            logger.error("获取用户详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取用户详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建用户
     * POST /api/users
     */
    public static void create(Context ctx) {
        if (!checkAdmin(ctx)) return;
        
        UserRequest request = ctx.bodyAsClass(UserRequest.class);
        
        try {
            // 检查用户名是否已存在
            if (UserDAO.findByUsername(request.username) != null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "用户名已存在"));
                return;
            }
            
            User user = new User();
            user.username = request.username;
            user.password = request.password; // TODO: 加密存储
            user.name = request.name != null ? request.name : request.username;
            user.role = request.role != null ? request.role : "收银员";
            user.email = request.email != null ? request.email : "";
            user.active = request.active != null ? request.active : true;
            
            UserDAO.insert(user);
            
            user.password = null;
            logger.info("创建用户: {}", user.username);
            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", user, "message", "用户创建成功"));
        } catch (Exception e) {
            logger.error("创建用户失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新用户
     * PUT /api/users/:id
     */
    public static void update(Context ctx) {
        if (!checkAdmin(ctx)) return;
        
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        UserRequest request = ctx.bodyAsClass(UserRequest.class);
        
        try {
            User user = UserDAO.findById(id);
            if (user == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "用户不存在"));
                return;
            }
            
            if (request.name != null) user.name = request.name;
            if (request.password != null && !request.password.isEmpty()) {
                user.password = request.password; // TODO: 加密存储
            }
            if (request.role != null) user.role = request.role;
            if (request.email != null) user.email = request.email;
            if (request.active != null) user.active = request.active;
            
            UserDAO.update(user);
            
            user.password = null;
            logger.info("更新用户: {}", user.username);
            ctx.json(Map.of("success", true, "data", user, "message", "用户更新成功"));
        } catch (Exception e) {
            logger.error("更新用户失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "更新用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除用户
     * DELETE /api/users/:id
     */
    public static void delete(Context ctx) {
        if (!checkAdmin(ctx)) return;
        
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        
        try {
            User user = UserDAO.findById(id);
            if (user == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "用户不存在"));
                return;
            }
            
            // 不允许删除自己
            User currentUser = ctx.attribute("currentUser");
            if (currentUser != null && currentUser.id == id) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "不能删除自己的账号"));
                return;
            }
            
            UserDAO.delete(id);
            
            logger.info("删除用户: {}", user.username);
            ctx.json(Map.of("success", true, "message", "用户删除成功"));
        } catch (Exception e) {
            logger.error("删除用户失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "删除用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 用户请求DTO
     */
    public static class UserRequest {
        public String username;
        public String password;
        public String name;
        public String role;
        public String email;
        public Boolean active;
    }
}