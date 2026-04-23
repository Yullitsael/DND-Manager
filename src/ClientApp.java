import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientApp {

    private static final String HOST = "localhost";
    private static final int PORT = 5050;

    private static final Color BG = new Color(15, 23, 42);
    private static final Color PANEL = new Color(30, 41, 59);
    private static final Color PANEL_DARK = new Color(2, 6, 23);
    private static final Color TEXT = new Color(226, 232, 240);
    private static final Color MUTED = new Color(148, 163, 184);
    private static final Color ACCENT = new Color(14, 165, 233);
    private static final Color SUCCESS = new Color(16, 185, 129);
    private static final Color WARNING = new Color(245, 158, 11);
    private static final Color PURPLE = new Color(139, 92, 246);

    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 24);
    private static final Font SUBTITLE_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font BODY_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font LOG_FONT = new Font("Consolas", Font.PLAIN, 13);

    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextArea logArea;

    private JLabel authLabel;
    private JLabel stateLabel;
    private JLabel turnLabel;
    private JLabel roundLabel;
    private JLabel diceLabel;

    private JLabel selectedNameLabel;
    private JLabel hpLabel;
    private JLabel acLabel;
    private JLabel statsLabel;

    private JPanel combatantListPanel;

    private PrintWriter out;
    private BufferedReader in;

    private final List<Combatant> combatants = new ArrayList<>();
    private Combatant selectedCombatant;

    private JButton nextTurnButton;
    private JButton rollDiceButton;
    private JButton imageButton;
    private JButton refreshButton;

    private boolean authenticated = false;
    private boolean isDM = false;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientApp().createUI());
    }

    private void createUI() {
        seedCombatants();

        frame = new JFrame("DND Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setSize(1400, 850);
        frame.setMinimumSize(new Dimension(1200, 750));
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        frame.setContentPane(root);

        // TOP
        JPanel topContainer = new JPanel(new BorderLayout(12, 12));
        topContainer.setOpaque(false);

        JLabel title = new JLabel("D&D Manager Client");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel("Authenticated client for combatants, state, dice, image request, and packet logging");
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(MUTED);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(subtitle);

        JPanel authPanel = createCardPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        usernameField = new JTextField("dm", 10);
        passwordField = new JPasswordField("1234", 10);
        JButton loginButton = styledButton("Login", ACCENT);
        authLabel = new JLabel("Not authenticated");
        authLabel.setFont(LABEL_FONT);
        authLabel.setForeground(WARNING);

        styleTextField(usernameField);
        stylePasswordField(passwordField);

        loginButton.addActionListener(e -> login());

        authPanel.add(styledText("Username:", LABEL_FONT, TEXT));
        authPanel.add(usernameField);
        authPanel.add(styledText("Password:", LABEL_FONT, TEXT));
        authPanel.add(passwordField);
        authPanel.add(loginButton);
        authPanel.add(Box.createHorizontalStrut(8));
        authPanel.add(authLabel);

        topContainer.add(titlePanel, BorderLayout.NORTH);
        topContainer.add(authPanel, BorderLayout.SOUTH);

        // MAIN GRID
        JPanel mainGrid = new JPanel(new GridLayout(1, 3, 12, 12));
        mainGrid.setOpaque(false);

        // LEFT: combatant list + selected details
        JPanel leftPanel = createCardPanel(new BorderLayout(12, 12));

        JLabel combatantsTitle = new JLabel("Combatants");
        combatantsTitle.setFont(SUBTITLE_FONT);
        combatantsTitle.setForeground(TEXT);

// TOP LIST PANEL
        combatantListPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        combatantListPanel.setBackground(PANEL);
        combatantListPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        combatantListPanel.setPreferredSize(new Dimension(320, 220));
        combatantListPanel.setMinimumSize(new Dimension(320, 220));

// DETAILS PANEL
        JPanel selectedPanel = new JPanel();
        selectedPanel.setLayout(new BoxLayout(selectedPanel, BoxLayout.Y_AXIS));
        selectedPanel.setBackground(PANEL_DARK);
        selectedPanel.setBorder(new EmptyBorder(16, 16, 16, 16));
        selectedPanel.setPreferredSize(new Dimension(320, 320));
        selectedPanel.setMinimumSize(new Dimension(320, 320));
        selectedPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JLabel selectedTitle = new JLabel("Selected Combatant");
        selectedTitle.setFont(SUBTITLE_FONT);
        selectedTitle.setForeground(TEXT);

        selectedNameLabel = new JLabel("-");
        selectedNameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        selectedNameLabel.setForeground(TEXT);
        selectedNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        hpLabel = new JLabel("HP: -");
        hpLabel.setFont(LABEL_FONT);
        hpLabel.setForeground(new Color(248, 113, 113));
        hpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        acLabel = new JLabel("AC: -");
        acLabel.setFont(LABEL_FONT);
        acLabel.setForeground(TEXT);
        acLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statsLabel = new JLabel("Stats: -");
        statsLabel.setFont(BODY_FONT);
        statsLabel.setForeground(TEXT);
        statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

// Wrap both list and details in one vertical panel
        JPanel leftContent = new JPanel();
        leftContent.setLayout(new BoxLayout(leftContent, BoxLayout.Y_AXIS));
        leftContent.setBackground(PANEL);

        leftContent.add(combatantsTitle);
        leftContent.add(Box.createVerticalStrut(10));
        leftContent.add(combatantListPanel);
        leftContent.add(Box.createVerticalStrut(12));

        selectedPanel.add(selectedTitle);
        selectedPanel.add(Box.createVerticalStrut(10));
        selectedPanel.add(selectedNameLabel);
        selectedPanel.add(Box.createVerticalStrut(10));
        selectedPanel.add(hpLabel);
        selectedPanel.add(Box.createVerticalStrut(8));
        selectedPanel.add(acLabel);
        selectedPanel.add(Box.createVerticalStrut(14));
        selectedPanel.add(statsLabel);

        leftContent.add(selectedPanel);

        leftPanel.add(leftContent, BorderLayout.CENTER);

        // CENTER: server state
        JPanel centerPanel = createCardPanel(new BorderLayout(12, 12));
        JLabel stateTitle = new JLabel("Server State");
        stateTitle.setFont(SUBTITLE_FONT);
        stateTitle.setForeground(TEXT);

        JPanel stateGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        stateGrid.setOpaque(false);

        stateLabel = infoCard("State", "-");
        turnLabel = infoCard("Turn", "-");
        roundLabel = infoCard("Round", "-");
        diceLabel = infoCard("Dice", "-");

        stateGrid.add(wrapLabelCard(stateLabel));
        stateGrid.add(wrapLabelCard(turnLabel));
        stateGrid.add(wrapLabelCard(roundLabel));
        stateGrid.add(wrapLabelCard(diceLabel));

        centerPanel.add(stateTitle, BorderLayout.NORTH);
        centerPanel.add(stateGrid, BorderLayout.CENTER);

        // RIGHT: actions
        JPanel rightPanel = createCardPanel(new BorderLayout(12, 12));
        JLabel actionsTitle = new JLabel("Client Actions");
        actionsTitle.setFont(SUBTITLE_FONT);
        actionsTitle.setForeground(TEXT);

        JPanel buttonGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        buttonGrid.setOpaque(false);
        buttonGrid.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        refreshButton = styledButton("Get State", new Color(51, 65, 85));
        nextTurnButton = styledButton("Next Turn", SUCCESS);
        rollDiceButton = styledButton("Roll d20", WARNING);
        imageButton = styledButton("Request Image", PURPLE);

        refreshButton.addActionListener(e -> sendCommand("GET_STATE"));
        nextTurnButton.addActionListener(e -> sendCommand("NEXT_TURN"));
        rollDiceButton.addActionListener(e -> sendCommand("ROLL_DICE 20"));
        imageButton.addActionListener(e -> sendCommand("GET_IMAGE"));

        buttonGrid.add(refreshButton);
        buttonGrid.add(nextTurnButton);
        buttonGrid.add(rollDiceButton);
        buttonGrid.add(imageButton);

        applyRoleControls();

        JLabel note = new JLabel("Use Login first, then test commands.");
        note.setFont(BODY_FONT);
        note.setForeground(MUTED);

        rightPanel.add(actionsTitle, BorderLayout.NORTH);
        rightPanel.add(buttonGrid, BorderLayout.CENTER);
        rightPanel.add(note, BorderLayout.SOUTH);

        mainGrid.add(leftPanel);
        mainGrid.add(centerPanel);
        mainGrid.add(rightPanel);

        // BOTTOM LOG
        JPanel logPanel = createCardPanel(new BorderLayout(12, 12));
        JLabel logTitle = new JLabel("Packet Log");
        logTitle.setFont(SUBTITLE_FONT);
        logTitle.setForeground(TEXT);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(PANEL_DARK);
        logArea.setForeground(TEXT);
        logArea.setCaretColor(TEXT);
        logArea.setFont(LOG_FONT);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));
        scrollPane.getViewport().setBackground(PANEL_DARK);
        scrollPane.setPreferredSize(new Dimension(1100, 220));

        logPanel.add(logTitle, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        root.add(topContainer, BorderLayout.NORTH);
        root.add(mainGrid, BorderLayout.CENTER);
        root.add(logPanel, BorderLayout.SOUTH);

        refreshCombatantList();
        refreshCombatantDetails();

        frame.setVisible(true);

        connect();
    }

    private void applyRoleControls() {
        if (nextTurnButton != null) {
            nextTurnButton.setEnabled(true);
            nextTurnButton.setBackground(SUCCESS);
        }
    }

    private void seedCombatants() {
        combatants.add(new Combatant("Player 1", "player", 67, 67, 13, 13, 14, 8, 11, 10, 13));
        combatants.add(new Combatant("Player 2", "player", 50, 50, 12, 10, 12, 14, 13, 11, 11));
        combatants.add(new Combatant("Monster 1", "monster", 80, 80, 15, 16, 10, 6, 8, 5, 14));
        combatants.add(new Combatant("Monster 2", "monster", 60, 60, 14, 14, 11, 7, 9, 6, 12));
        selectedCombatant = combatants.get(0);
    }

    private void refreshCombatantList() {
        combatantListPanel.removeAll();

        for (Combatant c : combatants) {
            JButton card = new JButton();
            card.setLayout(new BorderLayout());
            card.setBackground(c == selectedCombatant ? ACCENT : PANEL_DARK);
            card.setBorder(new EmptyBorder(10, 10, 10, 10));
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel name = new JLabel(c.name);
            name.setForeground(Color.WHITE);
            name.setFont(new Font("Arial", Font.BOLD, 14));

            JLabel role = new JLabel(c.name.toLowerCase().contains("monster") ? "Monster" : "Player");
            role.setForeground(MUTED);
            role.setFont(BODY_FONT);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(name);
            textPanel.add(role);

            card.add(textPanel, BorderLayout.CENTER);

            card.addActionListener(e -> {
                selectedCombatant = c;
                refreshCombatantList();
                refreshCombatantDetails();
            });

            combatantListPanel.add(card);
        }

        combatantListPanel.revalidate();
        combatantListPanel.repaint();
    }

    private void refreshCombatantDetails() {
        if (selectedCombatant == null) return;

        selectedNameLabel.setText(selectedCombatant.name);
        hpLabel.setText("HP: " + selectedCombatant.hp + "/" + selectedCombatant.maxHp);
        acLabel.setText("AC: " + selectedCombatant.ac);
        statsLabel.setText(
                "<html>" +
                        "<div style='width:260px;'>" +
                        "<b>STR</b> " + selectedCombatant.str + " &nbsp;&nbsp; " +
                        "<b>DEX</b> " + selectedCombatant.dex + " &nbsp;&nbsp; " +
                        "<b>INT</b> " + selectedCombatant.intel + "<br><br>" +
                        "<b>WIS</b> " + selectedCombatant.wis + " &nbsp;&nbsp; " +
                        "<b>CHA</b> " + selectedCombatant.cha + " &nbsp;&nbsp; " +
                        "<b>CON</b> " + selectedCombatant.con +
                        "</div>" +
                        "</html>"
        );

        statsLabel.setVerticalAlignment(SwingConstants.TOP);
    }


    private JPanel createCardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(PANEL);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }

    private JLabel styledText(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private void styleTextField(JTextField field) {
        field.setFont(BODY_FONT);
        field.setBackground(PANEL_DARK);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                new EmptyBorder(6, 8, 6, 8)
        ));
    }

    private void stylePasswordField(JPasswordField field) {
        field.setFont(BODY_FONT);
        field.setBackground(PANEL_DARK);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                new EmptyBorder(6, 8, 6, 8)
        ));
    }

    private JButton styledButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFont(LABEL_FONT);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JLabel infoCard(String title, String value) {
        return new JLabel(
                "<html><div style='text-align:center;'>" +
                        "<div style='font-size:11px; color:#94a3b8; text-transform:uppercase;'>" + title + "</div>" +
                        "<div style='font-size:18px; font-weight:bold; color:#e2e8f0; margin-top:4px;'>" + value + "</div>" +
                        "</div></html>",
                SwingConstants.CENTER
        );
    }

    private JPanel wrapLabelCard(JLabel label) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_DARK);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void updateInfoLabel(JLabel label, String title, String value) {
        label.setText(
                "<html><div style='text-align:center;'>" +
                        "<div style='font-size:11px; color:#94a3b8; text-transform:uppercase;'>" + title + "</div>" +
                        "<div style='font-size:18px; font-weight:bold; color:#e2e8f0; margin-top:4px;'>" + value + "</div>" +
                        "</div></html>"
        );
    }

    private void connect() {
        try {
            Socket socket = new Socket(HOST, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            log("Connected to server.");

            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        log("RX: " + response);
                        handleResponse(response);
                    }
                } catch (IOException e) {
                    log("Error reading from server: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            log("Connection failed: " + e.getMessage());
        }
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        sendCommand("LOGIN " + username + " " + password);
    }

    private void sendCommand(String command) {
        if (out == null) {
            log("Not connected to server.");
            return;
        }

        boolean isLoginCommand = command.startsWith("LOGIN");

        if (!authenticated && !isLoginCommand) {
            log("Blocked: login required before using commands.");
            JOptionPane.showMessageDialog(frame, "Please login first.", "Authentication Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        log("TX: " + command);
        out.println(command);
    }

    private void reconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (Exception ignored) {
        }

        out = null;
        in = null;

        connect();
    }

    private void handleResponse(String response) {
        if (response.startsWith("AUTH_OK")) {
            authenticated = true;

            if (response.contains("role=DM")) {
                isDM = true;
                authLabel.setText("Authenticated as DM");
            } else {
                isDM = false;
                authLabel.setText("Authenticated as Player");
            }

            authLabel.setForeground(SUCCESS);
            applyRoleControls();

        } else if (response.startsWith("AUTH_FAIL")) {
            authenticated = false;
            isDM = false;

            authLabel.setText("Login failed");
            authLabel.setForeground(new Color(248, 113, 113));
            applyRoleControls();

            reconnect();

        } else if (response.startsWith("STATE")) {
            String[] parts = response.split(" ");
            updateInfoLabel(stateLabel, "State", parts[1].split("=")[1]);
            updateInfoLabel(turnLabel, "Turn", parts[2].split("=")[1]);
            updateInfoLabel(roundLabel, "Round", parts[3].split("=")[1]);

        } else if (response.startsWith("OK next_turn")) {
            String[] parts = response.split(" ");
            updateInfoLabel(stateLabel, "State", parts[2].split("=")[1]);
            updateInfoLabel(turnLabel, "Turn", parts[3].split("=")[1]);
            updateInfoLabel(roundLabel, "Round", parts[4].split("=")[1]);

        } else if (response.startsWith("DICE_RESULT")) {
            String[] parts = response.split(" ");
            updateInfoLabel(diceLabel, "Dice", parts[2].split("=")[1]);

        } else if (response.startsWith("IMAGE_READY")) {
            JOptionPane.showMessageDialog(frame, response, "Large Data Transfer", JOptionPane.INFORMATION_MESSAGE);

        } else if (response.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(frame, response, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    static class Combatant {
        String name;
        String type;
        int hp;
        int maxHp;
        int ac;
        int str, dex, intel, wis, cha, con;

        Combatant(String name, String type, int hp, int maxHp, int ac,
                  int str, int dex, int intel, int wis, int cha, int con) {
            this.name = name;
            this.type = type;
            this.hp = hp;
            this.maxHp = maxHp;
            this.ac = ac;
            this.str = str;
            this.dex = dex;
            this.intel = intel;
            this.wis = wis;
            this.cha = cha;
            this.con = con;
        }
    }
}