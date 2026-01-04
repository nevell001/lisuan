import java.util.Scanner;

public class InteractiveHello {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("请输入您的名字: ");
        String name = scanner.nextLine();

        System.out.print("请输入您的年龄: ");
        int age = scanner.nextInt();

        System.out.println("\n=================================");
        System.out.println("Hello, " + name + "!");
        System.out.println("您今年 " + age + " 岁");
        System.out.println("=================================");

        scanner.close();
    }
}