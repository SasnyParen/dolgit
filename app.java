package com.example.demo2;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;

public class HelloApplication extends Application {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/sys";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";
    private static final String DB_TABLE = "test";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DATA = "data";

    private TableView<rowData> tableView;
    private ObservableList<rowData> data;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("test");

        tableView = new TableView<>();

        TableColumn<rowData, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_ID));
        idColumn.setResizable(false);

        TableColumn<rowData, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_NAME));
        nameColumn.setResizable(false);

        TableColumn<rowData, String> dataColumn = new TableColumn<>("Data");
        dataColumn.setCellValueFactory(new PropertyValueFactory<>(COLUMN_DATA));
        dataColumn.setResizable(false);
        dataColumn.setPrefWidth(252);

        tableView.getColumns().addAll(idColumn, nameColumn, dataColumn);

        Button updateButton = new Button("Обновить");
        updateButton.setOnAction(e -> refreshData());

        Button addButton = new Button("Добавить");
        addButton.setOnAction(e -> showAddDialog());

        Button editButton = new Button("Изменить значение");
        editButton.setOnAction(e -> showEditDialog());

        Button deleteButton = new Button("Удалить строку");
        deleteButton.setOnAction(e -> deleteRow());

        HBox buttonBox = new HBox(10, updateButton, addButton, editButton, deleteButton);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(tableView, buttonBox);
        primaryStage.setScene(new Scene(root, 415, 300));
        primaryStage.show();
        primaryStage.setResizable(false);

        refreshData();
    }

    private void refreshData() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM "+DB_TABLE)) {

            data = FXCollections.observableArrayList();
            while (rs.next()) {
                int id = rs.getInt(COLUMN_ID);
                String name = rs.getString(COLUMN_NAME);
                String data1 = rs.getString(COLUMN_DATA);
                data.add(new rowData(id, name, data1));
            }

            tableView.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAddDialog() {
        Dialog<rowData> dialog = new Dialog<>();
        dialog.setTitle("Добавить новую строку");
        dialog.setHeaderText(null);

        ButtonType addButton = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        TextField dataField = new TextField();

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Data:"), 0, 1);
        grid.add(dataField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == addButton) {
                String name = nameField.getText();
                String data = dataField.getText();
                return new rowData(0, name, data);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newRow -> insertRow(newRow));
    }

    private void insertRow(rowData newRow) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO "+DB_TABLE+" ("+COLUMN_NAME+", "+ COLUMN_DATA+ ") VALUES (?, ?)")) {

            stmt.setString(1, newRow.getName());
            stmt.setString(2, newRow.getData());
            stmt.executeUpdate();

            refreshData();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showEditDialog() {
        rowData selectedRow = tableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            return;
        }

        Dialog<rowData> dialog = new Dialog<>();
        dialog.setTitle("Изменить значение");
        dialog.setHeaderText(null);

        ButtonType editButton = new ButtonType("Изменить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(editButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(selectedRow.getName());
        TextField dataField = new TextField(selectedRow.getData());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Data:"), 0, 1);
        grid.add(dataField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == editButton) {
                String name = nameField.getText();
                String data = dataField.getText();
                return new rowData(selectedRow.getId(), name, data);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedRow -> updateRow(updatedRow));
    }

    private void updateRow(rowData updatedRow) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("UPDATE "+DB_TABLE+" SET "+COLUMN_NAME+" = ?, "+COLUMN_DATA+" = ? WHERE id = ?")) {

            stmt.setString(1, updatedRow.getName());
            stmt.setString(2, updatedRow.getData());
            stmt.setInt(3, updatedRow.getId());
            stmt.executeUpdate();

            refreshData();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteRow() {
        rowData selectedRow = tableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Удаление строки");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Вы уверены, что хотите удалить выбранную строку?");

        confirmation.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM "+DB_TABLE+" WHERE "+COLUMN_ID+" = ?")) {

                    stmt.setInt(1, selectedRow.getId());
                    stmt.executeUpdate();

                    refreshData();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static class rowData {
        private int id;
        private String name;
        private String data;

        public rowData(int id, String name, String data) {
            this.id = id;
            this.name = name;
            this.data = data;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getData() {
            return data;
        }
    }
}
