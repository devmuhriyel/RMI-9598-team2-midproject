package org.chatSocketProgramming.it2229598_prelims;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage pStage) throws Exception {
        Group root = new Group();
        Scene pScene = new Scene(root, Color.NAVY);
        pStage.setTitle("Client Chat Application");
        pStage.setScene(pScene);
        pStage.show();
    }
}

