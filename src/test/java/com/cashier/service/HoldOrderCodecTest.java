package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.CartItem;
import com.cashier.model.Product;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 挂单明细编解码测试。
 *
 * <p>标准收银台与触屏收银台共用这一份实现（{@link HoldOrderCodec}），
 * 这里锁住两件容易被改坏的事：序列化↔恢复必须往返一致；明细损坏时只跳过坏行，
 * 不能让整单挂单恢复失败。</p>
 */
@DisplayName("挂单明细编解码测试")
class HoldOrderCodecTest extends DatabaseTestBase {

    private static final ProductDAORefactored PRODUCT_DAO = DAOFactory.getInstance().getProductDAO();

    @Test
    @DisplayName("购物车序列化后可原样恢复商品与数量")
    void roundTripRestoresItems() throws Exception {
        Product first = insertedProduct("HOLD-CODE-001", "挂单编解码商品A");
        Product second = insertedProduct("HOLD-CODE-002", "挂单编解码商品B");

        List<CartItem> cart = List.of(new CartItem(first, 2), new CartItem(second, 5));
        String json = HoldOrderCodec.serialize(cart);

        List<CartItem> restored = HoldOrderCodec.parse(json, PRODUCT_DAO);

        assertEquals(2, restored.size(), "恢复的明细行数应与挂单一致: " + json);
        assertEquals(first.id, restored.get(0).product.id);
        assertEquals(2, restored.get(0).quantity);
        assertEquals(second.id, restored.get(1).product.id);
        assertEquals(5, restored.get(1).quantity);
        assertEquals(0, first.price.multiply(BigDecimal.valueOf(2)).compareTo(restored.get(0).subtotal),
            "恢复后的小计应按商品价格与数量重算");
    }

    @Test
    @DisplayName("商品已被删除的行跳过，其余明细仍能恢复")
    void parseSkipsDeletedProduct() throws Exception {
        Product kept = insertedProduct("HOLD-CODE-003", "挂单编解码商品C");

        String json = "[{\"productId\":" + kept.id + ",\"quantity\":3},{\"productId\":99999999,\"quantity\":1}]";

        List<CartItem> restored = HoldOrderCodec.parse(json, PRODUCT_DAO);

        assertEquals(1, restored.size(), "被删除的商品不应阻塞其余明细恢复");
        assertEquals(kept.id, restored.get(0).product.id);
        assertEquals(3, restored.get(0).quantity);
    }

    @Test
    @DisplayName("整数字段损坏只跳过坏行，不让整单恢复失败")
    void parseToleratesBrokenFieldAmongValidRows() throws Exception {
        Product product = insertedProduct("HOLD-CODE-004", "挂单编解码商品D");

        // 旧实现里标准收银台会在 "abc" 上抛 IllegalArgumentException，整单恢复直接失败
        String json = "[{\"productId\":\"abc\",\"quantity\":2},{\"productId\":" + product.id + ",\"quantity\":3}]";

        List<CartItem> restored = HoldOrderCodec.parse(json, PRODUCT_DAO);

        assertEquals(1, restored.size(), "坏行应被跳过，好行必须恢复: " + json);
        assertEquals(product.id, restored.get(0).product.id);
        assertEquals(3, restored.get(0).quantity);
    }

    @Test
    @DisplayName("空或损坏的明细不抛异常")
    void parseToleratesEmptyOrBrokenInput() throws Exception {
        Product product = insertedProduct("HOLD-CODE-005", "挂单编解码商品E");

        assertTrue(HoldOrderCodec.parse(null, PRODUCT_DAO).isEmpty());
        assertTrue(HoldOrderCodec.parse("", PRODUCT_DAO).isEmpty());
        assertTrue(HoldOrderCodec.parse("not-json", PRODUCT_DAO).isEmpty());
        assertTrue(HoldOrderCodec.parse("[]", PRODUCT_DAO).isEmpty());
        assertTrue(HoldOrderCodec.parse("[{\"productId\":\"abc\",\"quantity\":2}]", PRODUCT_DAO).isEmpty(),
            "商品 ID 不可解析时该行应被跳过");
        assertEquals("[]", HoldOrderCodec.serialize(List.of()), "空购物车也应能挂单");

        List<CartItem> defaultQuantity =
            HoldOrderCodec.parse("[{\"productId\":" + product.id + "}]", PRODUCT_DAO);
        assertEquals(1, defaultQuantity.size(), "缺少 quantity 字段时按 1 件恢复");
        assertEquals(1, defaultQuantity.get(0).quantity);
    }

    private static Product insertedProduct(String code, String name) throws Exception {
        Product product = new Product(0, code, name, 10.0, 50, "测试分类", code + "-BAR", "件",
            "描述", "品牌", "供应商", "规格", 0, 5.0);
        assertTrue(PRODUCT_DAO.insert(product), "测试商品应能插入");
        return product;
    }
}
