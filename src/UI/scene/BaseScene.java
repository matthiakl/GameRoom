package UI.scene;

import UI.Main;
import UI.button.ImageButton;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

/**
 * Created by LM on 03/07/2016.
 */
public abstract class BaseScene extends Scene {
    public final static double FADE_IN_OUT_TIME = 0.1;
    private StackPane rootStackPane;
    private Stage parentStage;

    public BaseScene(StackPane stackPane,Stage parentStage){
        super(stackPane, Main.GENERAL_SETTINGS.getWindowWidth(), Main.GENERAL_SETTINGS.getWindowHeight());
        this.rootStackPane = stackPane;
        this.parentStage = parentStage;
        getStylesheets().add("res/flatterfx.css");
        initAndAddWrappingPaneToRoot();

        widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
                //Main.logger.debug("New window's width : "+ newSceneWidth);
                Main.GENERAL_SETTINGS.setWindowWidth(newSceneWidth.intValue());
            }
        });
        heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
                //Main.logger.debug("New window's height : "+ newSceneHeight);
                Main.GENERAL_SETTINGS.setWindowHeight(newSceneHeight.intValue());
            }
        });

    }
    public StackPane getRootStackPane(){
        return rootStackPane;
    }

    public void fadeTransitionTo(BaseScene scene2, Stage stage){
        Timeline fadeOutTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(getWrappingPane().opacityProperty(), getWrappingPane().opacityProperty().getValue(), Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                        new KeyValue(getWrappingPane().opacityProperty(), 0, Interpolator.LINEAR)
                ));
        fadeOutTimeline.setCycleCount(1);
        fadeOutTimeline.setAutoReverse(false);
        fadeOutTimeline.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                scene2.getWrappingPane().setOpacity(0);
                stage.setScene(scene2);
                Timeline fadeInTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0),
                                new KeyValue(scene2.getWrappingPane().opacityProperty(), 0, Interpolator.LINEAR)),
                        new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                new KeyValue(scene2.getWrappingPane().opacityProperty(), 1, Interpolator.LINEAR)
                        ));
                fadeInTimeline.setCycleCount(1);
                fadeInTimeline.setAutoReverse(false);
                fadeInTimeline.play();
            }
        });
        fadeOutTimeline.play();
    }

    /**
     *
     * @return wrapping pane, which is just under the root pane
     */
    public abstract Pane getWrappingPane();

    abstract void initAndAddWrappingPaneToRoot();

    public Stage getParentStage() {
        return parentStage;
    }

    public void addEscapeKeyEvent(ImageButton backButton){
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()){
                case ESCAPE:
                    backButton.fireEvent(new MouseEvent(MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 0, false, false, false, false, false, false, false, false, false, false, null));
                    break;
                default:
                    break;
            }
        });
    }
}
