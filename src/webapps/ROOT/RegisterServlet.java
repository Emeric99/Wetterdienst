package webapps.ROOT;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.HexFormat;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // MariaDB-Verbindungsparameter – hier wird der DNS-Name des MariaDB-Containers verwendet
    private static final String JDBC_URL = "jdbc:mariadb://mariadb:3306/dbdemo";
    private static final String JDBC_USER = "dbuser";
    private static final String JDBC_PASSWORD = "complexpassword";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Parameter aus dem Formular auslesen
        String email = request.getParameter("email");
        String passwort = request.getParameter("passwort");
        String benutzername = request.getParameter("benutzername");
        String adresse = request.getParameter("adresse");
        String postleitzahl = request.getParameter("postleitzahl");
        String stadt = request.getParameter("stadt");

        // Serverseitige Validierung: Alle Felder müssen ausgefüllt sein
        if (email == null || email.isEmpty() ||
            passwort == null || passwort.isEmpty() ||
            benutzername == null || benutzername.isEmpty() ||
            adresse == null || adresse.isEmpty() ||
            postleitzahl == null || postleitzahl.isEmpty() ||
            stadt == null || stadt.isEmpty()) {
            response.sendRedirect("register.html?error=" + URLEncoder.encode("Alle Felder müssen ausgefüllt werden", StandardCharsets.UTF_8.name()));
            return;
        }

        String emailError = "";
        String usernameError = "";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // JDBC-Treiber laden und Verbindung herstellen
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);

            // Prüfung, ob die E-Mail bereits existiert
            String sql = "SELECT 1 FROM users WHERE email = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();
            if (rs.next()) {
                emailError = "Diese Email ist bereits registriert";
            }
            rs.close();
            stmt.close();

            // Prüfung, ob der Benutzername bereits vergeben ist
            sql = "SELECT 1 FROM users WHERE benutzername = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, benutzername);
            rs = stmt.executeQuery();
            if (rs.next()) {
                usernameError = "Dieser Benutzername ist bereits vergeben";
            }
            rs.close();
            stmt.close();

            // Falls Fehler auftreten, zurück zum Formular mit Fehlermeldungen
            if (!emailError.isEmpty() || !usernameError.isEmpty()) {
                String redirectUrl = "register.html?" +
                        (emailError.isEmpty() ? "" : "emailError=" + URLEncoder.encode(emailError, StandardCharsets.UTF_8.name()) + "&") +
                        (usernameError.isEmpty() ? "" : "usernameError=" + URLEncoder.encode(usernameError, StandardCharsets.UTF_8.name()) + "&") +
                        "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8.name()) + "&" +
                        "benutzername=" + URLEncoder.encode(benutzername, StandardCharsets.UTF_8.name()) + "&" +
                        "adresse=" + URLEncoder.encode(adresse, StandardCharsets.UTF_8.name()) + "&" +
                        "postleitzahl=" + URLEncoder.encode(postleitzahl, StandardCharsets.UTF_8.name()) + "&" +
                        "stadt=" + URLEncoder.encode(stadt, StandardCharsets.UTF_8.name());
                response.sendRedirect(redirectUrl);
                return;
            }

            // Passwort hashen: Erzeuge einen Salt und berechne den Hash
            byte[] salt = generateSalt();
            byte[] hash = hashPassword(passwort, salt);
            // Speichern im Format "salt:hash" als Hexadezimal-Strings
            String hashedPassword = HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);

            // Neuen Benutzer in die Datenbank einfügen
            sql = "INSERT INTO users (email, passwort, benutzername, adresse, postleitzahl, stadt) VALUES (?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, benutzername);
            stmt.setString(4, adresse);
            stmt.setString(5, postleitzahl);
            stmt.setString(6, stadt);
            stmt.executeUpdate();

            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<html><body><h2>Registrierung erfolgreich!</h2></body></html>");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Fehler bei der Registrierung: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { }
            try { if (conn != null) conn.close(); } catch (SQLException e) { }
        }
    }

    // Erzeugt einen zufälligen Salt mit 8 Byte Länge (wie von RSA PKCS5 empfohlen)
    public static byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        return salt;
    }

    // Hash-Funktion, die PBKDF2 mit HMAC SHA512 verwendet
    public static byte[] hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        // 210.000 Iterationen, 512 Bit Schlüssel
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 210000, 512);
        SecretKey key = secretKeyFactory.generateSecret(spec);
        spec.clearPassword(); // Löscht das Passwort aus dem Speicherspec, um die Sicherheit zu erhöhen
        return key.getEncoded();
    }
}
