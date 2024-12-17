package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import static Utils.ResourceLoader.loadProperties;

public class QQLoginUI extends JFrame {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JButton loginButton;
	private JButton registerButton;

	private String IP;
	private int Port;
	// cloud: "104.198.172.29", 60001

	public QQLoginUI() {
		// TODO: change to the server's IP address and port in config.properties
		Properties properties = loadProperties();
		IP = properties.getProperty("server.ip");
		Port = Integer.parseInt(properties.getProperty("server.port"));

		// Set up the frame
		setTitle("Chatapp");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(450, 400);
		this.setResizable(false);

		setLayout(new GridBagLayout());
		getContentPane().setBackground(new Color(240, 242, 245));

		initComponents();

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initComponents() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Logo or App Name
		JLabel titleLabel = new JLabel("Let's Chat", SwingConstants.CENTER);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
		titleLabel.setForeground(new Color(24, 119, 242));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		add(titleLabel, gbc);

		// Username Label and Field
		JLabel usernameLabel = new JLabel("Username:");
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		add(usernameLabel, gbc);

		usernameField = new JTextField(20);
		usernameField.setBorder(BorderFactory.createCompoundBorder(
				usernameField.getBorder(),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)
		));
		gbc.gridx = 1;
		add(usernameField, gbc);

		// Password Label and Field
		JLabel passwordLabel = new JLabel("Password:");
		gbc.gridx = 0;
		gbc.gridy = 2;
		add(passwordLabel, gbc);

		passwordField = new JPasswordField(20);
		passwordField.setBorder(BorderFactory.createCompoundBorder(
				passwordField.getBorder(),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)
		));
		gbc.gridx = 1;
		add(passwordField, gbc);

		// Login Button
		loginButton = new JButton("Login");
		loginButton.setBackground(new Color(24, 119, 242));
		loginButton.setForeground(Color.WHITE);
		loginButton.setFocusPainted(false);
		loginButton.addActionListener(this::loginAction);
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		add(loginButton, gbc);

		// Register Button
		registerButton = new JButton("Register");
		registerButton.setBackground(new Color(66, 103, 178));
		registerButton.setForeground(Color.WHITE);
		registerButton.setFocusPainted(false);
		registerButton.addActionListener(this::registerAction);
		gbc.gridx = 0;
		gbc.gridy = 4;
		add(registerButton, gbc);
	}

	private void loginAction(ActionEvent e) {
		String username = usernameField.getText();
		String password = new String(passwordField.getPassword());

		if (username.isEmpty() || password.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"Please enter both username and password",
					"Login Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		new LoginWorker(username,password).execute();
	}

	private class LoginWorker extends SwingWorker<Boolean, Void> {
		private final String username;
		private final String password;

		public LoginWorker(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		protected Boolean doInBackground() throws Exception {
			// login verification logic
			AuthClient guest = new AuthClient(IP, Port, QQLoginUI.this);
			int verify = guest.GuestSpeakAndVerify("Login " + username + " " + password);
            return verify == 1;
        }

		@Override
		protected void done() {
			try {
				boolean loginSuccess = get();
				if (loginSuccess) {
					QQLoginUI.this.dispose();
					new ChatroomUI(username, IP, Port).setVisible(true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void registerAction(ActionEvent e) {
		String username = usernameField.getText();
		String password = new String(passwordField.getPassword());

		if (username.isEmpty() || password.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"Please enter both username and password",
					"Registration Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		// registration logic
		new RegisterWorker(username, password).execute();
	}

	private class RegisterWorker extends SwingWorker<Boolean, Void> {
		private final String username;
		private final String password;

		public RegisterWorker(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		protected Boolean doInBackground() throws Exception {
			// login verification logic
			AuthClient guest = new AuthClient(IP, Port, QQLoginUI.this);
			int verify = guest.GuestSpeakAndVerify("Register " + username + " " + password);
			return verify == 3;
		}

		@Override
		protected void done() {
			try {
				boolean Success = get();
				if (Success) {
					usernameField.setText("");
					passwordField.setText("");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void showDialog(String s) {
		JOptionPane.showMessageDialog(this, s);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(QQLoginUI::new);
	}
}