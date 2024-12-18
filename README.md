# Project: Chat App 

 **Developed By**: Jiaxin Sun, Xibo He



## 📖 About the Project

The `Chatapp` project is a real-time chatroom application based on a **Client-Server (C/S) architecture**, capable of running on local systems or cloud servers. This document will show it running locally.

This project leverages **multi-threads,  networking, database, GUI**, and **cryptography**.

#### Key Technologies and Their Usage

1. **Multi-threading**

   - The server uses multiple threads to handle concurrent communication from multiple clients simultaneously.

   - Each client connection is managed by a separate thread and file transfer uses additional threads, enabling real-time chat and file upload/download without blocking other operations.

2. **Networking**

   - The project utilizes **Java Sockets** for communication between the client and server.

   - The server listens for incoming connections, while clients connect to the server to send and receive messages, files, and other requests.

3. **Database Management**
   - Both **MySQL** and **SQLite** databases are supported for persistent data storage.
   
   - User information, chat messages, and file attachment information are stored in the database.
   
4. **GUI**

   - The user interface is implemented using **Java Swing**.

   - Features:
     - Login Screen (`LoginUI.java`)
     - Chatroom Interface (`ChatroomUI.java`) with support for text, file sharing, and audio recording.

5. **Cryptography and Security**

   - **TLS**: All data transmitted between the client and server is encrypted using **TLS** with keystore and truststore certificates to ensure secure communication.

   - **Password Encryption**: User passwords are hashed and securely stored using **bcrypt**.

6. **Audio APIs** (Additional)

   - The project integrates **Java Audio APIs** to enable voice message functionality.

   - Clients can record and play back audio messages.



## 🛠️How to run

#### Project Structure

The project uses **Maven** for build and dependency management. 

```
Chatapp/
│-- src/
│   ├── main/                    
│   │   ├── java/                
│   │   │   ├── client/          # Client-side code
│   │   │   │   ├── AudioPlayer.java       	# Handles audio playback
│   │   │   │   ├── AudioRecord.java       	# Handles audio recording
│   │   │   │   ├── AuthClient.java        	# Client-side authentication logic
│   │   │   │   ├── ChatroomUI.java        	# User Interface for the chatroom
│   │   │   │   ├── Client.java            	# Main client implementation
│   │   │   │   ├── LoginUI.java           	# User Interface for login screen
│   │   │   │   └── Pcm2Wav.java           	# Utility for PCM to WAV audio conversion
│   │   │   ├── Server/          # Server-side code
│   │   │   │   ├── ChatRoom.java          	# Core chatroom logic
│   │   │   │   ├── DatabaseConnection.java# Database connection utilities
│   │   │   │   ├── MsgTask.java           	# Handles message tasks
│   │   │   │   └── Server.java            	# Main server implementation
│   │   │   └── Utils/          # Utility classes
│   │   │       └── ResourceLoader.java    	# Loads external resources
│   │   └── resources/         
│   │       ├── db/             # Database-related files or scripts
│   │       │   ├── chatapp.db          	# Sqlite database
│   │       │   └── database.sql    		# Initial SQL
│   │       └── config.properties  # Configuration file
│   └── test/                   # Test files
│       └── java/              
│           ├── client_run.java # Test entry point for the client
│           ├── local_test.java # Local environment testing
│           └── server_run.java # Test entry point for the server
│-- target/                     # Compiled output 
│-- client.truststore           # Client-side SSL/TLS truststore
│-- pom.xml                     # Maven project configuration
│-- server.crt                  # Server-side SSL/TLS certificate
└── server.keystore             # Server-side SSL/TLS keystore
```

#### 📥 Setup Instructions

1.**Dependencies**:

- Sync dependencies using **Maven** (`pom.xml`).

2.**Audio Support**:

- Ensure the **microphone** and playback devices are accessible to the application.

3.**Database Configuration**:

- By default, **SQLite** is used for testing (see `resources/db/chatapp.db`).

- To use **MySQL**, modify the `isMySQL` flag in `DatabaseConnection.java` and run `main` to initialize database.

4.**Configuration**:

- Update `config.properties` to customize network ports, database settings, and resource paths.

#### **⚠️**Notes:

- If making changes, ensure the `config.properties` file is properly configured for smooth operation.
- Communication between the client and server is secured using SSL/TLS certificates (`.crt` and `.keystore`). Update certificates if expired.

#### 🛠️ RUN

1.**Run Tests**:

- Execute `local_test.java` in the `src/test/java/` folder to launch the server and client locally.

2.**Register Users**:

- Register two users via the Login Screen to chat.

![image-20241216235340373](/img/image-20241216235340373.png)

3.**Main UI Features**:

- **Send Messages**: Text-based chat.
- **File Sharing**: Upload/download files.
- **Voice Recording**: Record and play audio messages.
- **Links**: Clickable links for file downloads and voice playback.

![image-20241216235619456](/img/image-20241216235619456.png)
