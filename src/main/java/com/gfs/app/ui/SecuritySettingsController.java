package com.gfs.app.ui;

import com.gfs.app.model.Permission;
import com.gfs.app.model.Role;
import com.gfs.app.repository.RoleRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SecuritySettingsController {

    // The FXML now uses ListView<Role> for the roles panel (fx:id="rolesTable")
    @FXML private ListView<Role>       rolesTable;
    @FXML private Label                roleCountLabel;
    @FXML private ListView<Permission> permissionsListView;
    @FXML private Label                permPanelTitle;

    private final RoleRepository roleRepo = new RoleRepository();
    private List<Permission> allPermissions;
    private Role currentRole;

    @FXML
    public void initialize() {
        allPermissions = roleRepo.getAllPermissions();

        setupRoleList();
        setupPermissionList();
        loadRoles();
    }

    // -------------------------------------------------------------------------
    // Role list — custom cell with inline Edit / Delete buttons
    // -------------------------------------------------------------------------
    private void setupRoleList() {
        rolesTable.setCellFactory(lv -> new ListCell<>() {
            private final Label  nameLabel  = new Label();
            private final Button editBtn    = new Button("Edit");
            private final Button deleteBtn  = new Button("Delete");
            private final HBox   actionsBox = new HBox(6, editBtn, deleteBtn);
            private final HBox   row        = new HBox(10, nameLabel, actionsBox);
            {
                HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);
                nameLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#111827;");
                actionsBox.setAlignment(Pos.CENTER_RIGHT);
                editBtn.setStyle(
                    "-fx-background-color:white; -fx-text-fill:#374151;" +
                    "-fx-border-color:#e5e7eb; -fx-border-radius:6; -fx-background-radius:6;" +
                    "-fx-font-size:11px; -fx-padding:3 9;");
                deleteBtn.setStyle(
                    "-fx-background-color:#fef2f2; -fx-text-fill:#991b1b;" +
                    "-fx-border-color:#fecaca; -fx-border-radius:6; -fx-background-radius:6;" +
                    "-fx-font-size:11px; -fx-padding:3 9;");
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 0;");

                editBtn.setOnAction(e -> {
                    Role r = getItem();
                    if (r != null) handleEditRole(r);
                });
                deleteBtn.setOnAction(e -> {
                    Role r = getItem();
                    if (r != null) handleDeleteRole(r);
                });
            }

            @Override
            protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("");
                    return;
                }
                nameLabel.setText(role.getName());
                boolean selected = rolesTable.getSelectionModel().getSelectedItem() == role;
                nameLabel.setStyle(selected
                    ? "-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#3a2316;"
                    : "-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#111827;");
                setStyle(selected
                    ? "-fx-background-color:#f5ede6; -fx-padding:10 12;"
                    : "-fx-background-color:white; -fx-padding:10 12;");
                setGraphic(row);
                setText(null);
            }
        });

        rolesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            currentRole = selected;
            if (selected != null) {
                permPanelTitle.setText("Permissions — " + selected.getName());
                loadPermissionsForRole(selected.getId());
            } else {
                permPanelTitle.setText("Permissions");
                permissionsListView.setItems(FXCollections.observableArrayList());
            }
            // Refresh cells to update selected highlight
            rolesTable.refresh();
        });
    }

    // -------------------------------------------------------------------------
    // Permission list — checkbox-style cell
    // -------------------------------------------------------------------------
    private void setupPermissionList() {
        permissionsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        permissionsListView.setCellFactory(lv -> new ListCell<>() {
            private final Label checkLabel = new Label();
            private final VBox  textBox    = new VBox(2);
            private final Label codeLabel  = new Label();
            private final Label descLabel  = new Label();
            private final HBox  row        = new HBox(12, checkLabel, textBox);
            {
                checkLabel.setStyle(
                    "-fx-min-width:18; -fx-min-height:18; -fx-max-width:18; -fx-max-height:18;" +
                    "-fx-background-radius:4; -fx-font-size:11px; -fx-alignment:center;");
                codeLabel.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#374151;");
                descLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");
                textBox.getChildren().addAll(codeLabel, descLabel);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 2 0;");
            }

            @Override
            protected void updateItem(Permission p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setGraphic(null); setText(null); return; }

                boolean assigned = isSelected();
                codeLabel.setText(p.getCode());
                descLabel.setText(p.getDescription() != null ? p.getDescription() : "");

                if (assigned) {
                    checkLabel.setText("✓");
                    checkLabel.setStyle(
                        "-fx-min-width:18; -fx-min-height:18; -fx-max-width:18; -fx-max-height:18;" +
                        "-fx-background-color:#3a2316; -fx-background-radius:4;" +
                        "-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:bold; -fx-alignment:center;");
                } else {
                    checkLabel.setText("");
                    checkLabel.setStyle(
                        "-fx-min-width:18; -fx-min-height:18; -fx-max-width:18; -fx-max-height:18;" +
                        "-fx-background-color:white; -fx-border-color:#d1d5db;" +
                        "-fx-border-radius:4; -fx-background-radius:4; -fx-alignment:center;");
                }

                setStyle(assigned
                    ? "-fx-background-color:#f5ede6; -fx-padding:8 12;"
                    : "-fx-background-color:white; -fx-padding:8 12;");
                setGraphic(row);
                setText(null);
            }
        });

        // Refresh cells on selection change so checkboxes update visually
        permissionsListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, n) -> permissionsListView.refresh());
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------
    private void loadRoles() {
        List<Role> roles = roleRepo.findAll();
        rolesTable.setItems(FXCollections.observableArrayList(roles));
        int count = roles.size();
        roleCountLabel.setText(count + " role" + (count == 1 ? "" : "s"));
        permPanelTitle.setText("Permissions");
        permissionsListView.setItems(FXCollections.observableArrayList());
        currentRole = null;
    }

    private void loadPermissionsForRole(long roleId) {
        List<Permission> rolePerms = roleRepo.getRolePermissions(roleId);
        permissionsListView.setItems(FXCollections.observableArrayList(allPermissions));
        permissionsListView.getSelectionModel().clearSelection();
        for (int i = 0; i < allPermissions.size(); i++) {
            long pid = allPermissions.get(i).getId();
            if (rolePerms.stream().anyMatch(rp -> rp.getId() == pid)) {
                permissionsListView.getSelectionModel().select(i);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------
    @FXML
    private void handleNewRole() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Role");
        dialog.setHeaderText(null);
        dialog.setContentText("Role name:");
        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            if (roleRepo.save(new Role(0, name.trim()))) {
                loadRoles();
            } else {
                showAlert("Error", "Failed to save role.");
            }
        });
    }

    private void handleEditRole(Role role) {
        TextInputDialog dialog = new TextInputDialog(role.getName());
        dialog.setTitle("Edit Role");
        dialog.setHeaderText(null);
        dialog.setContentText("Role name:");
        dialog.showAndWait().ifPresent(newName -> {
            if (newName.isBlank()) return;
            role.setName(newName.trim());
            if (roleRepo.save(role)) {
                loadRoles();
            } else {
                showAlert("Error", "Failed to update role.");
            }
        });
    }

    private void handleDeleteRole(Role role) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete role '" + role.getName() + "'?\nThis may remove permissions from users.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            if (roleRepo.deleteRole(role.getId())) {
                loadRoles();
            } else {
                showAlert("Error", "Cannot delete role. It may be assigned to users.");
            }
        }
    }

    @FXML
    private void handleSavePermissions() {
        if (currentRole == null) {
            showAlert("No role selected", "Select a role first.");
            return;
        }
        List<Long> selectedPermIds = permissionsListView.getSelectionModel()
                .getSelectedItems().stream()
                .map(Permission::getId)
                .collect(Collectors.toList());

        roleRepo.updateRolePermissions(currentRole.getId(), selectedPermIds);
        showInfo("Saved", "Permissions updated for role: " + currentRole.getName());
        // Reload to keep visual state in sync
        loadPermissionsForRole(currentRole.getId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}