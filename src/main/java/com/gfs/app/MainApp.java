package com.gfs.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;


    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showLogin();
        primaryStage.show();
    }

    public static void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/fxml/login.fxml")
            );

            Scene scene = new Scene(loader.load(), 480, 320);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/css/app.css").toExternalForm()
            );

            primaryStage.setTitle("GFS Desktop App - Login");
            primaryStage.setMinWidth(650);
            primaryStage.setMinHeight(500);
            primaryStage.setWidth(900);
            primaryStage.setHeight(600);
            primaryStage.centerOnScreen();
            primaryStage.setScene(scene);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showMainLayout() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/fxml/main-layout.fxml")
            );

            Scene scene = new Scene(loader.load(), 1280, 800);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/css/app.css").toExternalForm()
            );

            primaryStage.setTitle("GFS Desktop App");
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(720);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}