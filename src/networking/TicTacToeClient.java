package networking;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TicTacToeClient extends JFrame implements Runnable {
    //we want two clients to vs each other, this means we need 2 clients playing at the same time..
    //..this means we have to use multiThreading/concurrent programming. We implement runnable..
    //..to specify a task that can execute concurrently with other tasks, which is our client.

    //each client will run as a separate thread


    private JTextField idField; //textField to display cplayers Mark
    private JTextArea displayArea; //JTextarea to display output
    private JPanel boardPanel; //panel for tic-tac-toe board
    private JPanel panel2; //panel to hold board
    private JScrollPane pane1;
    private Square[][] board; //tic-tac-toe board
    private Square currentSquare; //current square
    private Socket connection; //connection to Server
    private Scanner input; //input from server
    private Formatter output; //output to Server
    private String ticTacToeHost; //host name for Server
    private String myMark; //this clients Mark
    private boolean myTurn; //determines which client's turn it is
    private final String X_MARK = "X"; //mark for first client
    private final String O_MARK = "O"; //mark for second client


    //set up user-interface and board
    public TicTacToeClient(String host) {

        ticTacToeHost = host; //set name of Server
        displayArea = new JTextArea(4, 30); //set up JTextArea
        displayArea.setEditable(false);
        add(new JScrollPane(displayArea), BorderLayout.SOUTH);

        boardPanel = new JPanel(); //set up panel for squares in board
        boardPanel.setLayout(new GridLayout(3, 3, 0, 0));

        board = new Square[3][3]; //board with 3 rows and 3 columns, with each cell holding a String


        //CREATES OUR BOARD

        //loop over the rows in the board
        for (int row = 0; row < board.length; row++) {
            //loop over the columns in the board
            for (int column = 0; column < board[row].length; column++) {
                //Each board has its own Square and each square has a mark, a location and a mouse listener, each square is initialized by an empty mark
                //each Square has a location from 0-8 as we have a 3x3 board. so each square will hold one of our locations in the 3x3 board.
                //so when we click a square on our 3x3 board, we know its location, and we can set it to X or O
                board[row][column] = new Square(" ", row * 3 + column);


                //we add our board to boardPanel our 3x3 GridLayout so it looks like a tic-tac-toe board
                boardPanel.add(board[row][column]); //add square to that cell
            }
        }

        idField = new JTextField(); //set up textField
        idField.setEditable(false);
        add(idField, BorderLayout.NORTH);

        panel2 = new JPanel(); //set up panel to contain boardPanel
        panel2.add(boardPanel, BorderLayout.CENTER); //add board panel
        add(panel2, BorderLayout.CENTER); //add container panel

        setSize(300, 225);  //set size of window
        setVisible(true); //show window

        startClient();
    }


    //start the client thread
    public void startClient() {
        try //connect to server and get streams
        {
            //make connection to server
            //the server address is running on the same machine as the client, so InetAddress.getByName(ticTacHoeHost) is basically localhost/127.0.0.1
            connection = new Socket(InetAddress.getByName(ticTacToeHost), 12345);

            //get streams for input and output
            input = new Scanner(connection.getInputStream());
            output = new Formatter(connection.getOutputStream());

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        //create and start worker thread for this client
        //running this client as a separate thread
        ExecutorService worker = Executors.newFixedThreadPool(1);
        worker.execute(this); //execute client
    }


    //control thread that allows continuous update of displayArea
    public void run() {

        //our server sends us the clients mark here
        myMark = input.nextLine(); //get player's mark (X or O)

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //display players mark
                idField.setText("You are player \"" + myMark + "\"");
            }
        });

        //myTurn = true if myMark = x
        myTurn = (myMark.equals(X_MARK));

        //receive messages sent to client and output them
        //infinite loop whilst the server and client has a connection, so we'll forever listen to messages from server
        while (true) {
            //this is how we as the client determine what to do, whatever the message the server sends us will allow us to decide what to do
            //we manage the "what to do" in processMessage
            if (input.hasNextLine())
                //send that as a parameter to our method processMessage
                processMessage(input.nextLine());
        }

    }

    //process messages received by client
    private void processMessage(String message) {
        if (message.equals("Valid move.")) {
            displayMessage("Valid move, please wait.\n");
            setMark(currentSquare, myMark);
        } else if (message.equals("Invalid move, try again")) {
            displayMessage(message + "\n");
            myTurn = true;
        } else if (message.equals("Opponent moved")) {
            int location = input.nextInt();
            input.nextLine();

            int row = location / 3;
            int col = location % 3;
            String opponentMark = myMark.equals("X") ? "0" : "X";

            setMark(board[row][col], opponentMark);
        } else if (message.equals("You win!")) {
            displayMessage("\n\nCongratulations! You have won!\n");
            myTurn = false;
        } else if (message.equals("You lose!")) {
            displayMessage("\n\nYou lost! Better luck next time.\n");
            myTurn = false;
        } else if (message.equals("Game is a draw.")) {
            displayMessage("\n\nGame ended in a draw.\n");
            myTurn = false;
        } else if (message.equals("Other player connected. Your move.")) {
            displayMessage("Other player connected. Your move.\n");
            myTurn = true;
        }
        else {
            displayMessage(message + "\n");
        }
    }

    //manipulate displayArea in event-dispatch thread
    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                displayArea.append(messageToDisplay); //updates output
            }
        });
    }

    private void setMark(final Square squareToMark, final String mark) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                squareToMark.setMark(mark);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkWin(String playerMark) {
        // Check rows and columns
        for (int i = 0; i < 3; i++) {
            if (board[i][0].getMark().equals(playerMark) &&
                    board[i][1].getMark().equals(playerMark) &&
                    board[i][2].getMark().equals(playerMark)) return true;

            if (board[0][i].getMark().equals(playerMark) &&
                    board[1][i].getMark().equals(playerMark) &&
                    board[2][i].getMark().equals(playerMark)) return true;
        }

        // Check diagonals
        if (board[0][0].getMark().equals(playerMark) &&
                board[1][1].getMark().equals(playerMark) &&
                board[2][2].getMark().equals(playerMark)) return true;

        if (board[0][2].getMark().equals(playerMark) &&
                board[1][1].getMark().equals(playerMark) &&
                board[2][0].getMark().equals(playerMark)) return true;

        return false;
    }

    //when its our turn and our thread isn't locked, when we click a square we sent that location to the server for processing
    //the server will then sendus back a message saying if it was valid, false etc. if valid our processMessage method updated our board with the square we clicked
    public void sendClickedSquare(int location) {
        //if it is my turn, so if it's not my turn it wont send the server the location. THIS is what stops the CLIENT from sending multiple messages to the server
        if (myTurn) {
            output.format("%d\n", location); //send location to server
            output.flush();
            myTurn = false; //not my turn any more
        }
    }

    //set current Square
    public void setCurrentSquare(Square square) {
        currentSquare = square; //set current square to argument
    }

    //private inner class for the squared on the board
    private class Square extends JPanel {
        private String mark; //mark to be drawn in this square
        private int location; //location of square

        public Square(String squareMark, int squareLocation) {
            mark = squareMark; //set mark for this square
            location = squareLocation; //set location of this square


            //when we click a Square we set our instance variable currentSquare to the square we clicked
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    setCurrentSquare(Square.this); //set Current square

                    //send location of this square
                    sendClickedSquare(getSquaredLocation());
                }
            });
        }

        //return preferred size of square
        public Dimension getPreferredSize() {
            return new Dimension(30, 30);
        }

        //return minimum size of Square
        public Dimension getMinimumSize() {
            return getPreferredSize(); //return preferred size
        }

        //set mark for Square
        public void setMark(String newMark) {
            mark =  newMark; //set mark of square
            //repaint so we can see the updated board with our mark set
            repaint();
        }

        // get mark for Square
        public String getMark() {
            return mark;
        }

        //return Square location
        public int getSquaredLocation() {
            return location; //return location of square
        }

        //draw Square
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            //this draws the square right here and we put our mark in the square
            g.drawRect(0, 0, 29, 29);
            g.drawString(mark, 11, 20); //draw mark
        }
    }


}




