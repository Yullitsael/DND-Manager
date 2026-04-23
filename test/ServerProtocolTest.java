import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

public class ServerProtocolTest {

    private String send(String command) throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(command);
            return in.readLine();
        }
    }

    @Test
    void testValidLoginDM() throws Exception {
        String response = send("LOGIN dm 1234");
        assertTrue(response.contains("AUTH_OK"));
    }

    @Test
    void testInvalidLogin() throws Exception {
        String response = send("LOGIN dm wrong");
        assertEquals("AUTH_FAIL", response);
    }

    @Test
    void testGetStateRequiresAuth() throws Exception {
        String response = send("GET_STATE");
        assertTrue(response.contains("ERROR") || response.contains("not_authenticated"));
    }

    @Test
    void testGetStateAfterLogin() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN dm 1234");
            in.readLine();

            out.println("GET_STATE");
            String response = in.readLine();

            assertTrue(response.startsWith("STATE"));
        }
    }

    @Test
    void testNextTurn() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN dm 1234");
            in.readLine();

            out.println("NEXT_TURN");
            String response = in.readLine();

            assertTrue(response.startsWith("OK"));
        }
    }

    @Test
    void testDiceRoll() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN dm 1234");
            in.readLine();

            out.println("ROLL_DICE 20");
            String response = in.readLine();

            assertTrue(response.startsWith("DICE_RESULT"));
        }
    }

    @Test
    void testInvalidDice() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN dm 1234");
            in.readLine();

            out.println("ROLL_DICE abc");
            String response = in.readLine();

            assertTrue(response.contains("ERROR"));
        }
    }

    @Test
    void testImageRequest() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN dm 1234");
            in.readLine();

            out.println("GET_IMAGE");
            String response = in.readLine();

            assertTrue(response.startsWith("IMAGE_READY") || response.contains("ERROR"));
        }
    }

    @Test
    void testUnknownCommand() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN dm 1234");
            in.readLine();

            out.println("HELLO_WORLD");
            String response = in.readLine();

            assertTrue(response.contains("ERROR"));
        }
    }

    @Test
    void testValidLoginPlayer1() throws Exception {
        String response = send("LOGIN player1 1111");
        assertEquals("AUTH_OK role=PLAYER", response);
    }

    @Test
    void testValidLoginPlayer2() throws Exception {
        String response = send("LOGIN player2 2222");
        assertEquals("AUTH_OK role=PLAYER", response);
    }

    @Test
    void testBadLoginFormat() throws Exception {
        String response = send("LOGIN dm");
        assertEquals("ERROR bad_login_format", response);
    }

    @Test
    void testNextTurnRequiresAuth() throws Exception {
        String response = send("NEXT_TURN");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void testRollDiceRequiresAuth() throws Exception {
        String response = send("ROLL_DICE 20");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void testImageRequiresAuth() throws Exception {
        String response = send("GET_IMAGE");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void testMissingDiceSides() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN dm 1234");
            in.readLine();

            out.println("ROLL_DICE");
            String response = in.readLine();

            assertTrue(response.contains("ERROR"));
        }
    }

    @Test
    void testD20ResultInRange() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN dm 1234");
            in.readLine();

            out.println("ROLL_DICE 20");
            String response = in.readLine();

            assertTrue(response.startsWith("DICE_RESULT"));
            String[] parts = response.split(" ");
            int result = Integer.parseInt(parts[2].split("=")[1]);
            assertTrue(result >= 1 && result <= 20);
        }
    }

    @Test
    void testMultipleNextTurnsKeepWorking() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN dm 1234");
            in.readLine();

            out.println("NEXT_TURN");
            String r1 = in.readLine();

            out.println("NEXT_TURN");
            String r2 = in.readLine();

            assertTrue(r1.startsWith("OK"));
            assertTrue(r2.startsWith("OK"));
        }
    }

    @Test
    void testGetStateAfterPlayerLogin() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN player1 1111");
            in.readLine();

            out.println("GET_STATE");
            String response = in.readLine();

            assertTrue(response.startsWith("STATE"));
        }
    }

    @Test
    void testPlayerCanNextTurn() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN player1 1111");
            in.readLine();

            out.println("NEXT_TURN");
            String response = in.readLine();

            assertTrue(response.startsWith("OK"));
        }
    }

    @Test
    void testPlayerCanRollDice() throws Exception {
        try (Socket socket = new Socket("localhost", 5050);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN player2 2222");
            in.readLine();

            out.println("ROLL_DICE 20");
            String response = in.readLine();

            assertTrue(response.startsWith("DICE_RESULT"));
        }
    }
}