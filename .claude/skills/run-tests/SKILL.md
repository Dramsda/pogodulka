---
name: run-tests
description: Uruchamia testy weather-service i przy błędzie ocenia, czy winny jest kod, czy test, bez zmieniania testów na własną rękę. Używaj, gdy użytkownik prosi o uruchomienie testów, gdy hook po turze zgłosi czerwone testy, przed commitem i przed integracją z zespołem Atrakcje.
---

1. Uruchom testy: `./mvnw -q test`, na Windows w PowerShellu `.\mvnw.cmd -q test`. Gdy chodzi o jedną klasę, dodaj `-Dtest=ContractTest`.
2. Wszystko zielone: powiedz to jednym zdaniem i podaj liczbę testów. Koniec.
3. Coś czerwone: dla każdego testu, który nie przeszedł, podaj:
   - nazwę testu,
   - co było oczekiwane, a co przyszło (status i JSON),
   - **czyja to wina**:
     - **kod**, gdy test zgadza się z `docs/kontrakt-pogoda.md`, a kod nie. Napraw kod.
     - **test**, gdy test sprawdza coś, czego kontrakt nie mówi, albo kontrakt się zmienił. **Nie zmieniaj testu.** Opisz, co i dlaczego chcesz w nim zmienić, i zapytaj użytkownika.
     - **niejasne**, gdy kontrakt tego nie rozstrzyga (punkt **[JUTRO]**). Zapytaj użytkownika.
4. Po naprawie kodu uruchom testy ponownie. Wynik jest wiarygodny dopiero, gdy wszystkie są zielone.

Nigdy:
- nie wyłączaj testu (`@Disabled`) i nie usuwaj go,
- nie poluzowuj asercji tylko po to, żeby test przeszedł,
- nie uruchamiaj `-DskipTests`, żeby „na chwilę” obejść problem.

Testy uruchamia też automatycznie hook po każdej turze (`.claude/hooks/run-tests-on-stop.sh`), gdy zmieniło się coś w `src/` albo w `pom.xml`. Ten skill służy do świadomego uruchomienia i analizy wyniku.
