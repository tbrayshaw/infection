package frontend;

import ai.AI;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;

public class PregameController {

    public ChoiceBox<String> p1choice;

    public ChoiceBox<String> p2choice;

    public Parent root;

    @SuppressWarnings("unused")
    public void initialize() {
        ObservableList<String> ais = FXCollections.observableArrayList(Main.aiNames);
        p1choice.setItems(ais);
        p1choice.setValue("Human");
        p2choice.setItems(ais);
        p2choice.setValue("Human");
    }

    public void startClicked(ActionEvent event) throws Exception {

        Stage stage = (Stage) p1choice.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("game.fxml"));

        Parent root = loader.load();
        GameController controller = loader.getController();

        AI ai1;
        String aiName1 = p1choice.getValue();
        if (aiName1 == "Human") {
            ai1 = null;
        } else {
            Class aiClass = Class.forName("ai." + aiName1);
            ai1 = (AI)aiClass.newInstance();
        }

        AI ai2;
        String aiName2 = p2choice.getValue();
        if (aiName2 == "Human") {
            ai2 = null;
        } else {
            Class aiClass = Class.forName("ai." + aiName2);
            ai2 = (AI)aiClass.newInstance();
        }

        controller.setAIs(ai1,aiName1,ai2,aiName2);

        stage.setScene(new Scene(root, 500, 600));
        stage.show();
    }


}
