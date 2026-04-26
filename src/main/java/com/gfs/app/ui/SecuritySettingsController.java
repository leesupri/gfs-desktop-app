package com.gfs.app.ui;

import com.gfs.app.model.Role;
import com.gfs.app.model.Permission;
import com.gfs.app.repository.RoleRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SecuritySettingsController {

    @FXML private TableView<Role> rolesTable;
    @FXML private TableColumn<Role, String> colRoleName;
    @FXML private ListView<Permission> permissionsListView;

    private final RoleRepository roleRepo = new RoleRepository();
    private List<Permission> allPermissions;
    private Role currentRole;

    @FXML
    public void initialize() {
        colRoleName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        rolesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            currentRole = selected;
            if (selected != null) {
                loadPermissionsForRole(selected.getId());
            } else {
                permissionsListView.setItems(FXCollections.observableArrayList());
            }
        });

        loadRoles();
        allPermissions = roleRepo.getAllPermissions();
    }

    private void loadRoles() {
        rolesTable.setItems(FXCollections.observableArrayList(roleRepo.findAll()));
    }

    private void loadPermissionsForRole(long roleId) {
        List<Permission> rolePerms = roleRepo.getRolePermissions(roleId);

        // Create a list of all permissions with a checkmark indicator if assigned
        permissionsListView.setItems(FXCollections.observableArrayList(allPermissions));
        permissionsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Permission p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    boolean assigned = rolePerms.stream().anyMatch(rp -> rp.getId() == p.getId());
                    setText(p.getCode() + " - " + p.getDescription() + (assigned ? " ✓" : ""));
                }
            }
        });

        // Pre‑select the permissions that are already assigned
        permissionsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        for (int i = 0; i < allPermissions.size(); i++) {
            long pid = allPermissions.get(i).getId();
            if (rolePerms.stream().anyMatch(rp -> rp.getId() == pid)) {
                permissionsListView.getSelectionModel().select(i);
            }
        }
    }

    @FXML
    private void handleNewRole() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Role");
        dialog.setHeaderText(null);
        dialog.setContentText("Role name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            Role newRole = new Role(0, name);
            if (roleRepo.save(newRole)) {
                loadRoles();
            } else {
                showAlert("Error", "Failed to save role.");
            }
        });
    }

    @FXML
    private void handleEditRole() {
        Role selected = rolesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Select a role to edit.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Edit Role");
        dialog.setHeaderText(null);
        dialog.setContentText("Role name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            selected.setName(newName);
            if (roleRepo.save(selected)) {
                loadRoles();
            } else {
                showAlert("Error", "Failed to update role.");
            }
        });
    }

    @FXML
    private void handleDeleteRole() {
        Role selected = rolesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Select a role to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete role '" + selected.getName() + "'?\nThis may remove permissions from users.", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            if (roleRepo.deleteRole(selected.getId())) {
                loadRoles();
                permissionsListView.setItems(FXCollections.observableArrayList());
                currentRole = null;
            } else {
                showAlert("Error", "Cannot delete role. Possibly assigned to users.");
            }
        }
    }

    @FXML
    private void handleSavePermissions() {
        if (currentRole == null) {
            showAlert("No role selected", "Select a role from the table first.");
            return;
        }

        List<Long> selectedPermIds = permissionsListView.getSelectionModel().getSelectedItems()
                .stream()
                .map(Permission::getId)
                .collect(Collectors.toList());

        roleRepo.updateRolePermissions(currentRole.getId(), selectedPermIds);
        showAlert("Success", "Permissions updated.");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}