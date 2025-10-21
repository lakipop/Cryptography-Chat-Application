package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatServerFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat_server.fxml"));
        Parent root = loader.load();
        
        // Create scene with narrow vertical layout
        Scene scene = new Scene(root, 500, 750);
        
        // Setup stage
        primaryStage.setTitle("[SERVER] Secure Chat Server - Fleurdelyx v2.0");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(750);
        
        // Show stage
        primaryStage.show();
        
        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
