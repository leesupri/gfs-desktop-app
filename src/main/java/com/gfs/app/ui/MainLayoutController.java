package com.gfs.app.ui;

import com.gfs.app.MainApp;
import com.gfs.app.SessionManager;
import com.gfs.app.model.ActivityLogAction;
import com.gfs.app.service.ActivityLogService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainLayoutController {

    @FXML private BorderPane rootPane;
    @FXML private Label      pageTitleLabel;
    @FXML private Label      currentUserLabel;
    @FXML private Label      userInitialsLabel;

    // Main
    @FXML private Button homeButton;
    @FXML private Button salesButton;

    // Reports
    @FXML private Button itemSalesReportButton;
    @FXML private Button receiptSummaryButton;
    @FXML private Button orderBoardButton;
    @FXML private Button marketListButton;
    @FXML private Button recipeButton;
    @FXML private Button productionButton;
    @FXML private Button consumptionDetailButton;
    @FXML private Button purchaseReportButton;
    @FXML private Button warehouseConsumptionButton;

    // Admin
    @FXML private Button activityLogButton;
    @FXML private Button staffButton;
    @FXML private Button securityButton;

    private Button currentActiveButton;

    @FXML
    public void initialize() {
        String displayName = SessionManager.getCurrentDisplayName();
        String title       = SessionManager.getCurrentTitle();

        if (displayName != null && title != null && !title.isBlank()) {
            currentUserLabel.setText(displayName + " (" + title + ")");
        } else {
            currentUserLabel.setText(displayName != null ? displayName : "-");
        }

        if (displayName != null && !displayName.isBlank()) {
            String[] parts = displayName.trim().split("\\s+");
            String initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)
                : String.valueOf(parts[0].charAt(0));
            userInitialsLabel.setText(initials.toUpperCase());
        } else {
            userInitialsLabel.setText("?");
        }

        applyPermissions();
        loadFirstAllowedPage();
    }

    private void applyPermissions() {
        setButtonAccess(homeButton,                 "home.view");
        setButtonAccess(salesButton,                "sales.view");
        setButtonAccess(itemSalesReportButton,      "item_sales_report.view");
        setButtonAccess(receiptSummaryButton,       "receipt_summary.view");
        setButtonAccess(orderBoardButton,           "order_board.view");
        setButtonAccess(marketListButton,           "market_list.view");
        setButtonAccess(recipeButton,               "recipe.view");
        setButtonAccess(productionButton,           "production.view");
        setButtonAccess(consumptionDetailButton,    "consumption_detail.view");
        setButtonAccess(purchaseReportButton,       "purchase.view");
        setButtonAccess(warehouseConsumptionButton, "warehouse_consumption.view");
        setButtonAccess(activityLogButton,          "activity_log.view");
        setButtonAccess(staffButton,                "staff.manage");
        setButtonAccess(securityButton,             "security.manage");
    }

    private void setButtonAccess(Button button, String permissionCode) {
        boolean allowed = SessionManager.hasPermission(permissionCode);
        button.setVisible(allowed);
        button.setManaged(allowed);
    }

    private void loadFirstAllowedPage() {
        if      (SessionManager.hasPermission("home.view"))                  showHome();
        else if (SessionManager.hasPermission("sales.view"))                 showSales();
        else if (SessionManager.hasPermission("item_sales_report.view"))     showItemSalesReport();
        else if (SessionManager.hasPermission("receipt_summary.view"))       showReceiptSummary();
        else if (SessionManager.hasPermission("order_board.view"))           showOrderBoard();
        else if (SessionManager.hasPermission("consumption_detail.view"))    showConsumptionDetail();
        else if (SessionManager.hasPermission("warehouse_consumption.view")) showWarehouseConsumption();
        else if (SessionManager.hasPermission("market_list.view"))           showMarketList();
        else if (SessionManager.hasPermission("recipe.view"))                showRecipeReport();
        else if (SessionManager.hasPermission("production.view"))            showProduction();
        else if (SessionManager.hasPermission("purchase.view"))            showPurchaseReport();
        
        else if (SessionManager.hasPermission("activity_log.view"))          showActivityLog();
        else    pageTitleLabel.setText("No Access");
    }

    // Navigation — Main
    @FXML private void showHome()  { loadPage("/fxml/home.fxml",  "Home");  setActiveButton(homeButton); }
    @FXML private void showSales() { loadPage("/fxml/sales.fxml", "Sales"); setActiveButton(salesButton); }

    // Navigation — Reports
    @FXML private void showItemSalesReport()     { loadPage("/fxml/item-sales-report.fxml",    "Item Sales Report");           setActiveButton(itemSalesReportButton); }
    @FXML private void showReceiptSummary()      { loadPage("/fxml/receipt-summary.fxml",      "Receipt Summary");             setActiveButton(receiptSummaryButton); }
    @FXML private void showOrderBoard()          { loadPage("/fxml/order-board.fxml",          "Order Board");                 setActiveButton(orderBoardButton); }
    @FXML private void showMarketList()          { loadPage("/fxml/market-list.fxml",          "Market List");                 setActiveButton(marketListButton); }
    @FXML private void showRecipeReport()        { loadPage("/fxml/recipe-report.fxml",        "Recipe Report");               setActiveButton(recipeButton); }
    @FXML private void showProduction()          { loadPage("/fxml/production-summary.fxml",   "Production Summary");          setActiveButton(productionButton); }
    @FXML private void showConsumptionDetail()   { loadPage("/fxml/consumption-detail.fxml",   "Consumption Detail Invoice");  setActiveButton(consumptionDetailButton); }
    @FXML private void showPurchaseReport() {loadPage("/fxml/purchase-report.fxml", "Purchase Reports");setActiveButton(purchaseReportButton);}
    @FXML private void showWarehouseConsumption(){ loadPage("/fxml/warehouse-consumption.fxml","Warehouse Consumption");       setActiveButton(warehouseConsumptionButton); }

    // Navigation — Admin
    @FXML private void showActivityLog()     { loadPage("/fxml/activity-log.fxml",      "Activity Log");      setActiveButton(activityLogButton); }
    @FXML private void showStaffSettings()   { loadPage("/fxml/staff-settings.fxml",    "Staff Settings");    setActiveButton(staffButton); }
    @FXML private void showSecuritySettings(){ loadPage("/fxml/security-settings.fxml", "Security Settings"); setActiveButton(securityButton); }

    @FXML
    private void handleLogout() {
        ActivityLogService.log(ActivityLogAction.LOGOUT, "User logged out");
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
            pageTitleLabel.setText("Error loading page");
        }
    }

    private void setActiveButton(Button button) {
        if (currentActiveButton != null)
            currentActiveButton.getStyleClass().remove("nav-button-active");
        currentActiveButton = button;
        if (button != null && !button.getStyleClass().contains("nav-button-active"))
            button.getStyleClass().add("nav-button-active");
    }
}