package se233.chapter3;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {
    public static Stage primaryStage;
    public static HostServices hs;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        hs = getHostServices();
        FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/se233/chapter3/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        primaryStage.setTitle("Indexer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) { 
        launch(args); 
    }
}
