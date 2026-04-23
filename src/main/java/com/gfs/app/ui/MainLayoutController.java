package com.gfs.app.ui;

import com.gfs.app.MainApp;
import com.gfs.app.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainLayoutController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label currentUserLabel;

    @FXML
    private Button homeButton;

    @FXML
    private Button salesButton;

    @FXML
    private Button marketListButton;

    @FXML
    private Button consumptionDetailButton;

    @FXML
    private Button activityLogButton;

    @FXML
    private Button staffButton;

    @FXML
    private Button securityButton;

    @FXML
    public void initialize() {
        String displayName = SessionManager.getCurrentDisplayName();
        String title = SessionManager.getCurrentTitle();

        if (displayName != null && title != null && !title.isBlank()) {
            currentUserLabel.setText(displayName + " (" + title + ")");
        } else {
            currentUserLabel.setText(displayName != null ? displayName : "-");
        }

        applyPermissions();
        loadFirstAllowedPage();
    }

    private void applyPermissions() {
        setButtonAccess(homeButton, "home.view");
        setButtonAccess(salesButton, "sales.view");
        setButtonAccess(marketListButton, "market_list.view");
        setButtonAccess(consumptionDetailButton, "consumption_detail.view");
        setButtonAccess(activityLogButton, "activity_log.view");
        setButtonAccess(staffButton, "staff.manage");
        setButtonAccess(securityButton, "security.manage");
    }

    private void setButtonAccess(Button button, String permissionCode) {
        boolean allowed = SessionManager.hasPermission(permissionCode);
        button.setVisible(allowed);
        button.setManaged(allowed);
    }

    private void loadFirstAllowedPage() {
        if (SessionManager.hasPermission("home.view")) {
            showHome();
        } else if (SessionManager.hasPermission("sales.view")) {
            showSales();
        } else if (SessionManager.hasPermission("consumption_detail.view")) {
            showConsumptionDetail();
        } else if (SessionManager.hasPermission("market_list.view")) {
            showMarketList();
        } else if (SessionManager.hasPermission("activity_log.view")) {
            showActivityLog();
        } else {
            pageTitleLabel.setText("No Access");
        }
    }

    @FXML
    private void showHome() {
        loadPage("/fxml/home.fxml", "Home");
    }

    @FXML
    private void showSales() {
        loadPage("/fxml/sales.fxml", "Sales");
    }

    @FXML
    private void showConsumptionDetail() {
        loadPage("/fxml/consumption-detail.fxml", "Consumption Detail Invoice");
    }

    @FXML
    private void showMarketList() {
        loadPage("/fxml/market-list.fxml", "Market List");
    }

    @FXML
    private void showActivityLog() {
        loadPage("/fxml/home.fxml", "Activity Log");
    }

    @FXML
    private void showStaffSettings() {
        loadPage("/fxml/home.fxml", "Staff Settings");
    }

    @FXML
    private void showSecuritySettings() {
        loadPage("/fxml/home.fxml", "Security Settings");
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        MainApp.showLogin();
    }

    private void loadPage(String fxmlPath, String pageTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            rootPane.setCenter(content);
            pageTitleLabel.setText(pageTitle);
        } catch (IOException e) {
            e.printStackTrace();
            pageTitleLabel.setText("Error");
        }
    }
}