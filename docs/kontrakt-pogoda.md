# Pogoda

> Wersja robocza. Punkty oznaczone **[JUTRO]** ustalamy na hackatonie.

**Dostawca:** zespół Pogoda (weather-service). Dane pobieramy z zewnętrznego API pogodowego (którego, **[JUTRO]**) i zapisujemy w naszej bazie. Dane w bazie możemy też dodawać ręcznie lub generować.

**Klient:** zespół Atrakcje (attractions-service). Pyta o pogodę dla miejsca i dnia albo zakresu dni wycieczki, żeby zdecydować, które atrakcje są dostępne.

**Przykładowe żądanie (metoda, ścieżka, parametry):**

Jeden dzień:
```
GET /api/weather/day?city=Kraków&date=2026-11-20
GET /api/weather/day?lat=50.0614&lon=19.9366&date=2026-11-20
```

Zakres dni:
```
GET /api/weather/range?city=Kraków&from=2026-11-20&to=2026-11-22
GET /api/weather/range?lat=50.0614&lon=19.9366&from=2026-11-20&to=2026-11-22
```

| parametr | wymagany | format | opis |
|---|---|---|---|
| `city` | `city` albo `lat`+`lon` | tekst, w URL zakodowany (`Krak%C3%B3w`) | nazwa miasta |
| `lat`, `lon` | `city` albo `lat`+`lon` | liczba dziesiętna | współrzędne |
| `date` | tak (`/day`) | `YYYY-MM-DD` | dzień |
| `from`, `to` | tak (`/range`) | `YYYY-MM-DD` | pierwszy i ostatni dzień (włącznie) |

Nie ustaliliśmy jeszcze:
- **[JUTRO]** port i adres bazowy (do uzgodnienia z pozostałymi zespołami),
- **[JUTRO]** maksymalna długość zakresu,
- **[JUTRO]** czy można pytać o daty z przeszłości.

**Wzór odpowiedzi:**

**[JUTRO]** Pola dla każdego dnia jeszcze nie są ustalone. Do wyboru mamy:
- same surowe dane,
- dane i kategorię pogody (np. `RAIN`),
- dane, kategorię i podpowiedź `outdoorFriendly`.

Już wiadomo, że:
- odpowiedź to `200 OK` w formacie JSON,
- `/range` zwraca listę dni,
- każda odpowiedź ma flagę `stale` (`true`, gdy dane pochodzą z bazy, bo zewnętrzne API nie odpowiedziało).

Szkic do dyskusji:
```json
{
  "location": { "name": "Kraków", "lat": 50.0614, "lon": 19.9366 },
  "days": [
    { "date": "2026-11-20", "...": "pola do ustalenia" }
  ],
  "stale": false
}
```

Błędy mają postać `{ "error": "<opis>" }`:

| kod | kiedy |
|---|---|
| `400 Bad Request` | brak `city` i `lat`/`lon`, zły format daty, `from > to` |
| `404 Not Found` | nie znaleziono miasta |
| `503 Service Unavailable` | zewnętrzne API nie odpowiada i w bazie nie ma danych. Dochodzi nagłówek `Retry-After: 60` |

**Limit oczekiwania na odpowiedź:** **[JUTRO]**

**Zachowanie przy błędzie i niedostępności usługi:**

Po naszej stronie (dostawca):
1. Zewnętrzne API nie odpowiada, a w bazie są dane dla tego miejsca i dnia: zwracamy `200` z danymi z bazy i `"stale": true`.
2. Zewnętrzne API nie odpowiada i w bazie nic nie ma: zwracamy `503` z nagłówkiem `Retry-After: 60` i treścią `{"error": "..."}`.
3. Klient wysłał błędne parametry: od razu `400`, bez wywoływania zewnętrznego API.

Po stronie klienta (Atrakcje), gdy nasz serwis nie odpowie: **[JUTRO]**
