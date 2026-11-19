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

### GET /api/weather/day

todo

### GET /api/weather/range

todo

## tests

todo

## notes

- dane pobieramy na bieżąco z Open-Meteo, bez bazy. Gdy API nie odpowiada, zwracamy `503` z `Retry-After: 60`. Pole `stale` jest zawsze `false`.
