# Networked Tic-Tac-Toe

A multiplayer Tic-Tac-Toe application built in Java using a client-server architecture.

The application uses Java sockets to allow two clients to connect to a central server and play against each other. The user interface is built with Java Swing, while multithreading and synchronization are used by the server to manage both players concurrently.

## Video Walkthrough

A walkthrough and demonstration of the application is available on YouTube:

[Watch the Tic-Tac-Toe Application Walkthrough](https://www.youtube.com/watch?v=Nq2k1KxFM3s)

## Features

- Two-player networked Tic-Tac-Toe
- Client-server architecture
- Java socket communication
- Multithreaded server
- Thread synchronization using `Lock` and `Condition`
- Java Swing graphical user interface
- Server-side move validation
- Win and draw detection
- Turn-based player synchronization

## Technologies

- Java
- Java Swing
- Java Sockets
- Java Concurrency
- `ExecutorService`
- `Lock` and `Condition`

## Project Structure

```text
java-networked-tictactoe/
├── src/
│   └── networking/
│       ├── TicTacToeClient.java
│       ├── TicTacToeServer.java
│       ├── TicTacToeServerClientTest.java
│       ├── TicTacToeServerTest.java
│       └── TicTaxToeClientServerTest2.java
├── .gitignore
└── README.md
```

## Prerequisites

To run the application, you need:

- Java Development Kit (JDK)
- A Java-compatible IDE or editor such as Visual Studio Code or IntelliJ IDEA

You can check that Java is installed by running:

```bash
java -version
javac -version
```

## Running the Application

The project contains one server and two client launcher classes.

All three applications should be running at the same time.

### Using Visual Studio Code

For the easiest setup, install the **Extension Pack for Java** in Visual Studio Code.

Open the root project folder in Visual Studio Code:

```text
java-networked-tictactoe
```

Java files containing a `main` method should display **Run | Debug** above the method.

### 1. Start the Server

Open:

```text
src/networking/TicTacToeServerTest.java
```

Click **Run** above the `main` method.

A Swing server window will open and display:

```text
Server awaiting Connections
```

Leave the server running.

The server listens for client connections on port `12345`.

### 2. Start Player 1

Open:

```text
src/networking/TicTacToeServerClientTest.java
```

Click **Run** above the `main` method.

A Tic-Tac-Toe client window will open and connect to the server using:

```text
127.0.0.1
```

The first client becomes Player X.

Leave this client running.

### 3. Start Player 2

Open:

```text
src/networking/TicTaxToeClientServerTest2.java
```

Click **Run** above the `main` method.

A second client window will open and connect to the same server.

Once both clients have connected, the game can begin.

### 4. Play

Player X moves first.

Players take turns clicking squares in their respective client windows. Each move is sent to the server, which validates the move, updates the game state, and informs the other client.

Keep all three windows running while playing:

```text
                 Server
                   |
             Port 12345
              /         \
             /           \
       Player X         Player 0
       Client 1         Client 2
```

## Network Configuration

By default, both clients connect to:

```text
127.0.0.1
```

This means the server and both clients are expected to run on the same computer.

The server listens on:

```text
Port 12345
```

The client launcher classes can also accept a server address as a command-line argument, allowing the client to connect to a different host when appropriate network access is available.

## Stopping and Restarting

Close the two client applications and the server before starting a new game.

When restarting, launch the applications in this order:

```text
1. TicTacToeServerTest
2. TicTacToeServerClientTest
3. TicTaxToeClientServerTest2
```

Only one instance of the server should be running on port `12345` at a time.

## Concepts Demonstrated

This project demonstrates several Java programming concepts, including:

- Object-oriented programming
- Client-server networking
- TCP socket communication
- Multithreading
- Concurrent programming
- Thread synchronization
- GUI development with Swing
- Event-driven programming
- Shared game-state management

## Author

Jerry Jefferson