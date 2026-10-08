package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 两个收银台加购/改数量时都必须用**刚从库里读到的商品行**，读库失败不得回落到旧快照（2026-10 审计 F5/F11）。
 *
 * <p>为什么需要门禁：加购时会重新查库拿最新行，但此前</p>
 * <ul>
 *   <li>触屏台只把新行用于库存校验，购物车行仍用网格里的旧对象 → 改价后按旧价结算；</li>
 *   <li>标准台读到 null（查库失败）时静默回落到调用方旧快照，而注释恰恰承诺"确保使用最新库存"。</li>
 * </ul>
 * <p>两个控制器都依赖 JavaFX/FXML，无头环境跑不了行为测试，所以用源码门禁钉住形状；
 * 小计重算的行为由 {@code CartItemTest.refreshProductRepricesLine} 覆盖。</p>
 */
@DisplayName("收银台取价门禁")
class PosStalePricePolicyTest {

    private static final String CART = "src/main/java/com/cashier/controller/CartController.java";
    private static final String TOUCH = "src/main/java/com/cashier/controller/TouchCartController.java";

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    @Test
    @DisplayName("标准收银台：查库失败必须显式拒绝，已在车里的行必须换成刚读到的行")
    void standardPosUsesFreshRow() throws Exception {
        String source = read(CART);

        assertTrue(source.contains("if (context.latestProduct() == null)"),
            "查库失败必须显式提示并放弃加购，不能用旧快照兜底（F11）");
        assertTrue(source.contains("cartItem.refreshProduct(product)"),
            "已在购物车里的行必须换成刚读到的商品行，否则改价后仍按旧价结算（F5）");
    }

    @Test
    @DisplayName("触屏收银台：新建购物车行必须用刚读到的行，不能再用网格里的旧对象")
    void touchPosUsesFreshRow() throws Exception {
        String source = read(TOUCH);

        assertTrue(source.contains("if (fresh == null)"),
            "查库失败必须显式提示并放弃加购（F11）");
        assertTrue(source.contains("new CartItem(fresh, 1)"),
            "新建购物车行必须用刚读到的行（F5）");
        assertTrue(source.contains("existing.refreshProduct(fresh)"),
            "已有行必须刷新商品对象，否则改价后仍按旧价结算（F5）");
        assertFalse(source.contains("new CartItem(product, 1)"),
            "用网格里的旧对象建行正是 F5 的形态，不得回归");
    }
}
