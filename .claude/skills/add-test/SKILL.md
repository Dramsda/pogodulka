---
name: add-test
description: Jak dopisać test w weather-service. Kontrakt API (status i kształt JSON) sprawdza ContractTest przez MockMvcTester, czystą logikę sprawdzają zwykłe testy JUnit. Używaj przy każdym nowym endpoincie, regule biznesowej, kodzie błędu albo naprawionym bugu, a także gdy użytkownik prosi o test.
---

Testy to nasza siatka bezpieczeństwa przed regresjami w kodzie pisanym przez AI. Mają być **mało liczne, szybkie i sprawdzać tylko kontrakt i reguły**.

## kiedy jaki test

| co się zmienia | test | gdzie |
|---|---|---|
| endpoint, kod odpowiedzi, pole w JSON | test kontraktu | `ContractTest` (jedna klasa na cały serwis) |
| czysta funkcja: walidacja dat, mapowanie, wyliczenia | zwykły JUnit, bez Springa | `<Klasa>Test` obok testowanej klasy |
| naprawiony bug | test, który bez poprawki byłby czerwony | tam, gdzie pasuje według dwóch wierszy wyżej |
| getter, adnotacja walidacji, sam Spring | **nic** | |

Zanim dopiszesz test, sprawdź, czy podobny już nie istnieje.

## kolejność: test przed kodem

Przy nowej regule z kontraktu najpierw napisz test na podstawie `docs/kontrakt-pogoda.md`. Pokaż go użytkownikowi, bo zmiany w `src/test` i tak wymagają jego zgody. Dopiero potem pisz kod, aż test przejdzie. Wtedy test opisuje to, co uzgodniliśmy, a nie to, co akurat AI zaimplementowało.

## szablon testu kontraktu

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContractTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void getDay() {
        assertThat(mockMvc.get().uri("/api/weather/day?city={city}&date={date}", "Kraków", tomorrow()))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "location": { "name": "Kraków" }, "stale": false }
                        """);
    }

    @Test
    void getDay_unknownCity() {
        assertThat(mockMvc.get().uri("/api/weather/day?city=Atlantyda&date={date}", tomorrow()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "error": "City Atlantyda not found" }
                        """);
    }

    private String tomorrow() {
        return LocalDate.now().plusDays(1).toString();
    }
}
```

Zasady:
- Nazwy testów: `metoda_scenariusz` (`getRange_fromAfterTo`), a dla ścieżki szczęśliwej sama nazwa metody (`getDay`).
- **Zawsze sprawdzaj status i JSON.** `isLenientlyEqualTo` pozwala pominąć pola zmienne (temperatura z generatora, `fetchedAt`), ale klucze z kontraktu muszą być w asercji. Inaczej zmiana nazwy pola przejdzie niezauważona.
- Daty liczymy względnie od dziś (`tomorrow()`), nigdy nie wpisujemy ich na sztywno, bo test zestarzeje się jutro.
- Dane przygotowujemy przez API (helper na dole klasy). Nigdy przez repozytorium.
- Bez Mockito, `@MockBean` i `@WebMvcTest`. Zewnętrzne API zastępuje generator danych (`weather.provider=fake`) z zarezerwowanymi miastami `Atlantyda` (404) i `Awaria` (503).

## szablon testu czystej funkcji

```java
class DateRangeValidatorTest {

    @Test
    void fromAfterTo() {
        assertThatThrownBy(() -> DateRangeValidator.validate(LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 20)))
                .isInstanceOf(BadRequestException.class);
    }
}
```

Jeden test na przypadek brzegowy, bez kontekstu Springa.

## po napisaniu

Uruchom `./mvnw test`, na Windows `.\mvnw.cmd test`. Nowy test kontraktu napisany przed kodem **ma być czerwony**. Jeśli od razu jest zielony, prawdopodobnie niczego nie sprawdza.
