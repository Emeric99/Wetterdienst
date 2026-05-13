# Wetterdienst Pro

Eine webbasierte Wetteranwendung mit REST API, KI-Chatbot und Musikempfehlungen, entwickelt als Java-Servlet-Anwendung auf Apache Tomcat.

---

## Funktionen

- **Aktuelle Wetterdaten** – Abfrage nach Stadtname oder GPS-Koordinaten
- **3-Tage-Vorhersage** – Stundenweise Wetterprognose für 3 Tage
- **Wetterstatistiken** – 7-Tage-Temperaturverlauf mit Chart.js
- **Musikempfehlungen** – Passende YouTube-Musik je nach Wetterlage
- **KI-Wetterassistent** – Regelbasierter Chatbot für Wetterfragen
- **Benutzerregistrierung** – Registrierung mit PBKDF2-Passworthashing und MariaDB

---

## Tech-Stack

| Bereich | Technologie |
|--------|-------------|
| Backend | Java (Jakarta EE), HTTP-Servlets |
| Externe APIs | OpenWeatherMap, Meteostat (RapidAPI), YouTube Data API |
| Passwort-Hashing | PBKDF2WithHmacSHA512 + SecureRandom Salt |
| Datenbank | MariaDB |
| Frontend | HTML5, Bootstrap 5, Chart.js, JavaScript |
| Server | Apache Tomcat |
| Deployment | Docker |

---

## Projektstruktur

```
Wetterdienst/
├── src/webapps/ROOT/
│   ├── WeatherRestServlet.java    # REST API: Wetter, Prognose, Statistik, Musik
│   ├── WeatherServlet.java        # Einfache Wetterabfrage (HTML-Ausgabe)
│   ├── WeatherAIBotServlet.java   # Regelbasierter KI-Chatbot
│   └── RegisterServlet.java       # Benutzerregistrierung mit PBKDF2
├── webapp/
│   ├── WEB-INF/web.xml            # Servlet-Konfiguration
│   ├── weather.html               # Haupt-Frontend
│   └── register.html              # Registrierungsformular
├── sql/
│   └── init.sql                   # Datenbankschema
├── docker-compose.yml             # Startet MariaDB & Tomcat
├── .env.example                   # Vorlage für API-Schlüssel
└── pom.xml                        # Maven Build-Konfiguration
```

---

## API-Endpunkte

| Methode | Endpunkt | Beschreibung |
|--------|---------|-------------|
| GET | `/api/weather?city=Hamburg` | Aktuelles Wetter nach Stadt |
| GET | `/api/weather?lat=53.5&lon=10.0` | Aktuelles Wetter nach Koordinaten |
| GET | `/api/forecast?city=Berlin` | 3-Tage-Vorhersage |
| GET | `/api/stats?city=Bremen&days=7` | Temperaturstatistik |
| GET | `/api/music?weather=sonnig` | Musikempfehlungen |
| GET | `/api/ai-bot?msg=Wie+wird+das+Wetter?` | KI-Chatbot |

---

## Lokale Ausführung

### Voraussetzungen

- Docker & Docker Compose
- API-Schlüssel für OpenWeatherMap, RapidAPI und YouTube

### Setup

```bash
# 1. Repository klonen
git clone https://github.com/Emeric99/Wetterdienst.git
cd Wetterdienst

# 2. API-Schlüssel konfigurieren
cp .env.example .env
# Öffnen Sie .env und tragen Sie Ihre API-Schlüssel ein

# 3. Projekt kompilieren
docker compose --profile build run build

# 4. Anwendung starten
docker compose up
```

Anschließend im Browser öffnen:
👉 **http://localhost:8080/wetterdienst/weather.html**

---

## API-Schlüssel

Die Anwendung benötigt folgende API-Schlüssel (kostenlos erhältlich):

| Service | Registrierung |
|--------|--------------|
| OpenWeatherMap | [openweathermap.org](https://openweathermap.org/api) |
| RapidAPI (Meteostat) | [rapidapi.com](https://rapidapi.com/meteostat/api/meteostat) |
| YouTube Data API | [console.cloud.google.com](https://console.cloud.google.com) |


---

## Was ich dabei gelernt habe

- Entwicklung einer REST API mit Java Servlets (Jakarta EE)
- Anbindung mehrerer externer APIs (OpenWeatherMap, RapidAPI, YouTube)
- Sichere Passwortspeicherung mit PBKDF2 und kryptografischem Salt
- Datenbankanbindung mit MariaDB und JDBC
- Deployment mit Apache Tomcat in einer Docker-Umgebung
- Frontend-Entwicklung mit Bootstrap 5 und Chart.js

---

*Persönliches Projekt · Hochschule Bremerhaven · 2024*
