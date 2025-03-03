package com.gameroom.ui;

import com.gameroom.data.game.entry.Emulator;
import com.gameroom.data.http.key.KeyChecker;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;
import com.gameroom.system.application.GameRoomUpdater;
import com.gameroom.system.application.settings.PredefinedSetting;
import com.gameroom.system.application.settings.SettingValue;
import com.gameroom.system.device.GameController;
import com.gameroom.system.device.StatsUtils;
import com.gameroom.ui.dialog.GameRoomAlert;
import com.gameroom.ui.scene.MainScene;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gameroom.system.application.settings.GeneralSettings.settings;
import static com.gameroom.system.application.settings.PredefinedSetting.SUPPORTER_KEY;

public class Main {
    public final static String ARGS_FLAG_DEV = "-dev";
    public final static String ARGS_FLAG_IGDB_KEY = "-igdb_key";
    public final static String ARGS_FLAG_SHOW = "-show";
    public final static String ARGS_START_GAME = "-start-game";

    public static boolean DEV_MODE = false;
    public final static boolean SUPPORTER_MODE = true;
    private static double TRUE_SCREEN_WIDTH;
    public static double TRUE_SCREEN_HEIGHT;

    public static double SCREEN_WIDTH;
    public static double SCREEN_HEIGHT;

    public static MainScene MAIN_SCENE;

    private static ResourceBundle RESSOURCE_BUNDLE;
    private static ResourceBundle SETTINGS_BUNDLE;
    public static ResourceBundle GAME_GENRES_BUNDLE;
    public static ResourceBundle GAME_THEMES_BUNDLE;


    private final static String MANUAL_TAG = "\\$string\\$";
    private final static char AUTO_TAG_CHAR = '%';
    private final static Pattern AUTO_TAG_PATTERN = Pattern.compile("\\" + AUTO_TAG_CHAR + "(.*)\\" + AUTO_TAG_CHAR);
    public final static String NO_STRING = "\'no_string\'";

    public static Logger LOGGER;

    public static final HashMap<String, File> FILES_MAP = new HashMap<>();

    public static Menu START_TRAY_MENU = new Menu();

    public static GameController gameController;

    public static TrayIcon TRAY_ICON;

    public static volatile boolean KEEP_THREADS_RUNNING = true;

    private static String[] calling_args;

    /*******************
     * EXECUTOR_SERVICE
     ***************************/
    private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private final static ScheduledThreadPoolExecutor SCHEDULED_EXECUTOR = new ScheduledThreadPoolExecutor(2);



    public static void main(String[] args) {
        SCHEDULED_EXECUTOR.setRemoveOnCancelPolicy(true);

        calling_args = args;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        Main.TRUE_SCREEN_WIDTH = (int) screenSize.getWidth();
        Main.TRUE_SCREEN_HEIGHT = (int) screenSize.getHeight();

        //here we bind min dimensions to 1920*1080 so that the UI is scaled correctly for lower resolution
        Main.SCREEN_WIDTH = TRUE_SCREEN_WIDTH <= 1920 ? TRUE_SCREEN_WIDTH : 1920;
        Main.SCREEN_HEIGHT = TRUE_SCREEN_HEIGHT <= 1080 ? TRUE_SCREEN_HEIGHT : 1080;


        PredefinedSetting.WINDOW_WIDTH.setDefaultValue(new SettingValue((int) TRUE_SCREEN_WIDTH, Integer.class, PredefinedSetting.WINDOW_WIDTH.getDefaultValue().getCategory()));
        PredefinedSetting.WINDOW_HEIGHT.setDefaultValue(new SettingValue((int) TRUE_SCREEN_HEIGHT, Integer.class, PredefinedSetting.WINDOW_HEIGHT.getDefaultValue().getCategory()));

        StatsUtils.printSystemInfo();
        LOGGER.info("Started app with screen true resolution : " + (int) TRUE_SCREEN_WIDTH + "x" + (int) TRUE_SCREEN_HEIGHT);

        settings(); //ensures settings are loaded
        Emulator.loadEmulators();
        
        LOGGER.info("Supporter mode : " + SUPPORTER_MODE);
        RESSOURCE_BUNDLE = ResourceBundle.getBundle("strings", settings().getLocale(PredefinedSetting.LOCALE));
        SETTINGS_BUNDLE = ResourceBundle.getBundle("settings", settings().getLocale(PredefinedSetting.LOCALE));
        GAME_GENRES_BUNDLE = ResourceBundle.getBundle("gamegenres", settings().getLocale(PredefinedSetting.LOCALE));
        GAME_THEMES_BUNDLE = ResourceBundle.getBundle("gamethemes", settings().getLocale(PredefinedSetting.LOCALE));

        //if(!DEV_MODE){
        //startUpdater();
        //}
    }

    public static String getArg(String flag, String[] args, boolean hasOption) {
        boolean argsHere = false;
        int index = 0;
        if (args != null) {
            for (String arg : args) {
                argsHere = argsHere || arg.compareToIgnoreCase(flag) == 0;
                if (!argsHere) {
                    index++;
                } else {
                    break;
                }
            }
            if (argsHere && args.length > index + 1 && hasOption) {
                String option = args[index + 1];
                if (!option.startsWith("-")) {
                    return option;
                }
                return null;
            }
            if (argsHere) {
                return "";
            }
        }
        return null;
    }

    public static void forceStop(Stage stage, String reason) {
        LOGGER.info("Stopping GameRoom because : " + reason);
        Platform.runLater(() -> {
            MAIN_SCENE.saveScrollBarVValue();
            KEEP_THREADS_RUNNING = false;
            Platform.setImplicitExit(true);
            stage.close();
            Platform.exit();
            //
        });
    }

    public static void restart(Stage stage, String reason) {
        LOGGER.info("Restarting GameRoom because : " + reason);
        try {
            Process process = new ProcessBuilder()
                    .command("GameRoom.exe")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        forceStop(stage, reason);
    }

    public static void open(Stage stage) {
        Platform.runLater(() -> {
            if (!MAIN_SCENE.getParentStage().isShowing()) {
                stage.setIconified(false);
                stage.show();
            }
            stage.toFront();
        });
    }

    public static void startUpdater() {
        GameRoomUpdater updater = GameRoomUpdater.getInstance();
        updater.setOnUpdatePressedListener((observable, oldValue, newValue) -> {
            GameRoomAlert.info(Main.getString("update_downloaded_in_background"));
        });
        GameRoomUpdater.getInstance().start();
    }

    public static String getVersion() {
        String version = "unknown";
        if (RESSOURCE_BUNDLE != null) {
            version = RESSOURCE_BUNDLE.getString(version);
        }
        if (Main.class.getPackage().getImplementationVersion() != null) {
            version = Main.class.getPackage().getImplementationVersion();
        }
        Main.LOGGER.info("App version : " + version);
        return version;
    }

    /**
     * Runs the specified {@link Runnable} on the
     * JavaFX application thread and waits for completion.
     *
     * @param action the {@link Runnable} to run
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static void runAndWait(Runnable action) {
        if (action == null)
            throw new NullPointerException("action");

        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        final CountDownLatch doneLatch = new CountDownLatch(1);

        // queue on JavaFX thread and wait for completion
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                doneLatch.countDown();
            }
        });

        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            // ignore exception
        }
    }

    public static String getString(String key, String... manuals) {
        String result = NO_STRING;
        if (RESSOURCE_BUNDLE == null) {
            return result;
        }
        try {
            result = RESSOURCE_BUNDLE.getString(key);
            Matcher tagMatcher = AUTO_TAG_PATTERN.matcher(result);

            while (tagMatcher.find()) {
                String otherKey = tagMatcher.group(1);
                String otherString = Main.getString(otherKey);
                if (!otherString.equals(NO_STRING)) {
                    result = result.replace(AUTO_TAG_CHAR + otherKey + AUTO_TAG_CHAR, otherString);
                }
            }
            for (String s : manuals) {
                result = result.replaceFirst(MANUAL_TAG, s);
            }
            return result;

        } catch (MissingResourceException e) {
            return result;
        }
    }

    public static void setRessourceBundle(ResourceBundle bundle) {
        if (bundle != null) {
            RESSOURCE_BUNDLE = bundle;
        }
    }

    public static String getSettingsString(String key) {
        if (SETTINGS_BUNDLE == null) {
            return "no_string";
        }
        try {
            return SETTINGS_BUNDLE.getString(key);

        } catch (MissingResourceException e) {
            return "no_string";
        }
    }

    public static void setSettingsBundle(ResourceBundle bundle) {
        if (bundle != null) {
            SETTINGS_BUNDLE = bundle;
        }
    }

    public static ExecutorService getExecutorService() {
        return EXECUTOR_SERVICE;
    }

    public static ScheduledThreadPoolExecutor getScheduledExecutor() {
        return SCHEDULED_EXECUTOR;
    }
}
