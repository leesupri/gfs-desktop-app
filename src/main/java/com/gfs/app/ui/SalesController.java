package com.gfs.app.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SalesController {

    @FXML
    private Label salesTitleLabel;

    @FXML
    public void initialize() {
        salesTitleLabel.setText("Sales Page");
    }
}