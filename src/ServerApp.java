import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class ServerApp {
    private static final int PORT = 5050;
    private static final String USERNAME = "dm";
    private static final String PASSWORD = "1234";

    private static final Color BG = new Color(15, 23, 42);
    private static final Color PANEL = new Color(30, 41, 59);
    private static final Color PANEL_DARK = new Color(2, 6, 23);
    private static final Color TEXT = new Color(226, 232, 240);
    private static final Color MUTED = new Color(148, 163, 184);
    private static final Color ACCENT = new Color(14, 165, 233);

    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font LOG_FONT = new Font("Consolas", Font.PLAIN, 13);
    private static JTextArea logArea;
    private static JLabel stateLabel;
    private static JLabel turnLabel;
    private static JLabel roundLabel;

    private static String serverState = "EXPLORATION";
    private static int currentTurn = 1;
    private static int round = 1;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerApp::createUI);
        new Thread(ServerApp::startServer).start();
    }

    private static void createUI() {
        JFrame frame = new JFrame("DND Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 550);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        frame.setContentPane(root);

        // TOP PANEL (STATE)
        JPanel top = new JPanel(new GridLayout(1, 3, 12, 12));
        top.setBackground(PANEL);
        top.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        stateLabel = new JLabel("State: " + serverState, SwingConstants.CENTER);
        turnLabel = new JLabel("Turn: " + currentTurn, SwingConstants.CENTER);
        roundLabel = new JLabel("Round: " + round, SwingConstants.CENTER);

        stateLabel.setForeground(TEXT);
        turnLabel.setForeground(TEXT);
        roundLabel.setForeground(TEXT);

        stateLabel.setFont(TITLE_FONT);
        turnLabel.setFont(TITLE_FONT);
        roundLabel.setFont(TITLE_FONT);

        top.add(stateLabel);
        top.add(turnLabel);
        top.add(roundLabel);

        // LOG AREA
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(PANEL_DARK);
        logArea.setForeground(TEXT);
        logArea.setCaretColor(TEXT);
        logArea.setFont(LOG_FONT);
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));
        scrollPane.getViewport().setBackground(PANEL_DARK);

        // OPTIONAL TITLE
        JLabel logTitle = new JLabel("Server Packet Log");
        logTitle.setForeground(TEXT);
        logTitle.setFont(TITLE_FONT);

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(PANEL);
        logPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        logPanel.add(logTitle, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        // ADD COMPONENTS
        root.add(top, BorderLayout.NORTH);
        root.add(logPanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        log("Server UI started.");
    }

    private static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log("Server listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                log("Client connected: " + socket.getInetAddress());
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
        }
    }

    private static void log(String message) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(message + "\n");
            }
        });
    }

    private static void refreshLabels() {
        SwingUtilities.invokeLater(() -> {
            if (stateLabel != null) stateLabel.setText("State: " + serverState);
            if (turnLabel != null) turnLabel.setText("Turn: " + currentTurn);
            if (roundLabel != null) roundLabel.setText("Round: " + round);
        });
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private boolean authenticated = false;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String line;
                while ((line = in.readLine()) != null) {
                    log("RX: " + line);

                    String[] parts = line.trim().split(" ");
                    String command = parts[0];

                    switch (command) {
                        case "LOGIN" -> handleLogin(parts, out);

                        case "GET_STATE" -> {
                            if (!checkAuth(out)) continue;
                            out.println("STATE state=" + serverState + " turn=" + currentTurn + " round=" + round);
                            log("TX: STATE state=" + serverState + " turn=" + currentTurn + " round=" + round);
                        }

                        case "NEXT_TURN" -> {
                            if (!checkAuth(out)) continue;

                            serverState = "COMBAT";
                            currentTurn++;

                            if (currentTurn > 4) {
                                currentTurn = 1;
                                round++;
                            }

                            refreshLabels();
                            out.println("OK next_turn state=" + serverState + " turn=" + currentTurn + " round=" + round);
                            log("TX: OK next_turn state=" + serverState + " turn=" + currentTurn + " round=" + round);
                        }

                        case "ROLL_DICE" -> {
                            if (!checkAuth(out)) continue;

                            if (parts.length < 2) {
                                out.println("ERROR missing_dice_sides");
                                log("TX: ERROR missing_dice_sides");
                                continue;
                            }

                            int sides;
                            try {
                                sides = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                out.println("ERROR invalid_dice_sides");
                                log("TX: ERROR invalid_dice_sides");
                                continue;
                            }

                            int result = new Random().nextInt(sides) + 1;
                            out.println("DICE_RESULT sides=" + sides + " result=" + result);
                            log("TX: DICE_RESULT sides=" + sides + " result=" + result);
                        }

                        case "GET_IMAGE" -> {
                            if (!checkAuth(out)) continue;

                            Path imagePath = Path.of("large-demo.jpg");
                            if (Files.exists(imagePath)) {
                                long size = Files.size(imagePath);
                                out.println("IMAGE_READY bytes=" + size + " path=" + imagePath.toAbsolutePath());
                                log("TX: IMAGE_READY bytes=" + size + " path=" + imagePath.toAbsolutePath());
                            } else {
                                out.println("ERROR image_missing");
                                log("TX: ERROR image_missing");
                            }
                        }

                        default -> {
                            out.println("ERROR unknown_command");
                            log("TX: ERROR unknown_command");
                        }
                    }
                }
            } catch (IOException e) {
                log("Client handler error: " + e.getMessage());
            }
        }

        private void handleLogin(String[] parts, PrintWriter out) {
            if (parts.length < 3) {
                out.println("ERROR bad_login_format");
                log("TX: ERROR bad_login_format");
                return;
            }

            String username = parts[1];
            String password = parts[2];

            if (username.equals("dm") && password.equals("1234")) {
                authenticated = true;
                out.println("AUTH_OK role=DM");
                log("TX: AUTH_OK role=DM");
            } else if (username.equals("player1") && password.equals("1111")) {
                authenticated = true;
                out.println("AUTH_OK role=PLAYER");
                log("TX: AUTH_OK role=PLAYER username=player1");
            } else if (username.equals("player2") && password.equals("2222")) {
                authenticated = true;
                out.println("AUTH_OK role=PLAYER");
                log("TX: AUTH_OK role=PLAYER username=player2");
            } else {
                out.println("AUTH_FAIL");
                log("TX: AUTH_FAIL");
            }
        }

        private boolean checkAuth(PrintWriter out) {
            if (!authenticated) {
                out.println("ERROR not_authenticated");
                log("TX: ERROR not_authenticated");
                return false;
            }
            return true;
        }
    }
}