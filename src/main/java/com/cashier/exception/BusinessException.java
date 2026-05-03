package com.cashier.exception;

/**
 * 业务异常
 * 用于业务逻辑相关的错误（如库存不足、金额不足等）
 */
public class BusinessException extends CashierException {
    
    /** 业务错误类型 */
    public enum BusinessErrorType {
        INSUFFICIENT_STOCK,        // 库存不足
        INSUFFICIENT_BALANCE,      // 余额不足
        PRODUCT_NOT_FOUND,         // 商品不存在
        MEMBER_NOT_FOUND,          // 会员不存在
        INVALID_PRICE,             // 价格无效
        INVALID_QUANTITY,          // 数量无效
        PROMOTION_INVALID,         // 促销无效
        TRANSACTION_LIMIT,         // 交易限制
        RETURN_NOT_ALLOWED,        // 不允许退货
        APPROVAL_REQUIRED,         // 需要审批
        DUPLICATE_OPERATION,       // 重复操作
        DATA_VALIDATION_FAILED     // 数据验证失败
    }
    
    private final BusinessErrorType businessErrorType;
    private final Object relatedEntity; // 相关实体（可选）
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param businessErrorType 业务错误类型
     */
    public BusinessException(String message, BusinessErrorType businessErrorType) {
        super(message, "BIZ_" + businessErrorType.name(), ErrorLevel.WARNING);
        this.businessErrorType = businessErrorType;
        this.relatedEntity = null;
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param businessErrorType 业务错误类型
     * @param relatedEntity 相关实体
     */
    public BusinessException(String message, BusinessErrorType businessErrorType, Object relatedEntity) {
        super(message, "BIZ_" + businessErrorType.name(), ErrorLevel.WARNING);
        this.businessErrorType = businessErrorType;
        this.relatedEntity = relatedEntity;
    }
    
    /**
     * 快速创建：库存不足
     * @param productName 商品名称
     * @param required 需要数量
     * @param available 可用数量
     */
    public static BusinessException insufficientStock(String productName, int required, int available) {
        return new BusinessException(
            String.format("商品「%s」库存不足，需要 %d，可用 %d", productName, required, available),
            BusinessErrorType.INSUFFICIENT_STOCK
        );
    }
    
    /**
     * 快速创建：余额不足
     * @param memberName 会员名称
     * @param required 需要金额
     * @param available 可用金额
     */
    public static BusinessException insufficientBalance(String memberName, double required, double available) {
        return new BusinessException(
            String.format("会员「%s」余额不足，需要 %.2f，可用 %.2f", memberName, required, available),
            BusinessErrorType.INSUFFICIENT_BALANCE
        );
    }
    
    /**
     * 快速创建：商品不存在
     * @param productId 商品ID
     */
    public static BusinessException productNotFound(String productId) {
        return new BusinessException(
            "商品不存在: " + productId,
            BusinessErrorType.PRODUCT_NOT_FOUND
        );
    }
    
    /**
     * 快速创建：会员不存在
     * @param memberId 会员ID
     */
    public static BusinessException memberNotFound(String memberId) {
        return new BusinessException(
            "会员不存在: " + memberId,
            BusinessErrorType.MEMBER_NOT_FOUND
        );
    }
    
    /**
     * 快速创建：价格无效
     * @param price 无效价格
     */
    public static BusinessException invalidPrice(double price) {
        return new BusinessException(
            "价格无效: " + price,
            BusinessErrorType.INVALID_PRICE
        );
    }
    
    /**
     * 快速创建：数量无效
     * @param quantity 无效数量
     */
    public static BusinessException invalidQuantity(int quantity) {
        return new BusinessException(
            "数量无效: " + quantity,
            BusinessErrorType.INVALID_QUANTITY
        );
    }
    
    /**
     * 快速创建：不允许退货
     * @param reason 原因
     */
    public static BusinessException returnNotAllowed(String reason) {
        return new BusinessException(
            "不允许退货: " + reason,
            BusinessErrorType.RETURN_NOT_ALLOWED
        );
    }
    
    /**
     * 快速创建：需要审批
     * @param approvalType 审批类型
     */
    public static BusinessException approvalRequired(String approvalType) {
        return new BusinessException(
            "需要审批: " + approvalType,
            BusinessErrorType.APPROVAL_REQUIRED
        );
    }
    
    /**
     * 快速创建：数据验证失败
     * @param field 字段名
     * @param reason 原因
     */
    public static BusinessException validationFailed(String field, String reason) {
        return new BusinessException(
            String.format("数据验证失败: %s - %s", field, reason),
            BusinessErrorType.DATA_VALIDATION_FAILED
        );
    }
    
    public BusinessErrorType getBusinessErrorType() {
        return businessErrorType;
    }
    
    public Object getRelatedEntity() {
        return relatedEntity;
    }
}