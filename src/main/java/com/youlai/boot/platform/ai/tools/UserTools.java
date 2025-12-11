package com.youlai.boot.platform.ai.tools;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.youlai.boot.system.model.entity.User;
import com.youlai.boot.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 基于 Spring AI Tool 的用户管理工具
 *
 * 提供受控的 CRUD 能力，供 LLM 通过 Tool Calling 调用
 */
@Component
@RequiredArgsConstructor
public class UserTools {

    private final UserService userService;

    @Tool(description = "根据关键字在用户列表中筛选用户")
    public String queryUser(
            @ToolParam(description = "搜索关键字，用于在列表中搜索筛选") String keywords
    ) {
        // 返回搜索关键字，前端会在用户列表页面进行筛选
        return "将在用户列表中搜索：" + keywords;
    }

    @Tool(description = "根据用户名更新用户昵称")
    public String updateUserNickname(
            @ToolParam(description = "用户名") String username,
            @ToolParam(description = "新的昵称") String nickname
    ) {

        boolean ok = userService.update(new LambdaUpdateWrapper<User>()
                .eq(User::getUsername, username)
                .set(User::getNickname, nickname)
        );
        return ok ? "用户昵称更新成功" : "用户昵称更新失败";
    }
}







