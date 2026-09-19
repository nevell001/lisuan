package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 控制器体积棘轮（ratchet）门禁。
 *
 * <p>两个收银台控制器一度各自涨到 2200 行上下：查库、结账、打印、弹窗、布局、样式全挤在一个类里，
 * 谁都不敢改。拆出来的部分见 CLAUDE.md「巨型控制器拆分」。这里的上限就是**当前值加少量余量**——
 * 不是"理想值"，而是防止在拆分继续推进之前又被塞回去；每次继续拆分后应把上限一起调小。</p>
 */
class ControllerSizePolicyTest {

    /** 路径 → 允许的最大行数（收紧时改小；确实需要放宽必须在 CLAUDE.md 说明理由）。 */
    private static final Map<String, Integer> LINE_CAPS = Map.of(
        "controller/TouchCartController.java", 1980,
        "controller/CartController.java", 2240);

    @Test
    @DisplayName("收银台控制器体积不得超过已拆分后的上限")
    void posControllersDoNotRegrow() throws Exception {
        for (Map.Entry<String, Integer> entry : LINE_CAPS.entrySet()) {
            String path = "src/main/java/com/cashier/" + entry.getKey();
            int lines = Files.readString(Path.of(path)).split("\n", -1).length;
            assertTrue(lines <= entry.getValue(),
                path + " 已涨到 " + lines + " 行（上限 " + entry.getValue()
                    + "）：请按职责拆分，不要把逻辑继续堆进控制器（见 CLAUDE.md「巨型控制器拆分」）");
        }
    }

    @Test
    @DisplayName("触屏收银台的视图构建必须留在视图工厂，而不是回到控制器")
    void touchViewConstructionStaysInFactory() throws Exception {
        String factory = read("src/main/java/com/cashier/controller/TouchCartViewFactory.java");
        String touch = read("src/main/java/com/cashier/controller/TouchCartController.java");

        assertTrue(factory.contains("static VBox productCard(Product p, Consumer<Product> onAddToCart)"),
            "商品卡片构建应属于 TouchCartViewFactory");
        assertTrue(factory.contains("static HBox cartRow(CartItem item, Consumer<CartItem> onIncrement"),
            "购物车行构建应属于 TouchCartViewFactory");
        assertTrue(factory.contains("static GridPane cashDenominationGrid("),
            "现金弹窗的控件构建应属于 TouchCartViewFactory");

        // 这些样式类只应出现在工厂里；一旦回到控制器，就是布局代码又开始堆积
        for (String styleClass : new String[] {"tpos-product-card-name", "tpos-cart-row-subtotal", "cash-denom-btn"}) {
            assertTrue(factory.contains(styleClass), "视图工厂应包含 " + styleClass);
            assertTrue(!touch.contains(styleClass), "控制器不得再内联视图构建：" + styleClass);
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
