package webapps.ROOT;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.Locale;
import java.util.regex.*;

@WebServlet("/api/ai-bot")
public class WeatherAIBotServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String msg = req.getParameter("msg");
        if (msg == null || msg.isBlank()) {
            out.println("{\"reply\":\"Bitte stelle mir eine Frage zum Wetter.\"}");
            return;
        }

        String reply = respondIntelligently(msg);
        out.printf("{\"reply\": \"%s\"}", escapeJson(reply));
    }

    private String respondIntelligently(String input) {
        String lower = input.toLowerCase(Locale.GERMANY);

        if (lower.contains("wetter in")) {
            String city = extractCity(lower);
            return "Möchtest du das Wetter in " + city + " wissen? Du kannst auch eine 3-Tage-Vorhersage anfordern.";
        }

        if (lower.matches(".*(kalt|frieren|winter).*")) {
            return "Es scheint kalt zu sein! ❄️ Denk an eine Jacke.";
        }

        if (lower.matches(".*(warm|heiß|sommer|sonne).*")) {
            return "Klingt nach Sonne! 😎 Trage Sonnencreme!";
        }

        if (lower.matches(".*(regen|nass|regnet).*")) {
            return "Heute ist es regnerisch. Schirm nicht vergessen ☔";
        }

        if (lower.contains("empfehlung") || lower.contains("vorschlag")) {
            return "Gerne! Sag mir die Stadt und ich schlage Kleidung oder Aktivitäten vor.";
        }

        return "Ich bin dein Wetterassistent 🤖 – frage mich z. B.: \"Wie wird das Wetter in Hamburg?\"";
    }

    private String extractCity(String sentence) {
        Pattern p = Pattern.compile("wetter in (\\w+)");
        Matcher m = p.matcher(sentence);
        return m.find() ? capitalize(m.group(1)) : "deiner Stadt";
    }

    private String capitalize(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    private String escapeJson(String s) {
        return s.replace("\"", "\\\"").replace("\n", "").replace("\r", "");
    }
}
