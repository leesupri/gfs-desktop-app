package com.gfs.app.ui;

import com.gfs.app.model.Role;
import com.gfs.app.model.StaffUser;
import com.gfs.app.repository.RoleRepository;
import com.gfs.app.repository.StaffRepository;
import com.gfs.app.util.PasswordUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StaffSettingsController {

    @FXML private TableView<StaffUser>          staffTable;
    @FXML private TableColumn<StaffUser, String> colName;
    @FXML private TableColumn<StaffUser, String> colUsername;
    @FXML private TableColumn<StaffUser, String> colTitle;
    @FXML private TableColumn<StaffUser, String> colActive;
    @FXML private TableColumn<StaffUser, Void>   colActions;
    @FXML private Label                          staffCountLabel;
    @FXML private ListView<Role>                 rolesListView;
    @FXML private Label                          rolePanelTitle;

    private final StaffRepository staffRepo = new StaffRepository();
    private final RoleRepository  roleRepo  = new RoleRepository();
    private List<Role> allRoles;

    @FXML
    public void initialize() {
        allRoles = roleRepo.findAll();
        setupColumns();
        setupRoleList();
        loadData();
    }

    // -------------------------------------------------------------------------
    // Column setup
    // -------------------------------------------------------------------------
    private void setupColumns() {

        // Name column — avatar circle + full name + title stacked
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colName.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) { setGraphic(null); return; }
                StaffUser user = getTableView().getItems().get(getIndex());

                // Avatar circle with initials
                String initials = buildInitials(user.getName());
                String avatarColor = pickAvatarColor(user.getId());
                Label avatarLabel = new Label(initials);
                avatarLabel.setStyle(
                    "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                StackPane avatar = new StackPane(avatarLabel);
                avatar.setStyle(
                    "-fx-background-color: " + avatarColor + ";" +
                    "-fx-background-radius: 50; -fx-min-width: 32; -fx-min-height: 32;" +
                    "-fx-max-width: 32; -fx-max-height: 32;");

                Label nameLabel = new Label(user.getName());
                nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
                Label titleLabel = new Label(user.getTitle() != null ? user.getTitle() : "");
                titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

                javafx.scene.layout.VBox nameBox = new javafx.scene.layout.VBox(2, nameLabel, titleLabel);
                HBox cell = new HBox(10, avatar, nameBox);
                cell.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(cell);
                setText(null);
            }
        });

        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colUsername.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px;");
            }
        });

        colTitle.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getTitle() != null ? d.getValue().getTitle() : ""));

        // Status badge column
        colActive.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().isActive() ? "Active" : "Inactive"));
        colActive.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); setStyle(""); return; }
                boolean active = "Active".equals(item);
                Label badge = new Label(item);
                badge.setStyle(active
                    ? "-fx-background-color:#f5ede6; -fx-text-fill:#3a2316;" +
                      "-fx-font-size:11px; -fx-font-weight:bold;" +
                      "-fx-background-radius:20; -fx-padding: 3 8 3 8;"
                    : "-fx-background-color:#f3f4f6; -fx-text-fill:#6b7280;" +
                      "-fx-font-size:11px; -fx-font-weight:bold;" +
                      "-fx-background-radius:20; -fx-padding: 3 8 3 8;");
                setGraphic(badge);
                setText(null);
            }
        });

        // Inline action buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                editBtn.setStyle(
                    "-fx-background-color:white; -fx-text-fill:#374151;" +
                    "-fx-border-color:#e5e7eb; -fx-border-radius:6; -fx-background-radius:6;" +
                    "-fx-font-size:12px; -fx-padding: 4 10;");
                deleteBtn.setStyle(
                    "-fx-background-color:#fef2f2; -fx-text-fill:#991b1b;" +
                    "-fx-border-color:#fecaca; -fx-border-radius:6; -fx-background-radius:6;" +
                    "-fx-font-size:12px; -fx-padding: 4 10;");
                editBtn.setOnAction(e -> {
                    StaffUser u = getTableView().getItems().get(getIndex());
                    showStaffDialog(u);
                });
                deleteBtn.setOnAction(e -> {
                    StaffUser u = getTableView().getItems().get(getIndex());
                    handleDeleteUser(u);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupRoleList() {
        staffTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadRolesForUser(selected);
            } else {
                rolesListView.setItems(FXCollections.observableArrayList());
                rolePanelTitle.setText("Roles");
            }
        });

        rolesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        rolesListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setText(null); setGraphic(null); return; }
                setText(role.getName());
                setStyle("-fx-font-size: 13px; -fx-padding: 8 12;");
            }
        });
    }

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------
    private void loadData() {
        List<StaffUser> users = staffRepo.findAll();
        staffTable.setItems(FXCollections.observableArrayList(users));
        int count = users.size();
        staffCountLabel.setText(count + " member" + (count == 1 ? "" : "s"));
    }

    private void loadRolesForUser(StaffUser user) {
        rolePanelTitle.setText("Roles — " + user.getName());
        List<Role> userRoles = staffRepo.getUserRoles(user.getId());
        List<Long> userRoleIds = userRoles.stream().map(Role::getId).collect(Collectors.toList());

        rolesListView.setItems(FXCollections.observableArrayList(allRoles));
        rolesListView.getSelectionModel().clearSelection();
        for (int i = 0; i < allRoles.size(); i++) {
            if (userRoleIds.contains(allRoles.get(i).getId())) {
                rolesListView.getSelectionModel().select(i);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------
    @FXML private void handleNewStaff()  { showStaffDialog(null); }
    @FXML private void handleRefresh()   { loadData(); }

    @FXML
    private void handleSaveRoles() {
        StaffUser selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No selection", "Select a staff member first."); return; }

        List<Long> selectedRoleIds = rolesListView.getSelectionModel().getSelectedItems()
                .stream().map(Role::getId).collect(Collectors.toList());
        staffRepo.updateUserRoles(selected.getId(), selectedRoleIds);
        showInfo("Saved", "Role assignments updated for " + selected.getName() + ".");
    }

    private void handleDeleteUser(StaffUser user) {
        if (!confirm("Delete " + user.getName() + "?\nThis cannot be undone.")) return;
        if (!staffRepo.delete(user.getId())) {
            showAlert("Error", "Could not delete user. They may have related records.");
        }
        loadData();
    }

    // -------------------------------------------------------------------------
    // Staff dialog (create / edit)
    // -------------------------------------------------------------------------
    private void showStaffDialog(StaffUser existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New Staff Member" : "Edit Staff Member");
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField  username = new TextField();
        TextField  name     = new TextField();
        TextField  title    = new TextField();
        PasswordField password = new PasswordField();
        CheckBox   active   = new CheckBox("Active");

        if (existing != null) {
            username.setText(existing.getUsername());
            name.setText(existing.getName());
            title.setText(existing.getTitle());
            active.setSelected(existing.isActive());
            password.setPromptText("Leave blank to keep current password");
        }

        grid.addRow(0, new Label("Username:"), username);
        grid.addRow(1, new Label("Full Name:"), name);
        grid.addRow(2, new Label("Title:"), title);
        grid.addRow(3, new Label("Password:"), password);
        grid.addRow(4, active);

        ListView<Role> roleListView = new ListView<>();
        roleListView.setItems(FXCollections.observableArrayList(allRoles));
        roleListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        roleListView.setPrefHeight(120);
        if (existing != null) {
            List<Long> userRoleIds = staffRepo.getUserRoles(existing.getId())
                    .stream().map(Role::getId).collect(Collectors.toList());
            for (int i = 0; i < allRoles.size(); i++) {
                if (userRoleIds.contains(allRoles.get(i).getId()))
                    roleListView.getSelectionModel().select(i);
            }
        }
        grid.addRow(5, new Label("Roles:"), roleListView);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> btn == saveBtn ? btn : null);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String newUsername = username.getText().trim();
        String newName     = name.getText().trim();
        String newTitle    = title.getText().trim();
        String newPassword = password.getText();
        boolean newActive  = active.isSelected();
        List<Long> selectedRoleIds = roleListView.getSelectionModel().getSelectedItems()
                .stream().map(Role::getId).collect(Collectors.toList());

        if (newUsername.isEmpty() || newName.isEmpty()) {
            showAlert("Validation Error", "Username and Full Name are required.");
            return;
        }

        if (existing == null) {
            StaffUser newUser = new StaffUser(0, newUsername, newName, newTitle, "", newActive);
            long newId = staffRepo.insert(newUser, newPassword);
            if (newId > 0) {
                staffRepo.updateUserRoles(newId, selectedRoleIds);
                loadData();
            } else {
                showAlert("Error", "Could not create user. Username may already exist.");
            }
        } else {
            StaffUser updated = new StaffUser(
                existing.getId(), newUsername, newName, newTitle,
                existing.getPasswordHash(), newActive);
            if (staffRepo.update(updated, newPassword)) {
                staffRepo.updateUserRoles(existing.getId(), selectedRoleIds);
                loadData();
            } else {
                showAlert("Error", "Update failed.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private static String buildInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        return parts.length >= 2
            ? String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)
            : String.valueOf(parts[0].charAt(0)).toUpperCase();
    }

    private static final String[] AVATAR_COLORS = {
        "#3a2316", "#4a3728", "#765f52", "#0c447c", "#533ab7", "#854f0b"
    };
    private static String pickAvatarColor(long id) {
        return AVATAR_COLORS[(int)(Math.abs(id) % AVATAR_COLORS.length)];
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }
}