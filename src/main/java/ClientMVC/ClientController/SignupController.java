package ClientMVC.ClientController;

import ClientMVC.ClientModel.LoginModel;
import ClientMVC.ClientView.Signup.SignupJFXController;
import ClientMVC.ClientView.Signup.SignupView;
import ClientMVC.ClientModel.SignupModel;
import ClientMVC.ClientView.Login.LoginView;
import javafx.application.Platform;
import javafx.stage.Stage;

public class SignupController {
    private final SignupModel model;
    private final SignupView view;
    private final SignupJFXController fxController;

    public SignupController(SignupModel model, SignupView view, SignupJFXController fxController) {
        this.model = model;
        this.view = view;
        this.fxController = fxController;

        initialize();
    }

    private void initialize() {
        fxController.setSignInListener(() -> {
            Platform.runLater(() -> {
                try {
                    // hide signup stage
                    view.getStage().hide();

                    // reuse login stage
                    Stage loginStage = ClientMVC.ClientView.Login.LoginView.getPrimaryStage();
                    if (loginStage != null) {
                        loginStage.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

    }
}
