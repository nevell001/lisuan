package com.cashier.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.CartItem;
import com.cashier.model.Product;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 触屏收银台业务逻辑的无界面测试。
 *
 * <p>触屏收银台没有 UI 级自动化覆盖（TestFX 需要真实显示环境，CI 跑不了），
 * 因此把纯逻辑抽成包内可见的静态方法（{@code TouchCartController.mergeHotProducts} /
 * {@code applyCashPayment} / {@code serializeCartItems} / {@code parseHoldCartItems} /
 * {@code currentStock} / {@code findCartItem}）后在此直接验证——测的是生产代码本身，
 * 不是复制品。</p>
 */
@DisplayName("触屏收银台业务逻辑（无界面）测试")
class TouchCartControllerLogicTest extends DatabaseTestBase {

    private static final int HOT_TARGET = 12;

    @Test
    @DisplayName("热销推荐：手动标记优先，不足时按商品ID去重补足到 12 个")
    void mergeHotProductsFillsUpToTarget() {
        List<Product> manual = List.of(product(1, "手动热销A"), product(2, "手动热销B"));
        List<Product> topSelling = new ArrayList<>();
        // 第一名与手动标记重复，且总量足够补满
        for (int id = 2; id <= 30; id++) {
            topSelling.add(product(id, "销量榜商品" + id));
        }

        List<Product> merged = TouchCartController.mergeHotProducts(manual, topSelling, HOT_TARGET);

        assertEquals(HOT_TARGET, merged.size(), "热销推荐应补足到目标数量");
        assertEquals(1, merged.get(0).id);
        assertEquals(2, merged.get(1).id, "手动标记的商品必须排在前面");
        assertEquals(HOT_TARGET, merged.stream().map(p -> p.id).distinct().count(),
            "补足时不得重复加入同一商品: " + merged.stream().map(p -> p.id).toList());
    }

    @Test
    @DisplayName("热销推荐：手动标记超过目标数量时不做截断")
    void mergeHotProductsKeepsManualProductsBeyondTarget() {
        List<Product> manual = List.of(product(1, "A"), product(2, "B"), product(3, "C"));

        List<Product> merged = TouchCartController.mergeHotProducts(manual, List.of(product(4, "D")), 2);

        assertEquals(3, merged.size(), "手动标记的热销商品不应因为超过目标数量而被丢弃");
    }

    @Test
    @DisplayName("热销推荐：没有手动标记时全部来自销量榜")
    void mergeHotProductsFromSellersOnly() {
        List<Product> topSelling = new ArrayList<>();
        for (int id = 1; id <= 20; id++) {
            topSelling.add(product(id, "销量榜商品" + id));
        }

        List<Product> merged = TouchCartController.mergeHotProducts(List.of(), topSelling, HOT_TARGET);

        assertEquals(HOT_TARGET, merged.size());
        assertEquals(1, merged.get(0).id, "销量榜顺序必须保持");
    }

    @Test
    @DisplayName("热销推荐：销量榜为空时只返回手动标记")
    void mergeHotProductsWithoutSellers() {
        List<Product> manual = List.of(product(7, "唯一热销"));

        List<Product> merged = TouchCartController.mergeHotProducts(manual, List.of(), HOT_TARGET);

        assertEquals(1, merged.size());
        assertSame(manual.get(0), merged.get(0));
    }

    @Test
    @DisplayName("现金分次收款：未付清时给出尚需金额，付清后才结算")
    void cashPaymentAccumulatesUntilPaid() {
        BigDecimal price = new BigDecimal("100.00");

        TouchCartController.CashProgress first =
            TouchCartController.applyCashPayment(BigDecimal.ZERO, new BigDecimal("30.00"), price);

        assertFalse(first.settled(), "只收了 30 元，不应结算");
        assertEquals(0, new BigDecimal("30.00").compareTo(first.received()));
        assertEquals(0, new BigDecimal("70.00").compareTo(first.stillNeed()));
        assertEquals(0, BigDecimal.ZERO.compareTo(first.change()), "未付清时没有找零");

        TouchCartController.CashProgress second =
            TouchCartController.applyCashPayment(first.received(), new BigDecimal("70.00"), price);

        assertTrue(second.settled(), "累计 100 元应付清");
        assertEquals(0, new BigDecimal("100.00").compareTo(second.received()));
        assertEquals(0, BigDecimal.ZERO.compareTo(second.change()));
        assertEquals(0, BigDecimal.ZERO.compareTo(second.stillNeed()));
    }

    @Test
    @DisplayName("现金收款：多付找零、刚好付清、差一分钱不结算")
    void cashPaymentChangeAndBoundaries() {
        BigDecimal price = new BigDecimal("100.00");

        TouchCartController.CashProgress overpay =
            TouchCartController.applyCashPayment(BigDecimal.ZERO, new BigDecimal("120.00"), price);
        assertTrue(overpay.settled());
        assertEquals(0, new BigDecimal("20.00").compareTo(overpay.change()), "多付 20 元应找零");

        TouchCartController.CashProgress exact =
            TouchCartController.applyCashPayment(new BigDecimal("50.00"), new BigDecimal("50.00"), price);
        assertTrue(exact.settled());
        assertEquals(0, BigDecimal.ZERO.compareTo(exact.change()));

        TouchCartController.CashProgress almost =
            TouchCartController.applyCashPayment(BigDecimal.ZERO, new BigDecimal("99.99"), price);
        assertFalse(almost.settled(), "差 1 分钱也不能算付清");
        assertEquals(0, new BigDecimal("0.01").compareTo(almost.stillNeed()));
    }

    @Test
    @DisplayName("挂单：购物车序列化后可原样恢复商品与数量")
    void holdOrderRoundTrip() throws Exception {
        ProductDAORefactored dao = DAOFactory.getInstance().getProductDAO();
        Product first = insertedProduct(dao, "TOUCH-HOLD-001", "触屏挂单商品A");
        Product second = insertedProduct(dao, "TOUCH-HOLD-002", "触屏挂单商品B");

        List<CartItem> cart = List.of(new CartItem(first, 2), new CartItem(second, 5));
        String json = TouchCartController.serializeCartItems(cart);

        List<CartItem> restored = TouchCartController.parseHoldCartItems(json);

        assertEquals(2, restored.size(), "恢复的明细行数应与挂单一致: " + json);
        assertEquals(first.id, restored.get(0).product.id);
        assertEquals(2, restored.get(0).quantity);
        assertEquals(second.id, restored.get(1).product.id);
        assertEquals(5, restored.get(1).quantity);
        assertEquals(0, first.price.multiply(BigDecimal.valueOf(2)).compareTo(restored.get(0).subtotal),
            "恢复后的小计应按商品价格与数量重算");
    }

    @Test
    @DisplayName("挂单：商品已被删除的行跳过，其余明细仍能恢复")
    void parseHoldOrderSkipsDeletedProduct() throws Exception {
        ProductDAORefactored dao = DAOFactory.getInstance().getProductDAO();
        Product kept = insertedProduct(dao, "TOUCH-HOLD-003", "触屏挂单商品C");

        String json = "[{\"productId\":" + kept.id + ",\"quantity\":3},{\"productId\":99999999,\"quantity\":1}]";

        List<CartItem> restored = TouchCartController.parseHoldCartItems(json);

        assertEquals(1, restored.size(), "被删除的商品不应阻塞其余明细恢复");
        assertEquals(kept.id, restored.get(0).product.id);
        assertEquals(3, restored.get(0).quantity);
    }

    @Test
    @DisplayName("挂单：损坏或空的明细不抛异常")
    void parseHoldOrderToleratesBrokenInput() throws Exception {
        ProductDAORefactored dao = DAOFactory.getInstance().getProductDAO();
        Product product = insertedProduct(dao, "TOUCH-HOLD-004", "触屏挂单商品D");

        assertTrue(TouchCartController.parseHoldCartItems(null).isEmpty());
        assertTrue(TouchCartController.parseHoldCartItems("").isEmpty());
        assertTrue(TouchCartController.parseHoldCartItems("not-json").isEmpty());
        assertTrue(TouchCartController.parseHoldCartItems("[]").isEmpty());
        assertEquals("[]", TouchCartController.serializeCartItems(List.of()), "空购物车也应能挂单");
        assertTrue(TouchCartController.parseHoldCartItems("[{\"productId\":\"abc\",\"quantity\":2}]").isEmpty(),
            "商品 ID 不可解析时该行应被跳过");

        List<CartItem> defaultQuantity =
            TouchCartController.parseHoldCartItems("[{\"productId\":" + product.id + "}]");
        assertEquals(1, defaultQuantity.size(), "缺少 quantity 字段时按 1 件恢复");
        assertEquals(1, defaultQuantity.get(0).quantity);
    }

    @Test
    @DisplayName("库存校验：优先用库存快照，快照缺失才回退商品自带数量")
    void currentStockPrefersInventorySnapshot() {
        Product snapshot = product(11, "库存商品");
        snapshot.quantity = 5;

        Product fresh = product(11, "库存商品");
        fresh.quantity = 2;
        Map<String, Product> inventory = new HashMap<>();
        inventory.put(fresh.name, fresh);

        assertEquals(2, TouchCartController.currentStock(snapshot, inventory),
            "多终端/切分类后应以最新快照为准");
        assertEquals(5, TouchCartController.currentStock(snapshot, new HashMap<>()),
            "快照里没有该商品时回退用商品自带数量");
    }

    @Test
    @DisplayName("购车行按商品ID定位：改名不影响同一行的识别")
    void findCartItemMatchesByProductId() {
        Product renamed = product(21, "改名后的商品");
        CartItem item = new CartItem(renamed, 4);
        List<CartItem> cart = List.of(item);

        assertSame(item, TouchCartController.findCartItem(cart, 21));
        assertNull(TouchCartController.findCartItem(cart, 22), "不同商品 ID 不应命中同一行");
    }

    private static Product product(int id, String name) {
        return new Product(id, name, BigDecimal.TEN, 20, "测试分类", "BAR" + id, "件",
            "描述", "品牌", "供应商", "规格", 0, BigDecimal.ONE);
    }

    private static Product insertedProduct(ProductDAORefactored dao, String code, String name)
            throws Exception {
        Product product = new Product(0, code, name, 10.0, 50, "测试分类", code + "-BAR", "件",
            "描述", "品牌", "供应商", "规格", 0, 5.0);
        assertTrue(dao.insert(product), "测试商品应能插入");
        return product;
    }
}
