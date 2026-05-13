package webapps.ROOT;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.util.*;
import org.json.*;

@WebServlet(urlPatterns = {"/api/weather", "/api/forecast", "/health", "/api/stats", "/api/music"})
public class WeatherRestServlet extends HttpServlet {

    private static final String OPENWEATHER_API_KEY = System.getenv().getOrDefault("OPENWEATHER_API_KEY", "");
    private static final String RAPIDAPI_KEY        = System.getenv().getOrDefault("RAPIDAPI_KEY", "");
    private static final String RAPIDAPI_HOST       = "meteostat.p.rapidapi.com";
    private static final String YOUTUBE_API_KEY     = System.getenv().getOrDefault("YOUTUBE_API_KEY", "");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        if ("/health".equals(path)) {
            out.println("{\"status\":\"UP\"}");
            return;
        }

        String lat  = req.getParameter("lat");
        String lon  = req.getParameter("lon");
        String city = req.getParameter("city");

        if ((lat == null || lon == null) && (city == null || city.isBlank())) {
            resp.setStatus(400);
            out.println("{\"error\":\"Parameter 'city' oder 'lat'+'lon' fehlt\"}");
            return;
        }

        try {
            if ("/api/weather".equals(path)) {
                out.println(lat != null ? getCurrentWeatherByCoords(lat, lon) : getCurrentWeatherByCity(city));
            } else if ("/api/forecast".equals(path)) {
                out.println(lat != null ? getForecastByCoords(lat, lon) : getForecastByCity(city));
            } else if ("/api/stats".equals(path)) {
                handleStatsRequest(req, resp);
            } else if ("/api/music".equals(path)) {
                handleMusicRequest(req, resp);
            } else {
                resp.setStatus(404);
                out.println("{\"error\":\"Pfad nicht gefunden\"}");
            }
        } catch (Exception e) {
            resp.setStatus(500);
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private String getCurrentWeatherByCity(String city) throws Exception {
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&lang=de&appid=%s",
                URLEncoder.encode(city, "UTF-8"), OPENWEATHER_API_KEY);
        return readUrl(url);
    }

    private String getForecastByCity(String city) throws Exception {
        String url = String.format("https://api.openweathermap.org/data/2.5/forecast?q=%s&units=metric&lang=de&appid=%s",
                URLEncoder.encode(city, "UTF-8"), OPENWEATHER_API_KEY);
        return extract3Days(readUrl(url));
    }

    private String getCurrentWeatherByCoords(String lat, String lon) throws Exception {
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&units=metric&lang=de&appid=%s",
                URLEncoder.encode(lat, "UTF-8"), URLEncoder.encode(lon, "UTF-8"), OPENWEATHER_API_KEY);
        return readUrl(url);
    }

    private String getForecastByCoords(String lat, String lon) throws Exception {
        String url = String.format("https://api.openweathermap.org/data/2.5/forecast?lat=%s&lon=%s&units=metric&lang=de&appid=%s",
                URLEncoder.encode(lat, "UTF-8"), URLEncoder.encode(lon, "UTF-8"), OPENWEATHER_API_KEY);
        return extract3Days(readUrl(url));
    }

    private String extract3Days(String rawJson) {
        JSONObject json = new JSONObject(rawJson);
        JSONArray list = json.getJSONArray("list");
        Map<String, JSONArray> grouped = new LinkedHashMap<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            String date = entry.getString("dt_txt").substring(0, 10);
            grouped.computeIfAbsent(date, d -> new JSONArray()).put(entry);
        }
        JSONObject result = new JSONObject();
        int count = 0;
        for (Map.Entry<String, JSONArray> day : grouped.entrySet()) {
            result.put(day.getKey(), day.getValue());
            if (++count >= 3) break;
        }
        return result.toString(2);
    }

    private void handleStatsRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String lat  = req.getParameter("lat");
        String lon  = req.getParameter("lon");
        String city = req.getParameter("city");
        String days = req.getParameter("days") != null ? req.getParameter("days") : "7";
        PrintWriter out = resp.getWriter();
        try {
            String stationId = getStationId(lat, lon, city);
            String url = String.format("https://meteostat.p.rapidapi.com/stations/daily?station=%s&start=%s&end=%s",
                    stationId,
                    LocalDate.now().minusDays(Integer.parseInt(days)).toString(),
                    LocalDate.now().toString());
            out.println(readRapidApiUrl(url));
        } catch (Exception e) {
            resp.setStatus(500);
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleMusicRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String weather = req.getParameter("weather");
        PrintWriter out = resp.getWriter();
        try {
            out.println(getYouTubeMusic(weather));
        } catch (Exception e) {
            resp.setStatus(500);
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private String getStationId(String lat, String lon, String city) throws Exception {
        String url;
        if (lat != null && lon != null) {
            url = String.format("https://meteostat.p.rapidapi.com/stations/nearby?lat=%s&lon=%s&limit=1",
                    URLEncoder.encode(lat, "UTF-8"), URLEncoder.encode(lon, "UTF-8"));
        } else {
            String weatherUrl = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
                    URLEncoder.encode(city, "UTF-8"), OPENWEATHER_API_KEY);
            JSONObject weatherJson = new JSONObject(readUrl(weatherUrl));
            double cityLat = weatherJson.getJSONObject("coord").getDouble("lat");
            double cityLon = weatherJson.getJSONObject("coord").getDouble("lon");
            url = String.format("https://meteostat.p.rapidapi.com/stations/nearby?lat=%s&lon=%s&limit=1",
                    URLEncoder.encode(String.valueOf(cityLat), "UTF-8"),
                    URLEncoder.encode(String.valueOf(cityLon), "UTF-8"));
        }
        JSONArray stations = new JSONObject(readRapidApiUrl(url)).getJSONArray("data");
        if (stations.length() == 0) throw new Exception("Keine Wetterstation gefunden");
        return stations.getJSONObject(0).getString("id");
    }

    private String getYouTubeMusic(String weather) throws Exception {
        String query = URLEncoder.encode(getMusicQueryForWeather(weather), "UTF-8");
        String url = String.format(
            "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=5&key=%s",
            query, YOUTUBE_API_KEY);
        return simplifyYouTubeResponse(readUrl(url));
    }

    private String simplifyYouTubeResponse(String rawJson) {
        JSONObject json = new JSONObject(rawJson);
        JSONArray items = json.getJSONArray("items");
        JSONArray tracks = new JSONArray();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject track = new JSONObject();
            track.put("title", item.getJSONObject("snippet").getString("title"));
            track.put("videoId", item.getJSONObject("id").getString("videoId"));
            track.put("thumbnail", item.getJSONObject("snippet").getJSONObject("thumbnails").getJSONObject("default").getString("url"));
            tracks.put(track);
        }
        return new JSONObject().put("tracks", tracks).toString();
    }

    private String getMusicQueryForWeather(String weather) {
        if (weather == null) return "weather mood music playlist";
        weather = weather.toLowerCase();
        if (weather.contains("sonnig") || weather.contains("klar"))    return "sunny day music playlist";
        if (weather.contains("regen"))                                  return "rainy day music playlist";
        if (weather.contains("schnee"))                                 return "snow winter music playlist";
        if (weather.contains("wolken") || weather.contains("bewölkt")) return "cloudy day music playlist";
        if (weather.contains("wind")  || weather.contains("sturm"))    return "windy day music playlist";
        return "weather mood music playlist";
    }

    private String readUrl(String urlStr) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
        con.setRequestMethod("GET");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = in.readLine()) != null) sb.append(l);
            return sb.toString();
        }
    }

    private String readRapidApiUrl(String urlStr) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("x-rapidapi-host", RAPIDAPI_HOST);
        con.setRequestProperty("x-rapidapi-key", RAPIDAPI_KEY);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = in.readLine()) != null) sb.append(l);
            return sb.toString();
        }
    }
}
