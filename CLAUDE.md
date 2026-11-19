# weather-service: serwis pogodowy (minihackaton)

Planer wycieczek składa się z 5 mikroserwisów robionych przez 5 zespołów. **My robimy tylko serwis Pogoda.** Pozostałe serwisy nas nie interesują. Rozmawiamy z nimi wyłącznie przez kontrakt HTTP. Naszym głównym klientem jest serwis Atrakcje: na podstawie pogody decyduje, które atrakcje są dostępne.

## źródła prawdy

- `docs/kontrakt-pogoda.md` to kontrakt API uzgadniany z innymi zespołami. Punkty z **[JUTRO]** jeszcze są otwarte. Nie zgaduj ich, tylko zapytaj.
- `README.md` opisuje API tak, jak jest naprawdę zaimplementowane: każdy endpoint i każdy kod odpowiedzi. Aktualizujemy go w tym samym commicie co endpoint.
- Kontrakt i README muszą być zgodne. Jeśli implementacja odbiega od kontraktu, poprawiamy kod albo świadomie zmieniamy kontrakt i dajemy znać zespołowi Atrakcje.

## stack (ustalony)

- Java 25, Spring Boot 4.1.2, Maven z wrapperem (`mvnw`). Maven nie jest zainstalowany globalnie.
- Spring Web MVC, Validation, Data JPA, RestClient (`spring-boot-starter-restclient`), Actuator, Lombok.
- PostgreSQL to baza docelowa, uruchamiana z `compose.yaml` (`spring-boot-docker-compose` stawia ją sam przy `mvnw spring-boot:run`).
- Dockerfile pozwala innym zespołom uruchomić nasz serwis bez budowania.
- Konwencje kodu opisuje skill `java-spring-style`. Nie dotyczą nas jego części o pessimistic lock i relacjach `@ManyToOne`: mamy jedną główną encję i brak relacji.

## stack (propozycje do potwierdzenia)

- Testy na H2 w trybie PostgreSQL (`h2` w scope `test`) z `@Transactional` rollback, zgodnie ze skillem. Alternatywa to Testcontainers, ale na zajęciach wymaga działającego Dockera.
- Schemat tworzony przez `spring.jpa.hibernate.ddl-auto=update`. Na 3 godziny nie potrzebujemy Flyway.

## architektura serwisu

```
GET /api/weather/day | /api/weather/range
        │
   WeatherController ──► WeatherService
                              │ 1. WeatherProvider (zewnętrzne API albo generator danych)
                              │ 2. udało się → zapis/aktualizacja w bazie → 200, stale=false
                              │ 3. błąd/timeout → dane z bazy → 200, stale=true
                              │ 4. w bazie też nic → 503 + Retry-After: 60
                         WeatherRepository (PostgreSQL)
```

- `WeatherProvider` to interfejs, bo będą co najmniej dwie implementacje: zewnętrzne API (którego, ustalamy **[JUTRO]**) i generator danych udawanych. Wybór implementacji przez property `weather.provider`.
- Dane można też dopisać ręcznie przez endpoint zapisu (propozycja: `POST /api/weather`). To endpoint wewnętrzny, poza kontraktem z Atrakcjami.
- Wyjątki zgodnie ze skillem. Każdy nowy kod statusu dostaje własny wyjątek:
  - `BadRequestException` → 400,
  - `NotFoundException` → 404,
  - `ProviderUnavailableException` → 503 z nagłówkiem `Retry-After`.
  - Treść błędu zawsze `{"error": "..."}`.

## komendy

```bash
./mvnw spring-boot:run          # start + postgres z compose.yaml
./mvnw test                     # testy
docker build -t weather-service .
```

Windows (PowerShell): `.\mvnw.cmd spring-boot:run`, `.\mvnw.cmd test`.

## zasady pracy na hackatonie

- Najpierw ma działać `GET /api/weather/day` z danymi z generatora. Prawdziwe API i fallback na bazę dokładamy potem. Działający kontrakt jest ważniejszy niż kompletność.
- Commity małe, w stylu ze skilla (`add weather entity and repository`).
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

`.claude/settings.json` rejestruje marketplace `ponytail` z GitHuba (`DietrichGebert/ponytail`), ale niczego nie włącza na siłę. Kto chce tryb „najprostsze rozwiązanie”, instaluje go poleceniem `claude plugin install ponytail@ponytail`. Pamiętaj, że ponytail lubi wycinać abstrakcje. Interfejs `WeatherProvider` i fallback na bazę zostają, bo są częścią ustaleń.
