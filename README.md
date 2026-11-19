# weather-service

Serwis pogodowy planera wycieczek. Zwraca prognozę dla miasta albo współrzędnych, na jeden dzień albo zakres dni.

## stack

- java 25
- spring boot 4.1.2 (web mvc, validation, restclient, actuator)
- bez bazy danych
- maven wrapper

## build and run

```bash
./mvnw spring-boot:run
```

windows:

```bash
.\mvnw.cmd spring-boot:run
```

docker:

```bash
docker build -t weather-service .
```

## API

Pełny kontrakt uzgodniony z innymi zespołami jest w `docs/kontrakt-pogoda.md`. Tę sekcję uzupełniamy na bieżąco, gdy powstają kolejne endpointy.

Lokalizacja: `city` **albo** `lat`+`lon` (nie oba naraz). Daty `YYYY-MM-DD`, każda w przedziale od dziś do dziś + 15 dni.

### GET /api/weather/day

```
GET /api/weather/day?city=Kraków&date=2026-11-20
GET /api/weather/day?lat=50.0614&lon=19.9366&date=2026-11-20
```

`200 OK`:

```json
{
  "location": { "name": "Kraków", "lat": 50.06143, "lon": 19.93658 },
  "days": [
    { "date": "2026-11-20", "tempMin": 4.2, "tempMax": 10.3, "precipitationMm": 8.2, "windKph": 19.4, "category": "RAIN" }
  ],
  "stale": false
}
```

- `days` ma zawsze jeden element.
- `location.name` to `null`, gdy pytano po `lat`/`lon`.
- `category`: `SUNNY` | `CLOUDY` | `RAIN` | `SNOW` | `STORM`, wyliczana z kodu WMO.
- `stale` jest zawsze `false`.

### GET /api/weather/range

```
GET /api/weather/range?city=Kraków&from=2026-11-20&to=2026-11-22
GET /api/weather/range?lat=50.0614&lon=19.9366&from=2026-11-20&to=2026-11-22
```

`200 OK`, ten sam kształt co `/day`. `days` zawiera po jednym elemencie na każdy dzień od `from` do `to` włącznie, rosnąco (najwyżej 16).

### błędy

Treść zawsze `{"error": "<opis>"}`.

| kod | kiedy |
|---|---|
| `400` | brak `city` i `lat`/`lon`; `city` razem z `lat`/`lon`; tylko `lat` albo tylko `lon`; `lat` poza -90…90, `lon` poza -180…180; `lat`/`lon` nie jest liczbą; brak daty; zły format daty; data przed dziś albo po dziś + 15; `from > to` |
| `404` | Open-Meteo nie zna miasta (`{"error": "City <nazwa> not found"}`) |
| `503` | Open-Meteo nie odpowiada w 3 s albo zwraca błąd; nagłówek `Retry-After: 60` |

### konfiguracja

| property | domyślnie | opis |
|---|---|---|
| `weather.provider` | `open-meteo` | `open-meteo` albo `fake` (dane udawane) |
| `weather.open-meteo.timeout` | `3s` | timeout połączenia i odczytu |

Dane udawane: `--weather.provider=fake`. Zarezerwowane miasta: `Atlantyda` → 404, `Awaria` → 503.

## tests

```bash
./mvnw test
```

- `ContractTest`: kontrakt przez HTTP na `FakeWeatherProvider` (bez sieci).
- `WeatherServiceTest`: kategoria z kodu WMO i walidacja dat, bez Springa.

## notes

- dane pobieramy na bieżąco z Open-Meteo, bez bazy. Gdy API nie odpowiada, zwracamy `503` z `Retry-After: 60`. Pole `stale` jest zawsze `false`.
