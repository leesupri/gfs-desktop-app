package com.gfs.app.ui;

import com.gfs.app.MainApp;
import com.gfs.app.SessionManager;
import com.gfs.app.db.AppDatabaseManager;
import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.ActivityLogAction;
import com.gfs.app.service.ActivityLogService;
import com.gfs.app.service.AuthResult;
import com.gfs.app.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Connection;

public class LoginController {

    @FXML private TextField   usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label       messageLabel;
    @FXML private Button      showPasswordButton;
    @FXML private CheckBox    rememberCheckbox;
    @FXML private VBox        mascotPanel;

    private final AuthService authService = new AuthService();
    private boolean     passwordVisible    = false;
    private TextField   visiblePasswordField;

    @FXML
    public void initialize() {
        messageLabel.setText("");
    }

    @FXML
    private void togglePasswordVisibility() {
        if (passwordVisible) {
            String current = visiblePasswordField.getText();
            passwordField.setText(current);
            HBox parent = (HBox) showPasswordButton.getParent();
            parent.getChildren().set(0, passwordField);
            visiblePasswordField = null;
            showPasswordButton.setText("👁");
            passwordVisible = false;
        } else {
            visiblePasswordField = new TextField(passwordField.getText());
            visiblePasswordField.setPromptText("Enter password");
            visiblePasswordField.getStyleClass().addAll(passwordField.getStyleClass());
            HBox parent = (HBox) showPasswordButton.getParent();
            parent.getChildren().set(0, visiblePasswordField);
            showPasswordButton.setText("🙈");
            passwordVisible = true;
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordVisible ? visiblePasswordField.getText() : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter username and password.", "red");
            return;
        }

        AuthResult result = authService.login(username, password);

        if (result != null) {
            SessionManager.login(result.getUser(), result.getPermissions());

            // Log successful login using the now-populated session
            ActivityLogService.log(
                result.getUser().getId(),
                result.getUser().getUsername(),
                ActivityLogAction.LOGIN,
                "Login successful"
            );

            MainApp.showMainLayout();
        } else {
            // Log failed attempt — user_id unknown so use 0
            ActivityLogService.log(0L, username, ActivityLogAction.LOGIN_FAILED,
                "Failed login attempt for username: " + username);
            showMessage("Invalid username, password, or inactive account.", "red");
        }
    }

    @FXML
    private void handleForgotPassword() {
        showMessage("Contact your system administrator to reset password.", "orange");
    }

    @FXML
    private void handleTestConnection() {
        try (Connection appConn     = AppDatabaseManager.getConnection();
             Connection reportsConn = ReportsDatabaseManager.getConnection()) {
            boolean appOk     = appConn     != null && !appConn.isClosed();
            boolean reportsOk = reportsConn != null && !reportsConn.isClosed();
            if (appOk && reportsOk) {
                showMessage("App DB and Reports DB connection successful.", "green");
            } else {
                showMessage("One or more database connections failed.", "red");
            }
        } catch (Exception e) {
            showMessage("DB Error: " + e.getMessage(), "red");
            e.printStackTrace();
        }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setText(msg);
    }
}