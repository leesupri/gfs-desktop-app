package com.gfs.app.ui;

import com.gfs.app.MainApp;
import com.gfs.app.SessionManager;
import com.gfs.app.db.AppDatabaseManager;
import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.service.AuthResult;
import com.gfs.app.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        messageLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Please enter username and password.");
            return;
        }

        AuthResult result = authService.login(username, password);

        if (result != null) {
            SessionManager.login(result.getUser(), result.getPermissions());
            MainApp.showMainLayout();
        } else {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Invalid username, password, or inactive account.");
        }
    }

    @FXML
private void handleTestConnection() {
    try (
        Connection appConn = AppDatabaseManager.getConnection();
        Connection reportsConn = ReportsDatabaseManager.getConnection()
    ) {
        boolean appOk = appConn != null && !appConn.isClosed();
        boolean reportsOk = reportsConn != null && !reportsConn.isClosed();

        if (appOk && reportsOk) {
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("App DB and Reports DB connection successful.");
        } else {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("One or more database connections failed.");
        }
    } catch (Exception e) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText("DB Error: " + e.getMessage());
        e.printStackTrace();
    }
}
}