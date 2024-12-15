package client;

//package com.ts.x.swing;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.Properties;

import static Utils.ResourceLoader.loadImage;

import javax.swing.*;


public class QQ extends JFrame{

	private String IP;
	private int port;
	// cloud: "104.198.172.29", 60001

	private static final long serialVersionUID = -6788045638380819221L;
	private JTextField ulName;
	private JPasswordField ulPasswd;

	private JLabel j1;
	private JLabel j2;
	private JLabel j3;
	private JLabel j4;

	private JButton b1;
	private JButton b2;
	private JButton b3;

	private JCheckBox c1;
	private JCheckBox c2;

	private JComboBox<String> cb1;


	public QQ(){
		// Load the server IP and port from config.properties
		loadProperty();

		this.setTitle("QQ");
		this.getRootPane().setWindowDecorationStyle(JRootPane.NONE);

		init(this);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);

		// absolute positioning
		this.setLayout(null);
		this.setBounds(0, 0, 355, 265);
		this.setResizable(false);

//		Image img0 = new ImageIcon("D:/logo.png").getImage();//TODO: not found this logo
//		this.setIconImage(img0);

		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	private void loadProperty(){
		Properties properties = new Properties();
		try {
			// TODO: change to the server's IP address and port in config.properties
			properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
			IP = properties.getProperty("server.ip");
			port = Integer.parseInt(properties.getProperty("server.port"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void init(QQ window){
		ClientUI guest = new ClientUI();
		//121.40.143.239
		guest.createClient("Guest", IP, port, false); //TODO: now in local
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Container container = this.getContentPane();
		j1 = new JLabel();

		// set the background image
		j1.setIcon(new ImageIcon(loadImage("images/bgimg.png")));
		j1.setBounds(0, 0, 355, 265);

		// set the profile photo
		j2 = new JLabel();
		j2.setIcon(new ImageIcon(loadImage("images/hdimg.png")));
		j2.setBounds(40, 65, 50, 53);

		ulName = new JTextField();
		ulName.setBounds(165, 70, 150, 20);

		j3 = new JLabel("Username:");
		j3.setBounds(100, 70, 65, 20);

		ulPasswd = new JPasswordField("");
		ulPasswd.setBounds(165, 100, 150, 20);

		j4= new JLabel("Password:");
		j4.setBounds(100, 100, 65, 20);

		b1 = new JButton("Login");
		b1.setForeground(Color.BLACK);
		b1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b1.setBounds(100, 130, 150, 20);
		b1.addActionListener(e -> {
			String username = ulName.getText();
			String userpassword = String.valueOf( ulPasswd.getPassword());

			if (username.isEmpty() || userpassword.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Username and Password cannot be empty");
				return;
			}

			int verify = guest.GuestSpeakAndVerify("Login"+' '+username+ ' '+ userpassword);
			if(verify == 1){
//				JOptionPane.showMessageDialog(this, "Login successful");
				guest.shutDownClient();
				window.setVisible(false);
//						guest.createClient(username, "104.198.172.29", 60001);
				guest.createClient(username, IP, port, true); //TODO: now in local
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ee) {
					ee.printStackTrace();
				}
			} else {
//				JOptionPane.showMessageDialog(this, "Login failed");
			}
		});

		b2 = new JButton("Register");
		b2.setForeground(Color.BLACK);
		b2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b2.setBounds(100, 160, 150, 20);
		b2.addActionListener(e -> {
			String username = ulName.getText();
			String userpassword = String.valueOf( ulPasswd.getPassword());
			try {
				Thread.sleep(3000);
			} catch (InterruptedException ee) {
				ee.printStackTrace();
			}
			guest.GuestSpeakAndVerify("Register"+' '+username+ ' '+ userpassword);
		});

		j1.add(j2);
		j1.add(j3);
		j1.add(j4);

		j1.add(b1);
		j1.add(b2);

		container.add(j1);
		container.add(ulName);
		container.add(ulPasswd);
	}

	public static void main(String[] args) {
		new QQ();
	}
}