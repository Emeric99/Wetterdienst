package webapps.ROOT;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.net.URL;
import java.net.HttpURLConnection;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.json.JSONObject;

@WebServlet("/weather1")
public class WeatherServlet extends HttpServlet {

    private static final String API_KEY = System.getenv().getOrDefault("OPENWEATHER_API_KEY", "");
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&lang=de&appid=%s";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String city = req.getParameter("city");
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<html><head><title>Wetter in " + city + "</title></head><body>");
        try {
            String query = URLEncoder.encode(city, "UTF-8");
            String urlString = String.format(API_URL, query, API_KEY);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                out.println("<h2>Fehler: Wetterdaten für \"" + city + "\" nicht gefunden.</h2>");
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONObject main = json.getJSONObject("main");
                double temp     = main.getDouble("temp");
                int humidity    = main.getInt("humidity");
                String desc     = json.getJSONArray("weather").getJSONObject(0).getString("description");

                out.printf("<h2>Wetter in %s</h2>%n", city);
                out.printf("<p>Temperatur: %.1f °C<br/>", temp);
                out.printf("Luftfeuchtigkeit: %d %%<br/>", humidity);
                out.printf("Beschreibung: %s</p>%n", desc);
            }
        } catch (Exception e) {
            out.println("<h2>Fehler:</h2><pre>" + e.getMessage() + "</pre>");
        }
        out.println("<a href=\"weather.html\">Neu abfragen</a>");
        out.println("</body></html>");
    }
}
