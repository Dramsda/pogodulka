# weather-service

Serwis pogodowy planera wycieczek. Zwraca prognozę dla miasta albo współrzędnych, na jeden dzień albo zakres dni.

## stack

- java 25
- spring boot 4.1.2 (web mvc, data jpa, validation, restclient, actuator)
- postgresql (compose.yaml), h2 w testach
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

- gdy zewnętrzne API nie odpowiada, oddajemy dane z bazy z `"stale": true`. Gdy w bazie nic nie ma, zwracamy `503` z `Retry-After: 60`. Dzięki temu awaria dostawcy nie blokuje planowania wycieczki.
