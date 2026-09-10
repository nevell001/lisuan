package com.cashier;

/**
 * 可执行 JAR 的启动入口。
 *
 * <p>JavaFX 的 JDK 启动器会拒绝直接运行继承 {@code javafx.application.Application} 的主类
 * （报 “缺少 JavaFX 运行时组件”），除非 JavaFX 位于 module-path 上。改由一个不继承
 * {@code Application} 的类作为 Main-Class 即可绕开该校验，使
 * {@code java -jar lisuan-fx-*-jar-with-dependencies.jar} 直接可用。</p>
 *
 * <p>DPI 等系统属性仍在 {@link CashierSystemFXApplication#main(String[])} 中设置，
 * 因此这里直接委托给它，而不是自行调用 {@code Application.launch}。</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        CashierSystemFXApplication.main(args);
    }
}
