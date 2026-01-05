@echo off
javac -cp flatlaf-3.5.jar Category.java Product.java Transaction.java Member.java Promotion.java RechargeRecord.java User.java OperationLog.java Shift.java DataManager.java CashierSystemGUI.java
if %errorlevel% neq 0 (
    echo 编译失败！
) else (
    echo 编译成功！
)
pause