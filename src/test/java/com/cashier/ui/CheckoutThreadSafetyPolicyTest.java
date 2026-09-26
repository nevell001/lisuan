package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结账 worker 的线程安全门禁（TD-007）。
 *
 * <p>两个收银台的结账都在 daemon 线程里跑，而 {@code cartItems}/{@code cartList}、
 * {@code inventoryMap}、{@code currentMember}、{@code appliedPromotion} 都归 FX 线程所有。
 * worker 一旦直接迭代/写入它们，FX 线程就可能读到"写了一半"的集合或对象
 * （{@code executeTransaction} 会迭代明细、把扣减后的商品 put 回 map、并就地改写会员状态）。</p>
 *
 * <p>约定：worker 只吃**切线程前拍好的快照**，结账结果用 {@code Platform.runLater} 回到 FX 线程处理。</p>
 */
@DisplayName("结账 worker 线程安全门禁")
class CheckoutThreadSafetyPolicyTest {

    /** worker 内不得出现的 FX 侧可变状态名。 */
    private static final List<String> FX_OWNED_STATE =
        List.of("cartItems", "cartList", "inventoryMap", "currentMember", "appliedPromotion");

    /** 结账输入快照：必须在切线程前创建，并原样传给 executeTransaction。 */
    private static final List<String> CHECKOUT_SNAPSHOTS =
        List.of("itemsAtSale", "memberForSale", "inventoryForSale");

    @Test
    @DisplayName("两个收银台的结账 worker 只能使用切线程前的快照")
    void checkoutWorkersConsumeSnapshotsOnly() throws Exception {
        assertWorkerUsesSnapshots("controller/CartController.java");
        assertWorkerUsesSnapshots("controller/TouchCartController.java");
    }

    private static void assertWorkerUsesSnapshots(String file) throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/cashier", file));
        String method = methodBody(source, "private void completeTransaction(");

        int workerStart = method.indexOf("new Thread(");
        assertTrue(workerStart > 0, file + " 的 completeTransaction 应在后台线程里结账");
        String before = method.substring(0, workerStart);
        String worker = method.substring(workerStart);

        for (String state : FX_OWNED_STATE) {
            assertFalse(worker.contains(state),
                file + " 的结账 worker 不得直接访问 FX 侧状态 " + state
                    + "：应在切线程前拍快照、结果用 Platform.runLater 回填");
        }

        for (String snapshot : CHECKOUT_SNAPSHOTS) {
            assertTrue(before.contains(snapshot + " ="),
                file + " 应在切线程前创建结账快照 " + snapshot);
            assertTrue(worker.contains(snapshot),
                file + " 的结账 worker 应使用快照 " + snapshot);
        }

        String args = callArguments(worker, "TransactionService.executeTransaction(");
        for (String snapshot : CHECKOUT_SNAPSHOTS) {
            assertTrue(args.contains(snapshot),
                file + " 必须把快照传给 executeTransaction（否则等于在 worker 里直接改 FX 侧状态），实际参数: " + args);
        }
    }

    /** 取出某个调用的实参文本（按括号配对）。 */
    private static String callArguments(String source, String call) {
        int start = source.indexOf(call);
        assertTrue(start >= 0, "找不到调用: " + call);
        int open = source.indexOf('(', start + call.length() - 1);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new IllegalStateException("调用未闭合: " + call);
    }

    /** 取出指定方法（按大括号配对）的方法体。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法签名: " + signature);
        int open = source.indexOf('{', start + signature.length());
        assertTrue(open >= 0, "方法没有方法体: " + signature);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new IllegalStateException("方法体未闭合: " + signature);
    }
}
