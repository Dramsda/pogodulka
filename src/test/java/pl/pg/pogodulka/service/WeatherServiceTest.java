package pl.pg.pogodulka.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import pl.pg.pogodulka.dto.WeatherCategory;
import pl.pg.pogodulka.model.BadRequestException;

class WeatherServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    // --- kategoria z kodu WMO ---

    @Test
    void category_clearSky() {
        assertThat(WeatherService.category(0)).isEqualTo(WeatherCategory.SUNNY);
        assertThat(WeatherService.category(1)).isEqualTo(WeatherCategory.SUNNY);
    }

    @Test
    void category_cloudsAndFog() {
        assertThat(WeatherService.category(2)).isEqualTo(WeatherCategory.CLOUDY);
        assertThat(WeatherService.category(3)).isEqualTo(WeatherCategory.CLOUDY);
        assertThat(WeatherService.category(45)).isEqualTo(WeatherCategory.CLOUDY);
        assertThat(WeatherService.category(48)).isEqualTo(WeatherCategory.CLOUDY);
    }

    @Test
    void category_drizzleRainAndShowers() {
        assertThat(WeatherService.category(51)).isEqualTo(WeatherCategory.RAIN);
        assertThat(WeatherService.category(57)).isEqualTo(WeatherCategory.RAIN);
        assertThat(WeatherService.category(63)).isEqualTo(WeatherCategory.RAIN);
        assertThat(WeatherService.category(67)).isEqualTo(WeatherCategory.RAIN);
        assertThat(WeatherService.category(82)).isEqualTo(WeatherCategory.RAIN);
    }

    @Test
    void category_snow() {
        assertThat(WeatherService.category(71)).isEqualTo(WeatherCategory.SNOW);
        assertThat(WeatherService.category(77)).isEqualTo(WeatherCategory.SNOW);
        assertThat(WeatherService.category(86)).isEqualTo(WeatherCategory.SNOW);
    }

    @Test
    void category_thunderstorm() {
        assertThat(WeatherService.category(95)).isEqualTo(WeatherCategory.STORM);
        assertThat(WeatherService.category(99)).isEqualTo(WeatherCategory.STORM);
    }

    @Test
    void category_unknownCode() {
        assertThat(WeatherService.category(42)).isEqualTo(WeatherCategory.CLOUDY);
    }

    // --- walidacja dat ---

    @Test
    void validateDates_todayToLastDay() {
        assertThatCode(() -> WeatherService.validateDates(TODAY, TODAY.plusDays(15), TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    void validateDates_fromAfterTo() {
        assertThatThrownBy(() -> WeatherService.validateDates(TODAY.plusDays(3), TODAY.plusDays(1), TODAY))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateDates_past() {
        assertThatThrownBy(() -> WeatherService.validateDates(TODAY.minusDays(1), TODAY, TODAY))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateDates_beyondForecast() {
        assertThatThrownBy(() -> WeatherService.validateDates(TODAY, TODAY.plusDays(16), TODAY))
                .isInstanceOf(BadRequestException.class);
    }
}
