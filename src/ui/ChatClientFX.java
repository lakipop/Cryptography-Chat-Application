package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatClientFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat_client.fxml"));
        Parent root = loader.load();
        
        // Get controller and set stage for file dialogs
        ui.controllers.ClientController controller = loader.getController();
        controller.setStage(primaryStage);
        
        // Create scene with narrow vertical layout
        Scene scene = new Scene(root, 500, 750);
        
        // Setup stage
        primaryStage.setTitle("[CLIENT] Secure Chat Client - Fleurdelyx v2.0");
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
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                System.err.println("FATAL ERROR in thread " + thread.getName() + ":");
                throwable.printStackTrace();
                System.err.flush();
                try { Thread.sleep(5000); } catch (InterruptedException e) {}
            });
            launch(args);
        } catch (Throwable t) {
            System.err.println("FATAL APPLICATION ERROR:");
            t.printStackTrace();
            System.err.flush();
            try { Thread.sleep(5000); } catch (InterruptedException e) {}
            System.exit(1);
        }
    }
}
