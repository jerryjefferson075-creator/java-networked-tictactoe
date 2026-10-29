package networking;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TicTacToeServer extends JFrame {

    //Altogether We'll have four 5 threads when everything is connected..
    //Main Thread which sets up the connections
    //Thread 1 (Manages Client 1s connection and game state)
    //Thread 2 (Manages client 2s connection and game state)
    //Client Thread 1 (This is run separately on the client machines, interacting with Thread 1)
    //Client Thread 2 (This is run separately on the client machine, interacting with Thread 2)
    //Synchronization happens between Thread 1 and Thread 2 to ensure the game progresses turn by turn.This is done using Lock and Condition



    private String[] board = new String[9];  //tic-tac-toe board
    private JTextArea outputArea; //for outputting moves
    private Player[] players; //array of players
    private ServerSocket server; //server socket to connect with Clients
    private int currentPlayer; //keep tracker of player with current move
    private final static int PLAYER_X = 0; //constant for first player
    private final static int PLAYER_0 = 1; //constant for second player
    private final static String[] MARKS = {"X","0"}; //array of Marks
    private ExecutorService runGame; //will run players. As we know, we use ExecuorService to run our Runnables(these are
    //(.. our tasks that we want to run concurrently.
    private Lock gameLock; //to lock game for synchronization
    private Condition otherPlayerConnected; //to wait for other player
    private Condition otherPlayerTurn; //to wait for other players turn
    private volatile boolean gameOver = false;


    //set up tic-tac-toe server and GUI that displays messages
    public TicTacToeServer()
    {
        super("Tic-Tac-Toe-Server"); //set title of window


        //create ExecutorService with a thread for each player. This is how we create our threads
        runGame = Executors.newFixedThreadPool(2); //remember this specifies how many threads can run simultaneously at the same time we only need 2
        gameLock = new ReentrantLock(); //create lock for game so we can create conditions for each thread. One go, the other stop. The other go, one stop.

        //condition variable for both players being connected. We'll use this to alert both Thread 1 and Thread 2 that both players have connected, so game can start
        otherPlayerConnected = gameLock.newCondition();


        //condition variable for the other player's turn, We'll use this to alert each Thread when it's their go and when one Thread needs to be locked allowing the other to go.
        otherPlayerTurn = gameLock.newCondition();


        //Our client has the actual board that is also from 0-8. We use this to keep up to date with each client moves. If client1 tells us that its moved at location 2,
        //us as the server, Thread1 can communicate with client 2 basically saying that client1 moved at location 2 so update your board client2 that location 2 has..
        //..now been taken and is unavailable.
        for (int i = 0; i < 9; i++) {
            board[i] = new String(""); //create tic-tac-toe board with nothing in the spaces at the moment
        }

        //Each player will represent a Thread and client communication
            players = new Player[2]; //puts 2 Player objects in our players array


        //to store the currentPlayer,
            currentPlayer = PLAYER_X; //set current player to first player

        try
        {
            //we create our server so Client can connect to this
            //backlog represents how many clients are waiting to be accepted by serverSocket.accept();
            //Main Thread creates a server so clients can connect
            server = new ServerSocket(12345,2); //set up ServerSocket
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            System.exit(1);
        }

        outputArea = new JTextArea(); //create JTextArea for output

        add(outputArea, BorderLayout.CENTER);
        outputArea.setText("Server awaiting Connections\n");

        setSize(300,300); //set size of window
        setVisible(true); //show window

    } //end of constructor


    //wait for two connections so game can be played
    public void execute()
    {
        //wait for each client to connect
        for (int i = 0; i < players.length; i++) {
            try  //wait for connection, create player, start Runnable
            {

                //Main Thread will pause until a client connects
                //Once a client connects, the main thread hands off client1 socket to a new Player Thread which is Thread 1, allowing for Thread1 to communicate with client1
                //Main Thread goes back to waiting for the next connection
                //Once second client connects, the main thread hands off client2 socket to a new Player Thread which is Thread 2, allowing for Thread2 to communicate with client2

                players[i] = new Player(server.accept(), i);
                //run the client threads concurrently
                runGame.execute(players[i]); //this will execute a separate thread for the client, so it will go straight to the "run" method in class Player. when a client connects
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
                System.exit(1);
            }
        }

        //both clients have connected to the server


        gameLock.lock(); //Once both clients connect we lock


        try
        {
            //we currently have Thread1 waiting on a condition to check if the other player has connected, since they have we let them know and then unlock our Main thread
            players[PLAYER_X].setSuspended(false); //set suspended for player x false (this is our conditions guarded wait so the thread doesn't wake up unexpectedly.)
            otherPlayerConnected.signal(); //wake up Thread1 Player X
        }

        finally {
            //main thread is done, we now begin player X execution from the point where it was waiting otherPlayerConnected.await();
            gameLock.unlock();
        }
    }

    // Add to TicTacToeServer class
    private boolean checkWin(String mark) {
        // Rows and columns
        for (int i = 0; i < 3; i++) {
            if (board[i*3].equals(mark) &&
                    board[i*3+1].equals(mark) &&
                    board[i*3+2].equals(mark)) return true;
            if (board[i].equals(mark) &&
                    board[i+3].equals(mark) &&
                    board[i+6].equals(mark)) return true;
        }

        // Diagonals
        if (board[0].equals(mark) &&
                board[4].equals(mark) &&
                board[8].equals(mark)) return true;
        if (board[2].equals(mark) &&
                board[4].equals(mark) &&
                board[6].equals(mark)) return true;

        return false;
    }

    private boolean isBoardFull() {
        for (String s : board)
            if (s.equals("")) return false;
        return true;
    }

    //display message in outputArea
    private void displayMessage(final String messageToDisplay)
    {
        //display message from event-dispatch thread of execution
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {  //updates outPutArea
                        outputArea.append(messageToDisplay);
                    }
                }
        );
    }


    public boolean validateAndMove(int location, int player) {
        // Wait until it's the player's turn
        while (player != currentPlayer) {
            gameLock.lock();
            try {
                otherPlayerTurn.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                gameLock.unlock();
            }
        }

        if (!isOccupied(location)) {
            board[location] = MARKS[currentPlayer]; // Make move

            String currentMark = MARKS[currentPlayer];
            Player current = players[currentPlayer];
            Player opponent = players[(currentPlayer + 1) % 2];

            // Notify opponent of move
            opponent.otherPlayerMoved(location);

            // Check win/draw
            boolean won = checkWin(currentMark);
            boolean draw = isBoardFull();

            if (won) {
                current.outputMessage("Valid move.");
                current.outputMessage("You win!");
                opponent.outputMessage("You lose!");
                setGameOver();
            } else if (draw) {
                current.outputMessage("Valid move.");
                opponent.outputMessage("Opponent moved");
                opponent.outputMessage(location + "");
                current.outputMessage("Game is a draw.");
                opponent.outputMessage("Game is a draw.");
                setGameOver();
            } else {
                // Continue game
                current.outputMessage("Valid move.");
                currentPlayer = (currentPlayer + 1) % 2;

                gameLock.lock();
                try {
                    otherPlayerTurn.signal(); // Let the other player move
                } finally {
                    gameLock.unlock();
                }
            }

            return true;
        } else {
            return false; // Invalid move
        }
    }



    //determine whether location is occupied
    public boolean isOccupied(int location)
    {
        if(board[location].equals(MARKS[PLAYER_X]) || board[location].equals(MARKS[PLAYER_0]))
        {
            return true; //location is not occupied
        }
        return false;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private void setGameOver() {
        gameOver = true;
    }

    //each Player is a thread that can run concurrently
    private class Player implements Runnable
    {
        private Socket connection; //connection to client
        private Scanner input; //input from Client
        private Formatter output; //output to client
        private int playerNumber; //tracks which player this is
        private String mark; //mark for this player
        private boolean suspended = true; //whether thread is suspended


        //This will represent Thread1 - Client1 communication
        //and Thread2 - Client2 communication.
        public Player(Socket socket, int number)
        {
            playerNumber = number; //store this players number
            mark = MARKS[playerNumber]; //specify player's mark
            connection = socket; //this is what establishes connected between client/server so we use this to get info from client. this is what we'll use to receive info from the client and send..
            //..info the client.

            try
            {
                //we get the clients information here in an input stream
                input = new Scanner(connection.getInputStream());

                //we send the client information using the outputStream
                output = new Formatter(connection.getOutputStream());

                connection.setSoTimeout(45000);  //timeout after 45 seconds of inactivity
            }

            catch (IOException ioException)
            {
                ioException.printStackTrace();
                System.exit(1);
            }

        }

        public void outputMessage(String message) {
            output.format("%s\n", message);
            output.flush();
        }

        //send message that other player moved
        public void otherPlayerMoved(int location)
        {
            //send message to client
            output.format("Opponent moved\n");
            output.format("%d\n", location); //send location of move to client so they can mark it on their board
            //flush so it doesn't wait in the buffer
            output.flush();
        }

        //control threads execution
        //this is called automatically when a thread starts,
        //this controls the info that's sent and received from client to our server here
        public void run()
        {
            //send client its mark(X or 0), process messages from Client
            try
            {
                displayMessage("Player " + mark + " connected\n");

                output.format("%s\n",mark); //send player's mark so either "X" or "O"
                output.flush(); //force it to the client so it receives it now

                //if player X, wait for another player to arrive
                if(playerNumber == PLAYER_X)
                {
                    output.format("%s\n%s", "Player X connected", "Waiting for another player\n");
                    output.flush(); //flush output

                    gameLock.lock(); //lock thread on client 1 player "X" (WE ALWAYS LOCK THE CURRENT THREAD BEFORE A CONDITION, as it can lead to race conditions etc..)

                    try
                    {
                        //lock conditions can sometimes wake up unexpectedly, even if no thread explicitly signals them, this is called a spurious wakeup. So what we do here is
                        // use a 'guarded wait'..
                        //..ensuring that even if a spurious wakeup does happen for that thread it won't affect our game.. suspended is still true. we set suspended to false when
                        // we intentionally..
                        //..want to wake up this thread again.
                        while (suspended)
                        {
                            otherPlayerConnected.await(); //as client1 were pausing here and waiting on otherPlayerConnected.signify() or signifyAll() to be called to tell us the
                            // other player has
                            //connected, then we can resume execution.
                            //when client2 calls signifyAll or signify then this client1 will wake up again and resume execution. so it will go from after the finally block
                        }
                    }
                    catch (InterruptedException exception)
                    {
                        exception.printStackTrace();
                    }
                    finally {
                        gameLock.unlock(); //go back to main thread waiting for player 2 to connect
                    }

                    //everytime we call otherPlayerConnected.signal() it will start from this point
                    //WERE HERE
                    //send message that other player connected
                    output.format("Other player connected. Your move.\n");
                    output.flush();
                }
                else
                //when the second client connects
                {
                    output.format("Player 0 connected, please wait\n");
                    output.flush(); //flush output
                }


                //once both clients connect this happens
                //while game not over
                //this is basically what keeps the game running
                //while this keeps returning false
                while (!isGameOver())
                {

                    //after each player has their turn, it will restart from here for the new player

                    int location = 0; //initialize move location

                    //the program maintains board locations as numbers from 0 to 8 (0 - 2 for the first row, 3 - 5 for the second row and 6 - 8 for the third row)


                    //get the clients move location by using our inputStream
                    if(input.hasNext()) //when the client clicks a spot on the board it sends that spot to us the server so we check the input stream if anything has been sent
                        location = input.nextInt(); //get move location.. we set location to whatever client sent


                    //check for valid move
                    if(validateAndMove(location,playerNumber))
                    {
                        displayMessage("\nlocation: " + location); //displays the location to our main server application
                    }
                    else
                    {
                        output.format("Invalid move, try again\n");
                        output.flush();
                    }
                }
            }

            finally {

                try {
                    //when the game is over it will close the connection
                    connection.close(); //close connection to client
                }

                catch (IOException ioException)
                {
                    ioException.printStackTrace();
                    System.exit(1);
                }
            }
        }

        //set whether or not thread is suspended
        public void setSuspended(boolean status)
        {
            suspended = status; //set value of suspended
        }

    }

}
