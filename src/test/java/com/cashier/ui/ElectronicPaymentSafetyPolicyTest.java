package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import com.cashier.service.PaymentService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectronicPaymentSafetyPolicyTest {

    @Test
    @DisplayName("桌面收银不得把微信支付宝直接记为支付成功")
    void desktopCheckoutDoesNotDirectlySettleElectronicPayment() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));

        assertFalse(source.contains("handlePayment(\"微信\")"));
        assertFalse(source.contains("handlePayment(\"支付宝\")"));
        assertTrue(source.contains("startElectronicPayment(PaymentOrder.PaymentChannel.WECHAT"));
        assertTrue(source.contains("startElectronicPayment(PaymentOrder.PaymentChannel.ALIPAY"));
        assertTrue(source.contains("PaymentService.queryPaymentStatus("));
        assertTrue(source.contains("latest.status == PaymentOrder.PaymentStatus.SUCCESS"));
        assertTrue(source.contains("PaymentService.cancelPaymentOrder("));
    }

    @Test
    @DisplayName("默认支付配置不得包含模拟商户凭据")
    void defaultPaymentConfigIsFailClosed() throws Exception {
        // 断言运行时默认配置（代码级 fail-closed），不依赖被 gitignore 的本地配置文件
        PaymentService.PaymentConfig config = new PaymentService.PaymentConfig();

        assertTrue("disabled".equals(config.mode));
        assertFalse(config.mockEnabled);
        assertFalse(config.wechatEnabled);
        assertFalse(config.alipayEnabled);
    }

    @Test
    @DisplayName("网关 HTTP 必须设置连接与请求超时（默认是无限等待）")
    void gatewayHttpCallsHaveTimeouts() throws Exception {
        for (String provider : new String[]{
            "src/main/java/com/cashier/service/payment/AlipayPrecreatePaymentProvider.java",
            "src/main/java/com/cashier/service/payment/WechatNativePaymentProvider.java"}) {
            String source = Files.readString(Path.of(provider));
            assertTrue(source.contains("HttpClient.newBuilder().connectTimeout("),
                provider + " 的 HttpClient 必须设置 connectTimeout：java.net.http 默认无限等待，"
                    + "网关不可达时会把调用线程永久挂住（TD-031）");
            assertTrue(source.contains(".timeout(REQUEST_TIMEOUT)"),
                provider + " 的请求必须设置 .timeout(...)：只有连接超时挡不住"
                    + "（连上但不回包的网关）");
            assertFalse(source.contains("HttpClient.newHttpClient()"),
                provider + " 不得再使用无超时的 HttpClient.newHttpClient()");
        }
    }

    @Test
    @DisplayName("下单（网关 HTTP）必须在 FX 线程之外发起")
    void paymentOrderCreationRunsOffTheFxThread() throws Exception {
        for (String cart : new String[]{
            "src/main/java/com/cashier/controller/CartController.java",
            "src/main/java/com/cashier/controller/TouchCartController.java"}) {
            String body = methodBody(Files.readString(Path.of(cart)), "private void startElectronicPayment(");
            assertTrue(body.contains("UIOptimizer.runInBackground("),
                cart + " 的 startElectronicPayment 必须在后台线程调用下单（仓库统一用 "
                    + "UIOptimizer.runInBackground）：网关请求放 FX 线程会冻住整个收银界面（TD-031）");
            assertTrue(body.contains("PaymentService.createPaymentOrder("),
                cart + " 仍需调用 PaymentService.createPaymentOrder(...)（只是要挪到后台线程）");
        }
    }

    /** 取方法体（按花括号配对）。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法签名: " + signature);
        int open = source.indexOf('{', start);
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
        throw new AssertionError("方法体不闭合: " + signature);
    }

    @Test
    @DisplayName("系统设置应提供微信支付宝接入配置入口")
    void settingsExposeElectronicPaymentConfiguration() throws Exception {
        String settingsView = Files.readString(Path.of(
            "src/main/resources/com/cashier/view/SettingsView.fxml"
        ));
        String settingsController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/SettingsController.java"
        ));

        assertTrue(settingsView.contains("<Tab text=\"%settings.tab.payment\">"));
        assertTrue(settingsView.contains("fx:id=\"wechatEnabledCheckBox\""));
        assertTrue(settingsView.contains("fx:id=\"wechatAppIdField\""));
        assertTrue(settingsView.contains("fx:id=\"wechatMchIdField\""));
        assertTrue(settingsView.contains("fx:id=\"wechatApiKeyField\""));
        assertTrue(settingsView.contains("fx:id=\"wechatPrivateKeyPathField\""));
        assertTrue(settingsView.contains("fx:id=\"wechatMerchantSerialNoField\""));
        assertTrue(settingsView.contains("fx:id=\"alipayEnabledCheckBox\""));
        assertTrue(settingsView.contains("fx:id=\"alipayAppIdField\""));
        assertTrue(settingsView.contains("fx:id=\"alipayPrivateKeyField\""));
        assertTrue(settingsView.contains("fx:id=\"alipayPublicKeyArea\""));
        assertTrue(settingsView.contains("fx:id=\"alipayGatewayField\""));
        assertTrue(settingsController.contains("PaymentService.saveConfig(paymentConfig)"));
    }

    @Test
    @DisplayName("API 支付配置更新也必须持久化到配置文件")
    void paymentApiConfigUpdatePersistsConfig() throws Exception {
        String apiController = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/PaymentApiController.java"
        ));

        assertTrue(apiController.contains("PaymentService.saveConfig(config)"));
        assertFalse(apiController.contains("PaymentService.setConfig(config);"));
    }

    @Test
    @DisplayName("支付查单和回调应保留真实渠道详情")
    void paymentStatusAndNotifyKeepProviderDetails() throws Exception {
        String paymentService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/PaymentService.java"
        ));
        String apiController = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/PaymentApiController.java"
        ));
        String wechatProvider = Files.readString(Path.of(
            "src/main/java/com/cashier/service/payment/WechatNativePaymentProvider.java"
        ));

        assertTrue(paymentService.contains("getPaymentDAO().updatePaymentSuccess("));
        assertTrue(paymentService.contains("order.channelTransactionId"));
        assertTrue(paymentService.contains("order.channelUserId"));
        assertTrue(apiController.contains("formParams.forEach("));
        assertTrue(wechatProvider.contains("decryptAes256Gcm("));
        assertTrue(wechatProvider.contains("loadPublicKeyFromCertificateOrPem("));
        assertTrue(apiController.contains("\"code\", \"SUCCESS\""));
        assertFalse(apiController.contains("extractXmlValue("));
        assertFalse(apiController.contains("total_fee"));
        assertFalse(apiController.contains("<xml><return_code>"));
    }
}
