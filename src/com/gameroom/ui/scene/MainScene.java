package com.gameroom.ui.scene;

import com.gameroom.data.game.GameWatcher;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.entry.GameEntryUtils;
import com.gameroom.data.game.scanner.OnScannerResultHandler;
import com.gameroom.data.http.images.ImageUtils;
import com.gameroom.system.application.SupportService;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.ui.Main;
import com.gameroom.ui.UIValues;
import com.gameroom.ui.control.button.gamebutton.GameButton;
import com.gameroom.ui.control.drawer.DrawerMenu;
import com.gameroom.ui.control.drawer.GroupType;
import com.gameroom.ui.control.drawer.SortType;
import com.gameroom.ui.control.specific.FloatingSearchBar;
import com.gameroom.ui.dialog.GameRoomAlert;
import com.gameroom.ui.dialog.GamesFoldersDialog;
import com.gameroom.ui.dialog.selector.GameScannerSelector;
import com.gameroom.ui.pane.gamestilepane.*;
import com.gameroom.ui.scene.exitaction.ClassicExitAction;
import com.gameroom.ui.scene.exitaction.ExitAction;
import com.gameroom.ui.scene.exitaction.MultiAddExitAction;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.ui.Main.*;
import static com.gameroom.ui.control.button.gamebutton.GameButton.COVER_HEIGHT_WIDTH_RATIO;

/**
 * Created by LM on 03/07/2016.
 */
public class MainScene extends BaseScene {
    private int input_mode = 0;

    public final static int INPUT_MODE_MOUSE = 0;
    public final static int INPUT_MODE_KEYBOARD = 1;

    private static boolean GARBAGE_COLLECTED_RECENTLY = false;

    private VBox tilesPaneWrapper = new VBox();
    private ScrollPane scrollPane;
    private BorderPane wrappingPane;
    private Pane emptyMessagePane;

    private DrawerMenu drawerMenu;

    private GamesTilePane tilePane;
    private RowCoverTilePane lastPlayedTilePane;
    private RowCoverTilePane recentlyAddedTilePane;
    private ToAddRowTilePane toAddTilePane;

    private ArrayList<GroupRowTilePane> groupRowList = new ArrayList<>();

    private FloatingSearchBar searchBar;
    private boolean showTilesPaneAgainAfterCancelSearch = false;

    private Label statusLabel;

    private boolean changeBackgroundNextTime = false;

    private SortType sortType = SortType.NAME;
    private GroupType groupType = GroupType.DEFAULT;

    public MainScene(Stage parentStage) {
        super(new StackPane(), parentStage, parentStage.getWidth(), parentStage.getHeight());
        setCursor(Cursor.DEFAULT);
        addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            if (getInputMode() == MainScene.INPUT_MODE_KEYBOARD) {
                setInputMode(MainScene.INPUT_MODE_MOUSE);
            }
        });
        initAll();
    }

    private void initAll() {
        initCenter();
        initTop();
       //displayWelcomeMessage();
        loadGames();
        configureAutomaticCaching();
        loadPreviousUIValues();
        SupportService.start();
    }

    private void configureAutomaticCaching() {
        //to empty ram usage
        tilePane.setCacheGameButtons(true);
        recentlyAddedTilePane.setCacheGameButtons(true);
        toAddTilePane.setCacheGameButtons(true);
        lastPlayedTilePane.setCacheGameButtons(true);
        for (GroupRowTilePane g : groupRowList) {
            g.setCacheGameButtons(true);
        }
    }

    private void loadPreviousUIValues() {
        Main.runAndWait(() -> {
            if (settings().getBoolean(PredefinedSetting.FOLDED_ROW_LAST_PLAYED)) {
                lastPlayedTilePane.fold();
            } else {
                lastPlayedTilePane.unfold();
            }
            if (settings().getBoolean(PredefinedSetting.FOLDED_ROW_RECENTLY_ADDED)) {
                recentlyAddedTilePane.fold();
            } else {
                recentlyAddedTilePane.unfold();
            }
            if (settings().getBoolean(PredefinedSetting.FOLDED_TOADD_ROW)) {
                toAddTilePane.fold();
            } else {
                toAddTilePane.unfold();
            }
            double scrollBarVValue = settings().getDouble(PredefinedSetting.SCROLLBAR_VVALUE);
            scrollPane.setVvalue(scrollBarVValue);

            if (settings().getBoolean(PredefinedSetting.HIDE_TILES_ROWS)) {
                forceHideTilesRows(true);
            }

            if (settings().getBoolean(PredefinedSetting.HIDE_TOOLBAR)) {
                //TODO maybe try to hide the drawer menu ?
                //drawerMenu.setVisible(false);
            }
        });
    }

    public void saveScrollBarVValue() {
        double scrollBarVValue = scrollPane.getVvalue();
        settings().setSettingValue(PredefinedSetting.SCROLLBAR_VVALUE, scrollBarVValue);
    }

    private void displayWelcomeMessage() {
        if (settings().getBoolean(PredefinedSetting.DISPLAY_WELCOME_MESSAGE)) {
            Platform.runLater(() -> {
                GameRoomAlert.info(Main.getString("Welcome_message"));
                GameRoomAlert.info(Main.getString("configure_scanner_messages"));

                GameScannerSelector selector = new GameScannerSelector();
                Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                ignoredOptionnal.ifPresent(pairs -> {
                    if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                        settings().setSettingValue(PredefinedSetting.ENABLED_GAME_SCANNERS, selector.getDisabledScanners());
                    }
                });
                new GamesFoldersDialog().showAndWait();
                settings().setSettingValue(PredefinedSetting.DISPLAY_WELCOME_MESSAGE, false);
                startGameWatcherService();
            });
        }
    }

    @Override
    public Pane getWrappingPane() {
        return wrappingPane;
    }

    @Override
    void initAndAddWrappingPaneToRoot() {
        maskView.setOpacity(0);
        setChangeBackgroundNextTime(true);

        wrappingPane = new BorderPane();
        getRootStackPane().getChildren().add(wrappingPane);
        statusLabel = new Label();
        getRootStackPane().getChildren().add(statusLabel);
    }

    @Override
    public void onPaused() {
        scrollPane.maxHeightProperty().unbind();
    }

    @Override
    public void onResumed() {
        scrollPane.maxHeightProperty().bind(heightProperty());
    }

    private void initCenter() {
        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.maxHeightProperty().bind(heightProperty());

        //centerPane.setPrefViewportHeight(tilePane.getPrefHeight());
        scrollPane.setFocusTraversable(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        tilesPaneWrapper.setSpacing(5 * Main.SCREEN_HEIGHT / 1080);
        tilePane = new CoverTilePane(this, Main.getString("all_games"));
        tilePane.setId("mainTilePane");
        tilePane.setQuickSearchEnabled(true);

        lastPlayedTilePane = new RowCoverTilePane(this, RowCoverTilePane.TYPE_LAST_PLAYED);
        lastPlayedTilePane.setId("lastPlayedTilePane");
        lastPlayedTilePane.setDisplayGamesCount(false);
        recentlyAddedTilePane = new RowCoverTilePane(this, RowCoverTilePane.TYPE_RECENTLY_ADDED);
        recentlyAddedTilePane.setId("recentlyAddedTilePane");
        recentlyAddedTilePane.setDisplayGamesCount(false);
        toAddTilePane = new ToAddRowTilePane(this) {
            @Override
            protected void batchAddEntries(ArrayList<GameEntry> entries) {
                batchAddGameEntries(entries, 0).run();
            }
        };
        toAddTilePane.setId("toAddTilePane");

        lastPlayedTilePane.addOnFoldedChangeListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                settings().setSettingValue(PredefinedSetting.FOLDED_ROW_LAST_PLAYED, newValue);
            }
        });
        recentlyAddedTilePane.addOnFoldedChangeListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                settings().setSettingValue(PredefinedSetting.FOLDED_ROW_RECENTLY_ADDED, newValue);
            }
        });
        toAddTilePane.addOnFoldedChangeListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                settings().setSettingValue(PredefinedSetting.FOLDED_TOADD_ROW, newValue);
            }
        });

        statusLabel.setText(Main.getString("loading") + "...");
        wrappingPane.setOpacity(0);

        try {
            remapArrowKeys(scrollPane);
        } catch (AWTException e) {
            e.printStackTrace();
        }

        initKeyShortcuts();

        GridPane topTilesPaneGridPane = new GridPane();
        ColumnConstraints halfConstraint = new ColumnConstraints();
        halfConstraint.setPercentWidth(50);
        //halfConstraint.maxWidthProperty().bind(lastPlayedTilePane.maxWidthProperty());
        lastPlayedTilePane.managedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                halfConstraint.setPercentWidth(50);
            } else {
                halfConstraint.setPercentWidth(0);
            }
        });

        topTilesPaneGridPane.getColumnConstraints().add(halfConstraint);
        topTilesPaneGridPane.add(lastPlayedTilePane, 0, 0);
        topTilesPaneGridPane.add(recentlyAddedTilePane, 1, 0);
        topTilesPaneGridPane.setHgap(50 * Main.SCREEN_WIDTH / 1920);

        tilesPaneWrapper.getChildren().addAll(toAddTilePane, topTilesPaneGridPane, tilePane);
        scrollPane.setContent(tilesPaneWrapper);
        scrollPane.setStyle("-fx-background-color: transparent;");
        wrappingPane.setCenter(scrollPane);
        drawerMenu = new DrawerMenu(this);
        drawerMenu.setFocusTraversable(false);

        wrappingPane.setLeft(drawerMenu);
        wrappingPane.setStyle("-fx-background-color: transparent;");

        emptyMessagePane = getEmptyMessagePane();
        emptyMessagePane.setVisible(false);

        getRootStackPane().getChildren().add(emptyMessagePane);

    }

    private void loadGames() {
        backgroundView.setVisible(false);
        maskView.setVisible(false);
        Task<Void> loadGamesTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                tilePane.setAutomaticSort(false);
                recentlyAddedTilePane.setAutomaticSort(false);
                lastPlayedTilePane.setAutomaticSort(false);
                toAddTilePane.setAutomaticSort(false);

                int i = 0;

                long refreshWPTime = 800;
                long lastWallpaperUpdate = 0;

                for (GameEntry entry : GameEntryUtils.ENTRIES_LIST) {
                    int finalI = i;

                    Main.runAndWait(() -> {
                        setChangeBackgroundNextTime(true);
                        addGame(entry);
                    });
                    long currentTime = System.currentTimeMillis();
                    if (lastWallpaperUpdate == 0) {
                        lastWallpaperUpdate = currentTime - (long) (Math.random() * refreshWPTime);
                    }
                    if (currentTime - lastWallpaperUpdate > refreshWPTime) {
                        lastWallpaperUpdate = currentTime;
                        setChangeBackgroundNextTime(false);
                        setImageBackground(entry.getImagePath(1));
                    }
                    updateProgress(finalI, GameEntryUtils.ENTRIES_LIST.size() - 1);
                    i++;
                }
                Platform.runLater(() -> checkDisplayEmptyMessagePane());
                return null;
            }
        };
        loadGamesTask.setOnSucceeded(event -> {
            tilePane.setAutomaticSort(true);
            recentlyAddedTilePane.setAutomaticSort(true);
            lastPlayedTilePane.setAutomaticSort(true);
            backgroundView.setOpacity(0);
            backgroundView.setVisible(true);
            maskView.setOpacity(0);
            maskView.setVisible(true);
            setChangeBackgroundNextTime(false);

            //dialog.getDialogStage().close();
            statusLabel.setText("");
            fadeTransitionTo(MainScene.this, getParentStage(), false);
            Platform.runLater(() -> {
                startGameWatcherService();
            });
            home();

            double scrollBarVValue = settings().getDouble(PredefinedSetting.SCROLLBAR_VVALUE);
            scrollPane.setVvalue(scrollBarVValue);

            if (settings().getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER) && SUPPORTER_MODE) {
                File workingDir = FILES_MAP.get("working_dir");
                if (workingDir != null && workingDir.listFiles() != null) {
                    for (File file : workingDir.listFiles()) {
                        if (file.isFile() && file.getName().startsWith("wallpaper")) {
                            setChangeBackgroundNextTime(false);
                            setImageBackground(file, true);
                            break;
                        }
                    }
                }
            }
            String sortById = settings().getString(PredefinedSetting.SORT_BY);
            String groupById = settings().getString(PredefinedSetting.GROUP_BY);

            sortType = SortType.fromId(sortById);
            groupType = GroupType.fromId(groupById);

            //will also sort by sortType so no double call here.
            groupBy(groupType);

            /*ObjectBinding<Bounds> visibleBounds = Bindings.createObjectBinding(() -> {
                Bounds viewportBounds = scrollPane.getViewportBounds();
                Bounds viewportBoundsInScene = scrollPane.localToScene(viewportBounds);
                Bounds viewportBoundsInPane = tilesPaneWrapper.sceneToLocal(viewportBoundsInScene);
                return viewportBoundsInPane ;
            }, scrollPane.hvalueProperty(), scrollPane.vvalueProperty(), scrollPane.viewportBoundsProperty());


            FilteredList<GameButton> visibleNodes = new FilteredList<>(tilePane.getGameButtons());
            visibleNodes.predicateProperty().bind(Bindings.createObjectBinding(() ->
                            gameButton -> gameButton.getBoundsInParent().intersects(visibleBounds.get()),
                    visibleBounds));


            visibleNodes.addListener((ListChangeListener.Change<? extends GameButton> c) -> {
                if(c.next()){
                    c.getAddedSubList().forEach(o -> {
                        o.clearCover();
                        o.setCache(false);
                        Main.LOGGER.debug("clearedCover: "+o.getEntry().getName());
                    });
                    c.getRemoved().forEach(o -> {
                        o.showCover();
                        Main.LOGGER.debug("showedCover: "+o.getEntry().getName());
                    });
                    System.out.println();
                }
            });*/
        });
        loadGamesTask.progressProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            if (newValue.doubleValue() == 1.0) {
                statusLabel.setText("");
            } else {
                statusLabel.setText(Main.getString("loading") + " " + Math.round(newValue.doubleValue() * 100) + "%...");
            }
        }));
        Main.getExecutorService().submit(loadGamesTask);
    }

    public void centerGameButtonInScrollPane(Node n, GamesTilePane pane) {
        //TODO fix here, input the right calculation to center gameButton
        double h = scrollPane.getContent().getBoundsInLocal().getHeight();
        double y = pane.getBoundsInParent().getMinY() + (n.getBoundsInParent().getMaxY() +
                n.getBoundsInParent().getMinY()) / 2.0;

        double v = scrollPane.getViewportBounds().getHeight();
        scrollPane.setVvalue(scrollPane.getVmax() * ((y - 0.5 * v) / (h - v)));
    }

    private void initTop() {

        searchBar = new FloatingSearchBar((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals("")) {
                searchGame(newValue);
            } else if (newValue != null && newValue.equals("")) {
                cancelSearch();
            }
        });
        searchBar.hide();

        AnchorPane pane = new AnchorPane();
        pane.getChildren().add(searchBar);
        drawerMenu.translateXProperty().addListener((observable, oldValue, newValue) -> {
            pane.setTranslateX(drawerMenu.getWidth() + newValue.doubleValue());
        });
        drawerMenu.widthProperty().addListener((observable, oldValue, newValue) -> {
            pane.setTranslateX(newValue.doubleValue() + drawerMenu.getTranslateX());
        });
        AnchorPane.setLeftAnchor(searchBar, 0.0);
        AnchorPane.setBottomAnchor(searchBar, 0.0);
        pane.setPickOnBounds(false);
        getRootStackPane().getChildren().add(pane);
    }

    public void forceHideTilesRows(boolean hide) {
        if (toAddTilePane != null) {
            toAddTilePane.setForcedHidden(hide);
        }
        if (lastPlayedTilePane != null) {
            lastPlayedTilePane.setForcedHidden(hide);
        }
        if (recentlyAddedTilePane != null) {
            recentlyAddedTilePane.setForcedHidden(hide);

        }
    }

    public void toggleTilesRows() {
        boolean wasHidden = settings().getBoolean(PredefinedSetting.HIDE_TILES_ROWS);
        settings().setSettingValue(PredefinedSetting.HIDE_TILES_ROWS, !wasHidden);
        forceHideTilesRows(!wasHidden);
    }

    public void toggleScrollBar(boolean fullScreen) {
        boolean disableInFullscreen = settings().getBoolean(PredefinedSetting.DISABLE_SCROLLBAR_IN_FULLSCREEN);
        if (scrollPane != null) {
            if (fullScreen && disableInFullscreen) {
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            } else if (!fullScreen) {
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            }
        }
    }

    private void refreshTrayMenu() {
        Main.START_TRAY_MENU.removeAll();

        GameEntryUtils.ENTRIES_LIST
                .stream()
                .sorted(Comparator.comparing(GameEntry::getName))
                .forEach(entry -> {
                    java.awt.MenuItem gameItem = new java.awt.MenuItem(entry.getName());
                    gameItem.addActionListener(e -> entry.startGame());
                    Main.START_TRAY_MENU.add(gameItem);
                });
        Main.START_TRAY_MENU.setEnabled(true);

    }

    public void home() {
        tilePane.sortByName();
        tilePane.setForcedHidden(false);
        tilePane.show();
        if (tilePane.isSearching()) {
            searchBar.clearSearchField();
        }
        lastPlayedTilePane.setForcedHidden(false);
        recentlyAddedTilePane.setForcedHidden(false);
        toAddTilePane.setForcedHidden(false);

        tilesPaneWrapper.getChildren().removeAll(groupRowList);
        groupRowList.clear();

        scrollPane.setVvalue(scrollPane.getVmin());
    }

    private void cancelSearch() {
        if (showTilesPaneAgainAfterCancelSearch) {
            lastPlayedTilePane.setForcedHidden(false);
            recentlyAddedTilePane.setForcedHidden(false);
            toAddTilePane.setForcedHidden(false);
        }
        tilePane.setTitle(Main.getString("all_games"));
        tilePane.cancelSearchText();
        if (groupRowList.size() > 0) {
            groupRowList.forEach(tilePane -> {
                tilePane.show();
                tilePane.cancelSearchText();
            });
            tilePane.hide();
        }
    }

    private void searchGame(String text) {
        tilePane.show();
        if (!tilePane.isSearching()) {
            showTilesPaneAgainAfterCancelSearch = lastPlayedTilePane.isManaged();
        }
        groupRowList.forEach(tilePane -> {
            tilePane.hide();
            tilePane.searchText(text);
        });

        lastPlayedTilePane.setForcedHidden(true);
        recentlyAddedTilePane.setForcedHidden(true);
        toAddTilePane.setForcedHidden(true);
        int found = tilePane.searchText(text);
        tilePane.setTitle(found + " " + Main.getString("results_found_for") + " \"" + text + "\"");
    }

    public void removeGame(GameEntry entry) {
        tilePane.removeGame(entry);
        lastPlayedTilePane.removeGame(entry);
        recentlyAddedTilePane.removeGame(entry);
        toAddTilePane.removeGame(entry);
        GameWatcher.getInstance().removeGame(entry);

        groupRowList.forEach(tilePane1 -> tilePane1.removeGame(entry));

        GameEntryUtils.removeGame(entry);
        refreshTrayMenu();

        checkDisplayEmptyMessagePane();
    }

    public void updateGame(GameEntry entry) {
        tilePane.updateGame(entry);
        lastPlayedTilePane.updateGame(entry);
        recentlyAddedTilePane.updateGame(entry);
        toAddTilePane.updateGame(entry);

        groupRowList.forEach(tilePane1 -> tilePane1.updateGame(entry));

        GameEntryUtils.updateGame(entry);
        refreshTrayMenu();

        sortBy(sortType);

        checkDisplayEmptyMessagePane();
    }

    public void addGame(GameEntry entry) {
        tilePane.addGame(entry);
        lastPlayedTilePane.addGame(entry);
        recentlyAddedTilePane.addGame(entry);
        toAddTilePane.removeGame(entry);
        GameWatcher.getInstance().removeGame(entry);

        groupRowList.forEach(tilePane1 -> tilePane1.addGame(entry));

        GameEntryUtils.addGame(entry);
        refreshTrayMenu();

        checkDisplayEmptyMessagePane();
    }

    public ExitAction batchAddGameEntries(ArrayList<GameEntry> entries, int entriesCount) {
        if (entriesCount < entries.size()) {
            GameEntry currentEntry = entries.get(entriesCount);
            GameEditScene gameEditScene = new GameEditScene(MainScene.this, currentEntry, GameEditScene.MODE_ADD, null);
            gameEditScene.disableBackButton();
            return new MultiAddExitAction(() -> {
                ExitAction action = batchAddGameEntries(entries, entriesCount + 1);
                gameEditScene.setOnExitAction(action); //create interface runnable to access property GameEditScene
                gameEditScene.addCancelButton(action);
                gameEditScene.addCancelAllButton();
                fadeTransitionTo(gameEditScene, getParentStage());
            }, gameEditScene);
        } else {
            return new ClassicExitAction(this, getParentStage(), MAIN_SCENE);
        }
    }

    private void startGameWatcherService() {
        if (settings().getBoolean(PredefinedSetting.DISPLAY_WELCOME_MESSAGE)) {
            return;
        }
        //toAddTilePane.disableFoldButton(true);
        toAddTilePane.setAutomaticSort(false);

        toAddTilePane.getIconButton().setOnAction(event -> GameWatcher.getInstance().start(true));
        GameWatcher.getInstance().setOnGameFoundHandler(new OnScannerResultHandler() {
            @Override
            public GameButton gameToAddFound(GameEntry entry) {
                toAddTilePane.addGame(entry);
                Platform.runLater(() -> checkDisplayEmptyMessagePane());
                return toAddTilePane.getGameButton(entry);
            }

            @Override
            public void onAllGamesFound(int gamesCount) {
                //toAddTilePane.disableFoldButton(false);
                toAddTilePane.show();
                Platform.runLater(() -> {
                    toAddTilePane.unfold();
                });
            }
        });
        GameWatcher.getInstance().start(false);
    }


    public int getInputMode() {
        return input_mode;
    }

    public void setInputMode(int input_mode) {
        this.input_mode = input_mode;
        switch (input_mode) {
            case INPUT_MODE_KEYBOARD:
                setCursor(Cursor.NONE);
                wrappingPane.setMouseTransparent(true);
                break;

            default:
            case INPUT_MODE_MOUSE:
                setCursor(Cursor.DEFAULT);
                wrappingPane.setMouseTransparent(false);
                break;
        }
    }

    public void setChangeBackgroundNextTime(boolean changeBackgroundNextTime) {
        this.changeBackgroundNextTime = changeBackgroundNextTime;
    }

    private void remapArrowKeys(ScrollPane scrollPane) throws AWTException {
        java.util.List<KeyEvent> mappedEvents = new ArrayList<>();
        scrollPane.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (mappedEvents.remove(event))
                    return;
                if (!event.isShiftDown()) {
                    switch (event.getCode()) {
                        case UP:
                        case DOWN:
                        case LEFT:
                        case RIGHT:
                        case ENTER:
                            setInputMode(INPUT_MODE_KEYBOARD);

                            KeyEvent newEvent = remap(event);
                            mappedEvents.add(newEvent);
                            event.consume();
                            javafx.event.Event.fireEvent(event.getTarget(), newEvent);
                            break;
                        default:
                            /*if(event.getEventType().equals(KeyEvent.KEY_PRESSED)) {
                                tilePane.getOnKeyTyped().handle(event);
                            }*/
                            break;
                    }
                }
            }

            private KeyEvent remap(KeyEvent event) {
                KeyEvent newEvent = new KeyEvent(
                        event.getEventType(),
                        event.getCharacter(),
                        event.getText(),
                        event.getCode(),
                        !event.isShiftDown(),
                        event.isControlDown(),
                        event.isAltDown(),
                        event.isMetaDown()
                );

                return newEvent.copyFor(event.getSource(), event.getTarget());
            }
        });
    }

    public void triggerKeyPressedOnMainPane(KeyEvent keyPressed) {
        tilePane.getOnKeyPressed().handle(keyPressed);
    }


    public void setImageBackground(File imgFile) {
        setImageBackground(imgFile, false);
    }

    /**
     * Sets the background image of the this {@link MainScene}. Checks if it should update the background or not (option
     * {@link PredefinedSetting#DISABLE_MAINSCENE_WALLPAPER} chosen or not), and if it should not change because the user
     * has set a static background image.
     *
     * @param imgFile  the imgFile to use to change the background
     * @param isStatic whether this image should not be changed by {@link GameButton}
     */
    public void setImageBackground(File imgFile, boolean isStatic) {
        if (settings().getBoolean(PredefinedSetting.ENABLE_STATIC_WALLPAPER) && !isStatic) {
            return;
        }
        if (!settings().getBoolean(PredefinedSetting.DISABLE_MAINSCENE_WALLPAPER)) {
            if (!backgroundView.isVisible()) {
                backgroundView.setVisible(true);
            }
            if (!maskView.isVisible()) {
                maskView.setVisible(true);
            }
            if (!changeBackgroundNextTime) {
                if (imgFile != null) {
                    ImageUtils.transitionToWindowBackground(imgFile, backgroundView);
                    if (maskView.getOpacity() != 1) {
                        Timeline fadeInTimeline = new Timeline(
                                new KeyFrame(Duration.seconds(0),
                                        new KeyValue(maskView.opacityProperty(), maskView.opacityProperty().getValue(), Interpolator.EASE_IN)),
                                new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                        new KeyValue(maskView.opacityProperty(), 1, Interpolator.EASE_OUT)
                                ));
                        fadeInTimeline.setCycleCount(1);
                        fadeInTimeline.setAutoReverse(false);
                        fadeInTimeline.play();
                    }
                } else {
                    Timeline fadeOutTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(0),
                                    new KeyValue(backgroundView.opacityProperty(), backgroundView.opacityProperty().getValue(), Interpolator.EASE_IN),
                                    new KeyValue(maskView.opacityProperty(), maskView.opacityProperty().getValue(), Interpolator.EASE_IN)),
                            new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                                    new KeyValue(backgroundView.opacityProperty(), 0, Interpolator.EASE_OUT),
                                    new KeyValue(maskView.opacityProperty(), 0, Interpolator.EASE_OUT)
                            ));
                    fadeOutTimeline.setCycleCount(1);
                    fadeOutTimeline.setAutoReverse(false);
                    fadeOutTimeline.play();
                }
            } else {
                changeBackgroundNextTime = false;
            }
        } else {
            if (backgroundView.isVisible()) {
                backgroundView.setVisible(false);
            }
            if (maskView.isVisible()) {
                maskView.setVisible(false);
            }
        }
    }

    public void toggleSearchField(){
        if(!searchBar.isVisible()){
            showSearchField();
        }else{
            hideSearchField();
        }
    }

    public void showSearchField() {
        searchBar.show();
        searchBar.getSearchField().requestFocus();
    }

    public void hideSearchField() {
        searchBar.hide();
    }

    public void groupBy(GroupType groupType) {
        showTilesPaneAgainAfterCancelSearch = false;
        tilesPaneWrapper.getChildren().removeAll(groupRowList);
        tilePane.setForcedHidden(true);
        groupRowList.clear();

        this.groupType = groupType;
        settings().setSettingValue(PredefinedSetting.GROUP_BY, groupType.getId());

        switch (groupType) {
            case DEFAULT:
                tilePane.setForcedHidden(false);
                break;
            case THEME:
                groupRowList = GroupsFactory.createGroupsByTheme(lastPlayedTilePane, this);
                break;
            case GENRE:
                groupRowList = GroupsFactory.createGroupsByGenre(lastPlayedTilePane, this);
                break;
            case SERIE:
                groupRowList = GroupsFactory.createGroupsBySerie(lastPlayedTilePane, this);
                break;
            case LAUNCHER:
                groupRowList = GroupsFactory.createGroupsByLaunchers(lastPlayedTilePane, this);
                break;
        }
        tilesPaneWrapper.getChildren().addAll(groupRowList);
        scrollPane.setVvalue(scrollPane.getVmin());

        sortBy(sortType);
    }

    public void sortBy(SortType sortType) {
        showTilesPaneAgainAfterCancelSearch = false;
        this.sortType = sortType;
        settings().setSettingValue(PredefinedSetting.SORT_BY, sortType.getId());

        switch (sortType) {
            case NAME:
                tilePane.sortByName();
                groupRowList.forEach(CoverTilePane::sortByName);
                break;
            case PLAY_TIME:
                tilePane.sortByTimePlayed();
                groupRowList.forEach(CoverTilePane::sortByTimePlayed);
                break;
            case RELEASE_DATE:
                tilePane.sortByReleaseDate();
                groupRowList.forEach(CoverTilePane::sortByReleaseDate);
                break;
            case RATING:
                tilePane.sortByRating();
                groupRowList.forEach(CoverTilePane::sortByRating);
                break;

        }
        scrollPane.setVvalue(scrollPane.getVmin());
    }

    public void newTileZoom(double value) {
        tilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 4 * value);
        tilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 4 * COVER_HEIGHT_WIDTH_RATIO * value);

        lastPlayedTilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 7 * value);
        lastPlayedTilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 7 * COVER_HEIGHT_WIDTH_RATIO * value);


        recentlyAddedTilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 7 * value);
        recentlyAddedTilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 7 * COVER_HEIGHT_WIDTH_RATIO * value);

        toAddTilePane.setPrefTileWidth(Main.SCREEN_WIDTH / 7 * value);
        toAddTilePane.setPrefTileHeight(Main.SCREEN_WIDTH / 7 * COVER_HEIGHT_WIDTH_RATIO * value);
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    private void initKeyShortcuts() {
        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if(searchBar.isVisible()){
                    event.consume();
                    searchBar.hide();
                }else if (drawerMenu.isSubMenuOpened()) {
                    event.consume();
                    drawerMenu.closeSubMenu(MainScene.this);
                } else {
                    event.consume();
                    drawerMenu.quitGameRoom();
                }

            }
        });
    }

    public void reloadCovers() {
        tilePane.getGameButtons().forEach(GameButton::showCover);
        toAddTilePane.getGameButtons().forEach(GameButton::showCover);
        recentlyAddedTilePane.getGameButtons().forEach(GameButton::showCover);
        lastPlayedTilePane.getGameButtons().forEach(GameButton::showCover);
        groupRowList.forEach(groupRowTilePane -> groupRowTilePane.getGameButtons().forEach(GameButton::showCover));

    }

    public void reloadLauncherLogos() {
        tilePane.getGameButtons().forEach(GameButton::setLauncherLogo);
        toAddTilePane.getGameButtons().forEach(GameButton::setLauncherLogo);
        recentlyAddedTilePane.getGameButtons().forEach(GameButton::setLauncherLogo);
        lastPlayedTilePane.getGameButtons().forEach(GameButton::setLauncherLogo);
        groupRowList.forEach(groupRowTilePane -> groupRowTilePane.getGameButtons().forEach(GameButton::setLauncherLogo));

    }

    private void checkDisplayEmptyMessagePane() {
        emptyMessagePane.setVisible(tilePane.getGameButtons().isEmpty() && toAddTilePane.getGameButtons().isEmpty());
    }

    private Pane getEmptyMessagePane() {
        VBox vbox = new VBox();
        Label noGamesLabel = new Label(getString("no_games_to_display"));

        HBox addBox = new HBox(UIValues.Constants.offsetSmall());
        Label addLabel = new Label(getString("click_to_add_game"));
        ImageView addImage = new ImageView();
        addImage.setStyle("-fx-background-color: transparent;");
        addImage.setId("main-add-button");
        addImage.setFitWidth(UIValues.Constants.offsetHuge());
        addImage.setFitHeight(UIValues.Constants.offsetHuge());
        addBox.setAlignment(Pos.CENTER);
        addBox.getChildren().addAll(addLabel, addImage);

        HBox scanBox = new HBox(UIValues.Constants.offsetSmall());
        Label scanLabel = new Label(getString("click_to_scan_games"));
        ImageView scanImage = new ImageView();
        scanImage.setStyle("-fx-background-color: transparent;");
        scanImage.setId("scan-button");
        scanImage.setFitWidth(UIValues.Constants.offsetHuge());
        scanImage.setFitHeight(UIValues.Constants.offsetHuge());
        scanBox.setAlignment(Pos.CENTER);
        scanBox.getChildren().addAll(scanLabel, scanImage);

        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(noGamesLabel, addBox, scanBox);
        vbox.setFocusTraversable(false);
        vbox.setPickOnBounds(false);

        return vbox;
    }

    public void checkFilesForEntries() {
        tilePane.getGameButtons().forEach(GameButton::checkFileExists);
        toAddTilePane.getGameButtons().forEach(GameButton::checkFileExists);
        recentlyAddedTilePane.getGameButtons().forEach(GameButton::checkFileExists);
        lastPlayedTilePane.getGameButtons().forEach(GameButton::checkFileExists);
        groupRowList.forEach(groupRowTilePane -> groupRowTilePane.getGameButtons().forEach(GameButton::checkFileExists));

    }
}
