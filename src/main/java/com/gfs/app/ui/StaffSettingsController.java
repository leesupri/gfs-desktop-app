package com.gfs.app.ui;

import com.gfs.app.model.StaffUser;
import com.gfs.app.model.Role;
import com.gfs.app.repository.StaffRepository;
import com.gfs.app.repository.RoleRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StaffSettingsController {

    @FXML private TableView<StaffUser> staffTable;
    @FXML private TableColumn<StaffUser, String> colUsername, colName, colTitle;
    @FXML private TableColumn<StaffUser, Boolean> colActive;
    @FXML private ListView<Role> rolesListView;

    private StaffRepository staffRepo = new StaffRepository();
    private RoleRepository roleRepo = new RoleRepository();
    private List<Role> allRoles;

    @FXML
    public void initialize() {
        // Set up table columns
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        // Show roles when a user is selected
        staffTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadRolesForUser(selected.getId());
            } else {
                rolesListView.setItems(FXCollections.observableArrayList());
            }
        });

        loadData();
        allRoles = roleRepo.findAll();
    }

    private void loadData() {
        staffTable.setItems(FXCollections.observableArrayList(staffRepo.findAll()));
    }

    private void loadRolesForUser(long userId) {
        List<Role> userRoles = staffRepo.getUserRoles(userId);
        rolesListView.setItems(FXCollections.observableArrayList(userRoles));
    }

    @FXML
    private void handleNewStaff() {
        showStaffDialog(null);
    }

    @FXML
    private void handleEditStaff() {
        StaffUser selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showStaffDialog(selected);
        } else {
            showAlert("No selection", "Please select a staff member to edit.");
        }
    }

    @FXML
    private void handleDeleteStaff() {
        StaffUser selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected != null && confirm("Delete " + selected.getName() + "?")) {
            if (staffRepo.delete(selected.getId())) {
                loadData();
            } else {
                showAlert("Error", "Could not delete user. Maybe there are related records.");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    // ------------------------------------------------------------------------
    // Staff edit/create dialog
    // ------------------------------------------------------------------------
    private void showStaffDialog(StaffUser existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New Staff" : "Edit Staff");
        dialog.setHeaderText(null);

        // Buttons: Save, Cancel
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        TextField username = new TextField();
        TextField name = new TextField();
        TextField title = new TextField();
        PasswordField password = new PasswordField();
        CheckBox active = new CheckBox("Active");

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

        // Role selection (multi‑select)
        ListView<Role> roleListView = new ListView<>();
        roleListView.setItems(FXCollections.observableArrayList(allRoles));
        roleListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        if (existing != null) {
            List<Long> userRoleIds = staffRepo.getUserRoles(existing.getId())
                    .stream().map(Role::getId).collect(Collectors.toList());
            for (int i = 0; i < allRoles.size(); i++) {
                if (userRoleIds.contains(allRoles.get(i).getId())) {
                    roleListView.getSelectionModel().select(i);
                }
            }
        }
        grid.addRow(5, new Label("Roles:"), roleListView);

        dialog.getDialogPane().setContent(grid);

        // Result conversion – we only need to know if Save was clicked
        dialog.setResultConverter(btn -> btn == saveBtn ? btn : null);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            // Collect data
            String newUsername = username.getText().trim();
            String newName = name.getText().trim();
            String newTitle = title.getText().trim();
            String newPassword = password.getText();
            boolean newActive = active.isSelected();
            List<Long> selectedRoleIds = roleListView.getSelectionModel().getSelectedItems()
                    .stream().map(Role::getId).collect(Collectors.toList());

            // Validation
            if (newUsername.isEmpty() || newName.isEmpty()) {
                showAlert("Validation Error", "Username and Full Name are required.");
                return;
            }

            if (existing == null) {
                // Create new user
                // Temporary object for insertion (id = 0)
                StaffUser newUser = new StaffUser(0, newUsername, newName, newTitle, "", newActive);
                long newId = staffRepo.insert(newUser, newPassword);
                if (newId > 0) {
                    staffRepo.updateUserRoles(newId, selectedRoleIds);
                    loadData();
                } else {
                    showAlert("Error", "Could not create user. Username may already exist.");
                }
            } else {
                // Update existing – we need a mutable representation for update parameters
                // Using the same immutable object but we'll pass it to update method
                StaffUser updatedUser = new StaffUser(
                    existing.getId(),
                    newUsername,
                    newName,
                    newTitle,
                    existing.getPassword(), // keep old hash unless changed
                    newActive
                );
                boolean ok = staffRepo.update(updatedUser, newPassword);
                if (ok) {
                    staffRepo.updateUserRoles(existing.getId(), selectedRoleIds);
                    loadData();
                } else {
                    showAlert("Error", "Update failed.");
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------------
    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}