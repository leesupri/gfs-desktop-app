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

    @FXML private Button recipeButton;

    @FXML
    private Button consumptionDetailButton;

    @FXML 
    private Button warehouseConsumptionButton;

    @FXML
    private Button activityLogButton;

    @FXML
    private Button staffButton;

    @FXML
    private Button securityButton;

    // Track the currently active button for highlighting
    private Button currentActiveButton;

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
        setButtonAccess(recipeButton, "recipe.view");
        setButtonAccess(consumptionDetailButton, "consumption_detail.view");
        setButtonAccess(warehouseConsumptionButton, "warehouse_consumption.view");
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
        }else if (SessionManager.hasPermission("warehouse_consumption.view")) {
            showWarehouseConsumption();
        } else if (SessionManager.hasPermission("market_list.view")) {
            showMarketList();
        }else if (SessionManager.hasPermission("recipe.view")) { showRecipeReport(); 

        }else if (SessionManager.hasPermission("activity_log.view")) {showActivityLog();

        } else {
            pageTitleLabel.setText("No Access");
        }
    }

    // ------------------------------------------------------------------------
    // Navigation methods with active button highlighting
    // ------------------------------------------------------------------------
    @FXML
    private void showHome() {
        loadPage("/fxml/home.fxml", "Home");
        setActiveButton(homeButton);
    }

    @FXML
    private void showSales() {
        loadPage("/fxml/sales.fxml", "Sales");
        setActiveButton(salesButton);
    }

    @FXML private void showRecipeReport() {
        loadPage("/fxml/recipe-report.fxml", "Recipe Report");
        setActiveButton(recipeButton);
    }

    @FXML
    private void showConsumptionDetail() {
        loadPage("/fxml/consumption-detail.fxml", "Consumption Detail Invoice");
        setActiveButton(consumptionDetailButton);
    }

    @FXML
    private void showWarehouseConsumption() {
        loadPage("/fxml/warehouse-consumption.fxml", "Warehouse Consumption Report");
        setActiveButton(warehouseConsumptionButton);
    }

    @FXML
    private void showMarketList() {
        loadPage("/fxml/market-list.fxml", "Market List");
        setActiveButton(marketListButton);
    }

    @FXML
    private void showActivityLog() {
        // Replace with actual activity log FXML when ready
        loadPage("/fxml/home.fxml", "Activity Log");
        setActiveButton(activityLogButton);
    }

    @FXML
    private void showStaffSettings() {
        // Replace with actual staff settings FXML when ready
        loadPage("/fxml/staff-settings.fxml", "Staff Settings");
        setActiveButton(staffButton);
    }

    @FXML
    private void showSecuritySettings() {
        // Replace with actual security settings FXML when ready
        loadPage("/fxml/security-settings.fxml", "Security Settings");
        setActiveButton(securityButton);
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        MainApp.showLogin();
    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------
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

    /**
     * Highlights the active navigation button and removes highlight from the previous one.
     *
     * @param button The button that was clicked (or the initial active page button)
     */
    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("nav-button-active");
        }
        currentActiveButton = button;
        if (button != null) {
            button.getStyleClass().add("nav-button-active");
        }
    }
}