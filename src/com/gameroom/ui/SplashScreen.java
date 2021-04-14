package com.gameroom.ui;

import java.awt.Color;
import java.io.InputStream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class SplashScreen implements Runnable {
	private static final int TIME = 20;
    private ImageIcon image;
    
    private JFrame frame;
    private JLabel label;
    private JProgressBar progressBar;
    
    private int targetValue;
    private String message;
    private Thread progressRunner;
    
    public SplashScreen()
    {
		targetValue = 0;
		message = "Starting...";
        createGUI();
        addProgressBar();
        progressRunner = new Thread(this);
		progressRunner.start();
    }
    private void createGUI(){
    	image = new ImageIcon(ClassLoader.getSystemResource("res/defaultImages/splashScreen.jpg"));
    	label = new JLabel(image);
        label.setSize(image.getIconWidth(), image.getIconHeight());
    	
        frame=new JFrame();
        frame.getContentPane().setLayout(null);
        frame.setUndecorated(true);
        frame.setSize(image.getIconWidth(), image.getIconHeight());
        frame.setLocationRelativeTo(null);
        frame.add(label);
        frame.setVisible(true);
    }
    
    private void addProgressBar(){
    	progressBar=new JProgressBar();
    	int width = image.getIconWidth()*2/3;
    	int height = 20;
    	int x = image.getIconWidth()/6;
    	int y = image.getIconHeight()-height*2;
        progressBar.setBounds(x, y, width, height);
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(true);
        progressBar.setBackground(Color.DARK_GRAY);
        progressBar.setForeground(Color.GRAY);
        progressBar.setValue(0);
        frame.add(progressBar);
    }
    
    public synchronized void progressUpdate(int newValue, String newMessage) {
    	message = newMessage;
    	targetValue = newValue;
    	if(!progressRunner.isAlive()) {
            progressRunner = new Thread(this);
    		progressRunner.start();
    	}    		
    }
    
	@Override
	public void run() {
    	for(int i = progressBar.getValue(); i <= targetValue; i++)
        {
            try{
                Thread.sleep(TIME);
                progressBar.setValue(i);
                progressBar.setString(i + "%: " + message);
                if (i >= 100) {
                	frame.dispose();
                	return;
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
	}

	public static void main(String[] args) {
		SplashScreen d = new SplashScreen();
		d.progressUpdate(100, "ne");
	}

}

