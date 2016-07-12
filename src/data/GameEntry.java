package data;

import UI.Main;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

/**
 * Created by LM on 02/07/2016.
 */
public class GameEntry {
    private final static File[] DEFAULT_IMAGES_PATHS = {new File("res/defaultImages/cover.jpg")};
    private final static int IMAGES_NUMBER = 3;

    private boolean savedLocaly = false;

    private String name = "";
    private String year = "";
    private String description = "";
    private String editor = "";
    private String path = "";
    private UUID uuid;
    private File[] imagesPaths = new File[IMAGES_NUMBER];
    private long playTime = 0; //Time in seconds

    public GameEntry(String name) {
        uuid = UUID.randomUUID();
        this.name = name;
    }

    public GameEntry(UUID uuid) {
        this.uuid = uuid;
        try {
            loadEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File propertyFile() throws IOException {
        File dir = new File(uuid.toString());
        if (!dir.exists()) {
            dir.mkdir();
        }
        File configFile = new File(uuid.toString() + File.separator + "entry.properties");
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
        return configFile;
    }

    private void saveEntry() {
        if (savedLocaly) {
            Properties prop = new Properties();
            OutputStream output = null;
            try {
                output = new FileOutputStream(propertyFile());

                // set the properties value
                prop.setProperty("name", name);
                prop.setProperty("year", year);
                prop.setProperty("description", description);
                prop.setProperty("editor", editor);
                prop.setProperty("path", path);

                for (int i = 0; i < IMAGES_NUMBER; i++) {
                    if (imagesPaths[i] != null) {
                        prop.setProperty("image" + i, imagesPaths[i].getPath());
                    } else {
                        break;
                    }
                }
                prop.setProperty("playTime", Long.toString(playTime));
                // save properties to project root folder
                prop.store(output, null);
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadEntry() throws IOException {
        Properties prop = new Properties();
        InputStream input = null;

        input = new FileInputStream(propertyFile());

        // load a properties file
        prop.load(input);

        if (prop.getProperty("name") != null) {
            name = prop.getProperty("name");
        }
        if (prop.getProperty("year") != null) {
            year = prop.getProperty("year");
        }
        if (prop.getProperty("description") != null) {
            description = prop.getProperty("description");
        }
        if (prop.getProperty("editor") != null) {
            editor = prop.getProperty("editor");
        }
        if (prop.getProperty("path") != null) {
            path = prop.getProperty("path");
        }
        if (prop.getProperty("playTime") != null) {
            playTime = Long.parseLong(prop.getProperty("playTime"));
        }

        for (int i = 0; i < IMAGES_NUMBER; i++) {
            if (prop.getProperty("image" + i) != null) {
                imagesPaths[i] = new File(prop.getProperty("image" + i));
            } else {
                break;
            }
        }

        input.close();

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        saveEntry();
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
        saveEntry();
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
        saveEntry();
    }

    public UUID getUuid() {
        return uuid;
    }

    public Image getImage(int index, double width, double height, boolean preserveRatio, boolean smooth){
        File currFile = getImagePath(index);
        if(DEFAULT_IMAGES_PATHS.length > index && currFile.equals(DEFAULT_IMAGES_PATHS[index])){
            return new Image(currFile.getPath(), width,height,preserveRatio,smooth);
        }else{
            return new Image("file:" + File.separator + File.separator + File.separator +  currFile.getAbsolutePath(), width,height,preserveRatio,smooth);
        }
    }

    /**
     * Should not be used to create a new imageView, use getImage instead
     * @param index
     * @return
     */
    public File getImagePath(int index) {
        if (index < imagesPaths.length) {
            File result = imagesPaths[index];
            if (result == null) {
                return DEFAULT_IMAGES_PATHS[index];
            }
            return result;
        }
        return DEFAULT_IMAGES_PATHS[0];
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        saveEntry();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        saveEntry();
    }

    public void setImagePath(int index, File imagePath) {
        if (imagesPaths.length > index) {
            imagesPaths[index] = imagePath;
        }
        saveEntry();
    }
    public long getPlayTimeSeconds() {
        return playTime;
    }
    public void setPlayTimeSeconds(long seconds){
        this.playTime = seconds;
        saveEntry();
    }
    public void addPlayTimeSeconds(long seconds){
        this.playTime+=seconds;
        saveEntry();
    }
    public String getPlayTimeFormatted(boolean fullFormat){
        String result  = "";
        long seconds=playTime, minutes=0,hours=0;

        if(seconds > 60){
            minutes = seconds /60;
            seconds = (long)(seconds/60.0 - seconds/60)*60;

            if(minutes > 60){
                hours = minutes/60;
                minutes = (long)(minutes/60.0 - minutes/60)*60;
            }
        }
        if(fullFormat){
            result = hours + "h" + minutes + "m" + seconds + "s";
        }else{
            if(hours>0){
                result = hours + "h";
            }else if(minutes > 0){
                result = minutes + "m";
            }else{
                result = seconds + "s";
            }
        }
        return result;

    }

    public void setSavedLocaly(boolean savedLocaly) {
        this.savedLocaly = savedLocaly;
        if(savedLocaly) {
            try {
                Main.ALL_GAMES_ENTRIES.appendEntry(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        saveEntry();
    }

    public void deleteFiles() {
        File file = new File(getUuid().toString());
        String[] entries = file.list();
        if(entries!=null) {
            for (String s : entries) {
                File currentFile = new File(file.getAbsolutePath(), s);
                currentFile.delete();
            }
            file.delete();
            try {
                Main.ALL_GAMES_ENTRIES.removeEntry(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public String getProcessName(){
        String name = "";
        for(int i = path.length()-1; i>=0; i--){
            char c = path.charAt(i);
            if(c=='\\' ||c == '/'){
                break;
            }else{
                name = c+name;
            }
        }
        return name;
    }

}
