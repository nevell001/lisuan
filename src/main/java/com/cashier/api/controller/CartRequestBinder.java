package com.cashier.api.controller;

import com.cashier.api.ApiMessages;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.CartItem;
import com.cashier.model.Product;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把 REST 请求里的商品行装成购物车，并统一做商品/数量/库存校验。
 */
final class CartRequestBinder {

    private static final int MAX_ITEMS = 50;

    static final Keys HOLD = new Keys(
        "api.hold.items_required", "api.hold.too_many_items", "api.hold.item_invalid",
        "api.hold.duplicate_item", "api.hold.product_not_found", "api.hold.stock_insufficient");

    private CartRequestBinder() {
    }

    static final class Keys {
        final String itemsRequired;
        final String tooManyItems;
        final String itemInvalid;
        final String duplicateItem;
        final String productNotFound;
        final String stockInsufficient;

        private Keys(String itemsRequired, String tooManyItems, String itemInvalid,
                     String duplicateItem, String productNotFound, String stockInsufficient) {
            this.itemsRequired = itemsRequired;
            this.tooManyItems = tooManyItems;
            this.itemInvalid = itemInvalid;
            this.duplicateItem = duplicateItem;
            this.productNotFound = productNotFound;
            this.stockInsufficient = stockInsufficient;
        }
    }

    /**
     * @param checkStock 是否按当前库存拦截
     * @return 购物车；校验失败时已写入 400 并返回 {@code null}
     */
    static List<CartItem> bind(Context ctx, List<HoldOrderApiController.HoldItemRequest> items,
                               boolean checkStock, Keys keys) throws SQLException {
        if (items == null || items.isEmpty()) {
            error(ctx, keys.itemsRequired);
            return null;
        }
        if (items.size() > MAX_ITEMS) {
            error(ctx, keys.tooManyItems, MAX_ITEMS);
            return null;
        }

        List<CartItem> cart = new ArrayList<>();
        Set<Integer> seenProductIds = new HashSet<>();
        ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
        for (HoldOrderApiController.HoldItemRequest item : items) {
            if (item == null || item.productId == null || item.productId <= 0
                    || item.quantity == null || item.quantity <= 0 || item.quantity > 9999) {
                error(ctx, keys.itemInvalid);
                return null;
            }
            if (!seenProductIds.add(item.productId)) {
                error(ctx, keys.duplicateItem, item.productId);
                return null;
            }
            Product product = productDAO.findById(item.productId);
            if (product == null) {
                error(ctx, keys.productNotFound, item.productId);
                return null;
            }
            if (checkStock && item.quantity > product.quantity) {
                error(ctx, keys.stockInsufficient, product.name, product.quantity);
                return null;
            }
            cart.add(new CartItem(product, item.quantity));
        }
        return cart;
    }

    private static void error(Context ctx, String key, Object... params) {
        ctx.status(HttpStatus.BAD_REQUEST)
            .json(Map.of("success", false, "message", ApiMessages.text(ctx, key, params)));
    }
}
