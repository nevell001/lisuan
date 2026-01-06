package com.cashier.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML 工具类
 * 提供加载 FXML 文件的便捷方法
 */
public class FXMLUtils {

    /**
     * 加载 FXML 文件并返回根节点
     * @param fxmlPath FXML 文件路径（相对于 resources 目录）
     * @return 根节点
     * @throws IOException 如果加载失败
     */
    public static Parent loadFXML(String fxmlPath) throws IOException {
        return loadFXML(fxmlPath, null, null);
    }

    /**
     * 加载 FXML 文件并返回根节点
     * @param fxmlPath FXML 文件路径（相对于 resources 目录）
     * @param controller 控制器实例
     * @return 根节点
     * @throws IOException 如果加载失败
     */
    public static Parent loadFXML(String fxmlPath, Object controller) throws IOException {
        return loadFXML(fxmlPath, controller, null);
    }

    /**
     * 加载 FXML 文件并返回根节点
     * @param fxmlPath FXML 文件路径（相对于 resources 目录）
     * @param controller 控制器实例
     * @param resources 资源包
     * @return 根节点
     * @throws IOException 如果加载失败
     */
    public static Parent loadFXML(String fxmlPath, Object controller, ResourceBundle resources) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource(fxmlPath));

        if (controller != null) {
            loader.setController(controller);
        }

        if (resources != null) {
            loader.setResources(resources);
        }

        return loader.load();
    }

    /**
     * 加载 FXML 文件并返回 FXMLLoader 对象
     * @param fxmlPath FXML 文件路径（相对于 resources 目录）
     * @return FXMLLoader 对象
     * @throws IOException 如果加载失败
     */
    public static FXMLLoader loadFXMLLoader(String fxmlPath) throws IOException {
        return loadFXMLLoader(fxmlPath, null, null);
    }

    /**
     * 加载 FXML 文件并返回 FXMLLoader 对象
     * @param fxmlPath FXML 文件路径（相对于 resources 目录）
     * @param controller 控制器实例
     * @return FXMLLoader 对象
     * @throws IOException 如果加载失败
     */
    public static FXMLLoader loadFXMLLoader(String fxmlPath, Object controller) throws IOException {
        return loadFXMLLoader(fxmlPath, controller, null);
    }

    /**
     * 加载 FXML 文件并返回 FXMLLoader 对象
     * @param fxmlPath FXML 文件路径（相对于 resources 目录）
     * @param controller 控制器实例
     * @param resources 资源包
     * @return FXMLLoader 对象
     * @throws IOException 如果加载失败
     */
    public static FXMLLoader loadFXMLLoader(String fxmlPath, Object controller, ResourceBundle resources) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        URL location = getClass().getResource(fxmlPath);

        if (location == null) {
            throw new IOException("无法找到 FXML 文件: " + fxmlPath);
        }

        loader.setLocation(location);

        if (controller != null) {
            loader.setController(controller);
        }

        if (resources != null) {
            loader.setResources(resources);
        }

        return loader;
    }

    /**
     * 创建模态对话框
     * @param fxmlPath FXML 文件路径
     * @param title 对话框标题
     * @param owner 所有者窗口
     * @return 对话框 Stage
     * @throws IOException 如果加载失败
     */
    public static Stage createModalDialog(String fxmlPath, String title, Stage owner) throws IOException {
        FXMLLoader loader = loadFXMLLoader(fxmlPath);
        Parent root = loader.load();

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setScene(new Scene(root));

        return dialog;
    }

    /**
     * 创建模态对话框（带控制器）
     * @param fxmlPath FXML 文件路径
     * @param title 对话框标题
     * @param owner 所有者窗口
     * @param controller 控制器实例
     * @return 对话框 Stage
     * @throws IOException 如果加载失败
     */
    public static Stage createModalDialog(String fxmlPath, String title, Stage owner, Object controller) throws IOException {
        FXMLLoader loader = loadFXMLLoader(fxmlPath, controller);
        Parent root = loader.load();

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setScene(new Scene(root));

        return dialog;
    }

    /**
     * 创建非模态对话框
     * @param fxmlPath FXML 文件路径
     * @param title 对话框标题
     * @param owner 所有者窗口
     * @return 对话框 Stage
     * @throws IOException 如果加载失败
     */
    public static Stage createNonModalDialog(String fxmlPath, String title, Stage owner) throws IOException {
        FXMLLoader loader = loadFXMLLoader(fxmlPath);
        Parent root = loader.load();

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.NONE);
        dialog.setTitle(title);
        dialog.setScene(new Scene(root));

        return dialog;
    }

    /**
     * 创建无边框对话框
     * @param fxmlPath FXML 文件路径
     * @param owner 所有者窗口
     * @return 对话框 Stage
     * @throws IOException 如果加载失败
     */
    public static Stage createUndecoratedDialog(String fxmlPath, Stage owner) throws IOException {
        FXMLLoader loader = loadFXMLLoader(fxmlPath);
        Parent root = loader.load();

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setScene(new Scene(root));

        return dialog;
    }

    /**
     * 检查 FXML 文件是否存在
     * @param fxmlPath FXML 文件路径
     * @return 如果存在返回 true，否则返回 false
     */
    public static boolean existsFXML(String fxmlPath) {
        return getClass().getResource(fxmlPath) != null;
    }
}