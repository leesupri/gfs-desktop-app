package com.gfs.app.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController {

    @FXML
    private Label homeTitleLabel;

    @FXML
    public void initialize() {
        homeTitleLabel.setText("Welcome to GFS Desktop App");
    }
}