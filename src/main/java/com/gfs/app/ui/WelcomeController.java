package com.gfs.app.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class WelcomeController {

    @FXML
    private Label titleLabel;

    @FXML
    public void initialize() {
        titleLabel.setText("Welcome to GFS Desktop App");
    }
}