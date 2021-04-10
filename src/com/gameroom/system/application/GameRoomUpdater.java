package com.gameroom.system.application;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

/**
 * Created by LM on 26/12/2016.
 */
public class GameRoomUpdater {

    private boolean started = true;
    private final static GameRoomUpdater updaterInstance = new GameRoomUpdater();


    private GameRoomUpdater() {
    }

    public static GameRoomUpdater getInstance() {
        return updaterInstance;
    }

    public void start() {
    }


    public boolean isStarted() {
        return started;
    }
    
    public void setSucceedPropertyListener(ChangeListener<? super EventHandler<WorkerStateEvent>> failedProperty) {
        
    }
    
    public void setFailedPropertyListener(ChangeListener<? super EventHandler<WorkerStateEvent>> failedProperty) {
        
    }

    public void setNoUpdateListener(ChangeListener<? super EventHandler<WorkerStateEvent>> noUpdateListener) {
        
    }

    public void setCancelledListener(ChangeListener<? super EventHandler<WorkerStateEvent>> cancelledListener) {
        
    }

    public void setChangeListener(ChangeListener<Number> changeListener) {
        
    }

    public void setOnUpdatePressedListener(ChangeListener<? super EventHandler<WorkerStateEvent>> onUpdatePressedListener) {
        
    }
}
