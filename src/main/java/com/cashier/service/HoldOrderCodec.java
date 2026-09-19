package com.cashier.service;

import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.CartItem;
import com.cashier.model.Product;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 挂单明细（购物车快照）的编解码。
 *
 * <p>格式：{@code [{"productId":1,"quantity":2},...]}。标准收银台与触屏收银台共用同一实现，
 * 保证两端挂的订单可以在另一端恢复——此前两个控制器各持一份复制的解析代码，
 * 且对损坏字段的处理不一致（触屏端容错、标准端会抛异常导致整单恢复失败）。</p>
 */
public final class HoldOrderCodec {

    private static final Logger logger = LoggerFactoryUtil.getLogger(HoldOrderCodec.class);

    private HoldOrderCodec() {
    }

    /** 序列化购物车为挂单明细 JSON。 */
    public static String serialize(List<CartItem> items) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{\"productId\":").append(item.product.id)
                .append(",\"quantity\":").append(item.quantity).append("}");
        }
        json.append("]");
        return json.toString();
    }

    /**
     * 解析挂单明细并逐条查商品。
     *
     * <p>纯数据操作，不触碰 UI 状态，可安全在后台线程调用；商品已被删除或字段损坏的行跳过，
     * 单行失败不影响其余明细。</p>
     *
     * @param json 挂单明细 JSON，可为 null/空
     * @param productDao 用于按商品 ID 取回商品
     * @return 恢复出的购物车明细（可能为空），不会为 null
     */
    public static List<CartItem> parse(String json, ProductDAORefactored productDao) {
        List<CartItem> items = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return items;
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return items;
        }
        String body = trimmed.substring(1, trimmed.length() - 1);
        if (body.isEmpty()) {
            return items;
        }

        for (String item : body.split("\\},\\{")) {
            String cleaned = item.replace("{", "").replace("}", "");
            int productId = 0;
            int quantity = 1;
            for (String field : cleaned.split(",")) {
                String[] kv = field.split(":");
                if (kv.length != 2) {
                    continue;
                }
                String key = kv[0].replace("\"", "").trim();
                String value = kv[1].trim();
                if ("productId".equals(key)) {
                    productId = parseField(value, 0);
                } else if ("quantity".equals(key)) {
                    quantity = parseField(value, 1);
                }
            }

            // 查找商品；单个商品失败不影响其余明细
            try {
                Product product = productDao.findById(productId);
                if (product != null) {
                    items.add(new CartItem(product, quantity));
                }
            } catch (SQLException e) {
                logger.warn("恢复商品失败 (ID: {}): {}", productId, e.getMessage());
            }
        }
        return items;
    }

    /** 解析整数字段：损坏时保留默认值并继续，避免一个坏字段让整单挂单恢复失败。 */
    private static int parseField(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("挂单明细字段不是整数: {}（按 {} 处理）", value, defaultValue);
            return defaultValue;
        }
    }
}
