package com.cashier.api.sync;

import com.cashier.model.User;
import io.javalin.websocket.WsContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * WebSocket 客户端上行消息策略测试
 * 回归 P1：客户端上行只允许控制类消息（PING/REQUEST_SYNC），
 * 业务事件（交易/商品/会员/库存等）只能由服务端广播，禁止伪造事件转发。
 */
@DisplayName("WebSocket 客户端上行消息策略测试")
class SyncManagerMessagePolicyTest {

    private final SyncManager syncManager = SyncManager.getInstance();
    private WsContext currentCtx;

    @AfterEach
    void cleanup() {
        if (currentCtx != null) {
            syncManager.unregisterTerminal(currentCtx);
            currentCtx = null;
        }
    }

    /**
     * 注册一个终端，返回记录其收到的所有 WS 消息的列表（不含注册阶段的列表推送）。
     */
    private List<String> registerMockTerminal() {
        WsContext ctx = mock(WsContext.class);
        currentCtx = ctx;
        List<String> sent = new ArrayList<>();
        doAnswer(invocation -> {
            sent.add(invocation.getArgument(0));
            return null;
        }).when(ctx).send(anyString());

        User user = new User();
        user.id = 7;
        user.username = "cashier_test";
        syncManager.registerTerminal(ctx, user, "T1");

        sent.clear(); // 忽略注册阶段的 ONLINE_TERMINALS 消息
        return sent;
    }

    @Test
    @DisplayName("伪造业务事件（交易/会员/库存/商品）不得被广播")
    void forgedBusinessEventsAreNotBroadcast() {
        List<String> sent = registerMockTerminal();

        syncManager.handleMessage(currentCtx, "{\"type\":\"TRANSACTION_CREATED\",\"data\":{\"fake\":true}}");
        syncManager.handleMessage(currentCtx, "{\"type\":\"MEMBER_UPDATED\",\"data\":{}}");
        syncManager.handleMessage(currentCtx, "{\"type\":\"INVENTORY_CHANGED\",\"data\":{}}");
        syncManager.handleMessage(currentCtx, "{\"type\":\"PRODUCT_UPDATED\",\"data\":{}}");

        assertTrue(sent.isEmpty(), "伪造业务事件不得向任何终端转发广播: " + sent);
    }

    @Test
    @DisplayName("PING 心跳仍被正常应答")
    void pingStillAnswered() {
        List<String> sent = registerMockTerminal();

        syncManager.handleMessage(currentCtx, "{\"type\":\"PING\"}");

        assertEquals(1, sent.size(), "PING 应恰好收到一次 PONG 应答");
        assertTrue(sent.get(0).contains("PONG"), "应答应包含 PONG: " + sent.get(0));
    }

    @Test
    @DisplayName("超过大小上限的消息被丢弃")
    void oversizedMessageIsDropped() {
        List<String> sent = registerMockTerminal();

        String oversized = "{\"type\":\"PING\",\"data\":\"" + "x".repeat(20_000) + "\"}";
        syncManager.handleMessage(currentCtx, oversized);

        assertTrue(sent.isEmpty(), "超长消息应被丢弃，不应有 PONG 应答");
    }

    @Test
    @DisplayName("每连接消息速率受限，超过窗口阈值后丢弃")
    void messageRateIsLimitedPerConnection() {
        List<String> sent = registerMockTerminal();

        // 快速发送远多于窗口上限（30/10 秒）的心跳：只有前 30 条被应答
        for (int i = 0; i < 40; i++) {
            syncManager.handleMessage(currentCtx, "{\"type\":\"PING\"}");
        }

        assertEquals(30, sent.size(), "速率窗口外的消息应被丢弃");
        assertTrue(sent.stream().allMatch(s -> s.contains("PONG")));
    }
}
