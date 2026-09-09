package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.dao.DAOFactory;
import com.cashier.model.PageResult;
import com.cashier.model.User;
import com.cashier.util.PasswordUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.Map;
import java.util.Objects;

/**
 * 用户管理 REST API
 */
public class UserApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(UserApiController.class);
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_DATA = "data";
    private static final String KEY_TOTAL = "total";

    private UserApiController() {}
    
    /**
     * 检查管理员权限
     */
    private static boolean checkAdmin(Context ctx) {
        User user = ctx.attribute("currentUser");
          if (user == null) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                    .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "未登录"));
            return false;
        }
        if (!"admin".equals(user.role)) {
                ctx.status(HttpStatus.FORBIDDEN)
                    .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "权限不足"));
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
            ApiPagination.PageRequest page = ApiPagination.from(ctx);
            PageResult<User> users = DAOFactory.getInstance().getUserDAO().findAll(page.page(), page.pageSize());
            
            // 移除密码字段
            users.getData().forEach(u -> u.password = null);
            
            ctx.json(ApiPagination.success(users));
        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "获取用户列表失败"));
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
            User user = DAOFactory.getInstance().getUserDAO().findById(id);
            if (user == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "用户不存在"));
                return;
            }
            
            user.password = null;
            ctx.json(Map.of(KEY_SUCCESS, true, KEY_DATA, user));
        } catch (Exception e) {
            logger.error("获取用户详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "获取用户详情失败"));
        }
    }
    
    /**
     * 创建用户
     * POST /api/users
     */
    public static void create(Context ctx) {
        if (!checkAdmin(ctx)) return;
        
        UserRequest request = ctx.bodyAsClass(UserRequest.class);
        if (request == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "请求体不能为空"));
            return;
        }
        
        try {
            // 检查用户名是否已存在
            if (DAOFactory.getInstance().getUserDAO().findByUsername(request.username) != null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "用户名已存在"));
                return;
            }
            
            User user = new User();
            user.username = request.username;
            user.password = PasswordUtil.hashPassword(request.password);
            user.name = request.name != null ? request.name : request.username;
            user.role = request.role != null ? request.role : "cashier";
            user.email = request.email != null ? request.email : "";
            user.active = Objects.requireNonNullElse(request.active, true);
            
            DAOFactory.getInstance().getUserDAO().insert(user);
            
            user.password = null;
            logger.info("创建用户: {}", user.username);
                ctx.status(HttpStatus.CREATED)
                    .json(Map.of(KEY_SUCCESS, true, KEY_DATA, user, KEY_MESSAGE, "用户创建成功"));
        } catch (Exception e) {
            logger.error("创建用户失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "创建用户失败"));
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
        if (request == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "请求体不能为空"));
            return;
        }
        
        try {
            User user = DAOFactory.getInstance().getUserDAO().findById(id);
            if (user == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "用户不存在"));
                return;
            }
            
            if (request.name != null) user.name = request.name;
            boolean passwordChanged = request.password != null && !request.password.isEmpty();
            if (passwordChanged) {
                user.password = PasswordUtil.hashPassword(request.password);
            }
            boolean roleChanged = request.role != null && !request.role.equals(user.role);
            if (roleChanged) user.role = request.role;
            if (request.email != null) user.email = request.email;
            boolean deactivated = Boolean.FALSE.equals(request.active);
            if (request.active != null) user.active = request.active;
            
            DAOFactory.getInstance().getUserDAO().update(user);

            // 密码/角色/禁用发生变化后，作废该用户已签发的全部 token（含可能泄露的旧 token）
            if (passwordChanged || roleChanged || deactivated) {
                ApiServer.getInstance().invalidateUserTokens(user.id);
            }
            
            user.password = null;
            logger.info("更新用户: {}", user.username);
            ctx.json(Map.of(KEY_SUCCESS, true, KEY_DATA, user, KEY_MESSAGE, "用户更新成功"));
        } catch (Exception e) {
            logger.error("更新用户失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "更新用户失败"));
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
            User user = DAOFactory.getInstance().getUserDAO().findById(id);
            if (user == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "用户不存在"));
                return;
            }
            
            // 不允许删除自己
            User currentUser = ctx.attribute("currentUser");
            if (currentUser != null && currentUser.id == id) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "不能删除自己的账号"));
                return;
            }
            
            DAOFactory.getInstance().getUserDAO().delete(id);
            
            // 删除用户后作废其已签发的全部 token
            ApiServer.getInstance().invalidateUserTokens(id);
            
            logger.info("删除用户: {}", user.username);
            ctx.json(Map.of(KEY_SUCCESS, true, KEY_MESSAGE, "用户删除成功"));
        } catch (Exception e) {
            logger.error("删除用户失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of(KEY_SUCCESS, false, KEY_MESSAGE, "删除用户失败"));
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
