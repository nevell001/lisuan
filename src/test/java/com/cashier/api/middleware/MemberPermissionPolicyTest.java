package com.cashier.api.middleware;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 会员等级/折扣的修改权限（TD-017）。
 *
 * <p>等级与折扣直接决定收款金额，属可被滥用的权限。两侧必须一致：</p>
 * <ul>
 *   <li><b>REST API</b>：{@code PUT /api/members/{id}} 由 {@link AuthorizationMiddleware} 限 finance/admin
 *       （本测试直接调它的判定函数，是行为级断言）；建档 {@code POST /api/members} 仍对收银员开放。</li>
 *   <li><b>桌面端</b>：{@code MemberEditController} 对非 finance/admin 禁用等级/折扣**以及积分**
 *       （等级与折扣在保存时由积分推导，只锁两个字段挡不住改折扣），并在保存时把三者还原；
 *       角色判定在 {@code MemberController.showEditDialog}。</li>
 * </ul>
 *
 * <p>另外：等级/折扣变更必须留痕——2026-09 核实发现会员路径此前**完全没有**操作日志，
 * 两条路径都已补 {@code MEMBER_LEVEL_DISCOUNT_UPDATED}。</p>
 */
@DisplayName("会员等级/折扣权限门禁")
class MemberPermissionPolicyTest {

    @Test
    @DisplayName("API：改会员限 finance/admin，建档仍对收银员开放")
    void apiRestrictsMemberLevelAndDiscountToFinanceAndAdmin() {
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "PUT", "/api/members/10"),
            "收银员不得通过 API 修改会员（等级/折扣会改变收款金额）");
        assertTrue(AuthorizationMiddleware.isAllowed("finance", "PUT", "/api/members/10"),
            "财务应可修改会员等级/折扣");
        assertTrue(AuthorizationMiddleware.isAllowed("admin", "PUT", "/api/members/10"),
            "管理员应可修改会员等级/折扣");
        assertTrue(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/members"),
            "建档是收银台日常操作，POST /api/members 不应被收紧（本次只收紧金额类字段）");
        assertTrue(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/members/10/recharge"),
            "充值也是收银台日常操作（且写 RechargeRecord 流水留痕）：2026-09 按 TD-018 决定"
                + "与桌面端口径统一为开放给收银员");
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "PUT", "/api/members/10"),
            "但金额类字段（等级/折扣/积分/余额）仍必须限 finance/admin");
    }

    @Test
    @DisplayName("桌面端：非 finance/admin 锁定等级、折扣与积分，且保存时还原")
    void desktopLocksLevelDiscountAndPointsForNonFinanceRoles() throws Exception {
        String editor = Files.readString(
            Path.of("src/main/java/com/cashier/controller/MemberEditController.java"));
        String list = Files.readString(
            Path.of("src/main/java/com/cashier/controller/MemberController.java"));

        assertTrue(editor.contains("private TextField pointsField;"),
            "锁定对象里有积分字段（等级/折扣由它推导），若字段改名请同步本门禁");
        assertTrue(editor.contains("public void setSensitiveFieldsEditable(boolean editable)"),
            "MemberEditController 必须提供按角色锁定金额类字段的入口");
        for (String control : new String[] {"levelComboBox", "discountField", "pointsField", "balanceField"}) {
            assertTrue(editor.contains(control + ".setDisable(!editable)"),
                control + " 必须随角色禁用：等级与折扣在保存时由**积分**推导（只锁两个字段挡不住改折扣），"
                    + "余额是储值金额（等同发钱，API 的 recharge 本就限 finance/admin）");
        }
        // 保存时还原：禁用控件不阻止程序化赋值，必须把值也还原
        int restore = editor.indexOf("member.balance = lockedBalance;");
        int recompute = editor.indexOf("MemberService.updateMemberLevel(member);");
        assertTrue(restore > 0 && restore > recompute,
            "锁定角色保存时必须把等级/折扣/积分/余额还原为打开对话框时的值，且要在"
                + "自动按积分推导等级之后（否则推导结果会覆盖还原值）");

        assertTrue(list.contains("setSensitiveFieldsEditable(canEditSensitiveFields)"),
            "MemberController 必须把角色判定结果传下去");
        assertTrue(list.contains("\"admin\".equals(operator.role) || \"finance\".equals(operator.role)"),
            "角色判定必须是 admin/finance（且取不到用户时按不可改处理，fail-closed）");
    }

    @Test
    @DisplayName("等级/折扣变更必须写审计日志（API 与桌面两条路径）")
    void levelAndDiscountChangesAreAudited() throws Exception {
        String api = Files.readString(
            Path.of("src/main/java/com/cashier/api/controller/MemberApiController.java"));
        String desktop = Files.readString(
            Path.of("src/main/java/com/cashier/controller/MemberController.java"));
        for (String[] source : new String[][] {{"MemberApiController", api}, {"MemberController", desktop}}) {
            assertTrue(source[1].contains("MEMBER_LEVEL_DISCOUNT_UPDATED"),
                source[0] + " 在等级/折扣变更时必须写操作日志——2026-09 核实发现会员路径此前没有任何留痕，"
                    + "\"谁把折扣改成 0.5 折\"事后查不到");
        }
    }
}
