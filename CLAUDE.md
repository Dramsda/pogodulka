# weather-service: serwis pogodowy (minihackaton)

Planer wycieczek składa się z 5 mikroserwisów robionych przez 5 zespołów. **My robimy tylko serwis Pogoda.** Pozostałe serwisy nas nie interesują. Rozmawiamy z nimi wyłącznie przez kontrakt HTTP. Naszym głównym klientem jest serwis Atrakcje: na podstawie pogody decyduje, które atrakcje są dostępne.

## źródła prawdy

- `docs/kontrakt-pogoda.md` to kontrakt API uzgadniany z innymi zespołami. Punkty z **[JUTRO]** jeszcze są otwarte. Nie zgaduj ich, tylko zapytaj.
- `README.md` opisuje API tak, jak jest naprawdę zaimplementowane: każdy endpoint i każdy kod odpowiedzi. Aktualizujemy go w tym samym commicie co endpoint.
- Kontrakt i README muszą być zgodne. Jeśli implementacja odbiega od kontraktu, poprawiamy kod albo świadomie zmieniamy kontrakt i dajemy znać zespołowi Atrakcje.

## stack (ustalony)

- Java 25, Spring Boot 4.1.2, Maven z wrapperem (`mvnw`). Maven nie jest zainstalowany globalnie.
- Spring Web MVC, Validation, RestClient (`spring-boot-starter-restclient`), Actuator, Lombok.
- Serwis nie używa bazy danych.
- Dockerfile pozwala innym zespołom uruchomić nasz serwis bez budowania.
- Konwencje kodu opisuje skill `java-spring-style`. Nie dotyczą nas jego części o JPA, pessimistic lock i relacjach `@ManyToOne`: nie mamy bazy danych.

## stack (propozycje do potwierdzenia)

- Brak propozycji do potwierdzenia.

## architektura serwisu

```
GET /api/weather/day | /api/weather/range
        │
   WeatherController      parametry HTTP, 400 przy złym formacie
        │
   WeatherService         walidacja dat, city albo lat/lon → Location,
        │                 kategoria pogody, budowa WeatherResponse
        ▼
   WeatherProvider        ← interfejs, WeatherService zna tylko jego
      ├── OpenMeteoProvider   RestClient, JSON Open-Meteo → nasz model
      └── FakeWeatherProvider dane udawane (testy, demo)
```

Granica między serwisem a źródłem danych (pakiet `model`):

```java
public interface WeatherProvider {
    Optional<Location> findCity(String name);                                      // empty → 404
    List<DailyWeather> getDaily(Location location, LocalDate from, LocalDate to); // awaria → ProviderUnavailableException
}

record Location(String name, double lat, double lon) {}
record DailyWeather(LocalDate date, double tempMin, double tempMax,
                    double precipitationMm, double windKph, int weatherCode) {} // weatherCode: kod WMO
```

- `WeatherProvider` zwraca **nasz** model, nigdy JSON zewnętrznego API ani DTO odpowiedzi. Szczegóły źródła (URL, format, mapowanie) zostają w implementacji.
- Nowe źródło danych (np. baza z repository) to nowa implementacja `WeatherProvider`. `WeatherService` się nie zmienia.
- Wybór implementacji przez property `weather.provider` (`open-meteo` | `fake`) i `@ConditionalOnProperty` na każdej klasie.
- Kategorię (`SUNNY`, `CLOUDY`, `RAIN`, `SNOW`, `STORM`) wylicza `WeatherService` z kodu WMO. To czysta funkcja z własnym testem JUnit.
- `FakeWeatherProvider` ma zarezerwowane miasta: `Atlantyda` (brak miasta → 404) i `Awaria` (rzuca `ProviderUnavailableException` → 503).
- Wyjątki zgodnie ze skillem. Każdy nowy kod statusu dostaje własny wyjątek:
  - `BadRequestException` → 400,
  - `NotFoundException` → 404,
  - `ProviderUnavailableException` → 503 z nagłówkiem `Retry-After`.
  - Treść błędu zawsze `{"error": "..."}`.

## komendy

```bash
./mvnw spring-boot:run          # start serwisu
./mvnw test                     # testy
docker build -t weather-service .
```

Windows (PowerShell): `.\mvnw.cmd spring-boot:run`, `.\mvnw.cmd test`.

## zasady pracy na hackatonie

- Najpierw ma działać `GET /api/weather/day` z danymi z generatora. Prawdziwe API (Open-Meteo) dokładamy potem. Działający kontrakt jest ważniejszy niż kompletność.
- Commity małe, w stylu ze skilla (`add weather entity and repository`).
- Nad repo pracuje kilka osób: przed każdym `git push` najpierw `git pull --rebase`, potem `mvnw test`, dopiero wtedy push. Konflikt przy rebase rozwiązujemy z użytkownikiem, nie na ślepo.
- Przed oddaniem uruchom skill `smoke-test` i porównaj odpowiedzi z `docs/kontrakt-pogoda.md`.

## testy (ochrona przed regresjami)

Cały kod pisze AI, więc testy są specyfikacją. Kod dopasowujemy do testów, nie odwrotnie.
- `ContractTest` sprawdza kontrakt przez HTTP: jeden test na każdy wiersz kontraktu. Czysta logika ma zwykłe testy JUnit. Szczegóły w skillu `add-test`.
- Przy nowej regule najpierw piszemy test na podstawie `docs/kontrakt-pogoda.md`, a potem kod.
- **Testów nie zmieniamy bez zgody użytkownika.** Pilnują tego dwie rzeczy:
  - reguła `ask` dla `Edit(/src/test/**)`,
  - hook `guard-test-edits.sh`, który pyta o zgodę przy każdej komendzie shella dotykającej `src/test`.
- Hook `run-tests-on-stop.sh` uruchamia `mvnw test` po każdej turze, w której zmienił się `src/` albo `pom.xml`. Czerwone testy nie pozwalają zakończyć tury. Analiza wyniku: skill `run-tests`.
- Nie używamy `@Disabled`, `-DskipTests` ani poluzowanych asercji.

## pluginy (opcjonalne)

`.claude/settings.json` rejestruje marketplace `ponytail` z GitHuba (`DietrichGebert/ponytail`), ale niczego nie włącza na siłę. Kto chce tryb „najprostsze rozwiązanie”, instaluje go poleceniem `claude plugin install ponytail@ponytail`. Pamiętaj, że ponytail lubi wycinać abstrakcje. Interfejs `WeatherProvider` zostaje, bo jest częścią ustaleń.
