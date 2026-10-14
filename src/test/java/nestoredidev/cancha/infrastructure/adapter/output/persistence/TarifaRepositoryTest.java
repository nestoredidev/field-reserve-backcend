package nestoredidev.cancha.infrastructure.adapter.output.persistence;

import jakarta.persistence.PersistenceException;
import nestoredidev.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TarifaRepositoryTest extends AbstractRepositoryTest {

    private static final LocalTime H10 = LocalTime.of(10, 0);
    private static final LocalTime H12 = LocalTime.of(12, 0);

    @Autowired
    private TarifaRepository repository;

    // ---------- listados ordenados ----------

    @Test
    void findByCanchaId_ordenaPorDiaYHoraInicioYSoloDeLaCancha() {
        Long cancha = data.cancha();
        Long otra = data.cancha();
        Long martes = data.tarifa(cancha, 2, "10:00", "12:00", "40.00", true);
        Long lunesTarde = data.tarifa(cancha, 1, "18:00", "20:00", "60.00", true);
        Long lunesManiana = data.tarifa(cancha, 1, "08:00", "10:00", "30.00", true);
        Long general = data.tarifa(cancha, null, "06:00", "23:00", "50.00", true);
        data.tarifa(otra, 1, "08:00", "10:00", "99.00", true);

        List<TarifaEntity> res = repository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(cancha);

        // en PostgreSQL ASC deja los NULL (tarifas generales) al final
        assertThat(res).extracting(TarifaEntity::getId).containsExactly(lunesManiana, lunesTarde, martes, general);
    }

    @Test
    void findByCanchaIdAndActivaTrue_excluyeLasInactivas() {
        Long cancha = data.cancha();
        Long activa = data.tarifa(cancha, 1, "08:00", "10:00", "30.00", true);
        data.tarifa(cancha, 1, "08:00", "10:00", "25.00", false);

        assertThat(repository.findByCanchaIdAndActivaTrueOrderByDiaSemanaAscHoraInicioAsc(cancha))
                .extracting(TarifaEntity::getId).containsExactly(activa);
    }

    // ---------- existeCruce ----------

    @Test
    void existeCruce_franjaQueSeSuperpone_esTrue() {
        Long cancha = data.cancha();
        data.tarifa(cancha, 1, "10:00", "12:00", "40.00", true);

        assertThat(repository.existeCruce(cancha, 1, LocalTime.of(11, 0), LocalTime.of(13, 0), null)).isTrue();
        assertThat(repository.existeCruce(cancha, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), null)).isTrue();
        assertThat(repository.existeCruce(cancha, 1, LocalTime.of(10, 30), LocalTime.of(11, 0), null)).isTrue();
        assertThat(repository.existeCruce(cancha, 1, LocalTime.of(8, 0), LocalTime.of(14, 0), null)).isTrue();
    }

    @Test
    void existeCruce_franjasContiguas_noSeCruzan() {
        Long cancha = data.cancha();
        data.tarifa(cancha, 1, "10:00", "12:00", "40.00", true);

        assertThat(repository.existeCruce(cancha, 1, LocalTime.of(12, 0), LocalTime.of(14, 0), null)).isFalse();
        assertThat(repository.existeCruce(cancha, 1, LocalTime.of(8, 0), H10, null)).isFalse();
    }

    @Test
    void existeCruce_otroDiaOGeneralConDiaCero_noSeCruzan() {
        Long cancha = data.cancha();
        data.tarifa(cancha, 1, "10:00", "12:00", "40.00", true);

        assertThat(repository.existeCruce(cancha, 2, H10, H12, null)).isFalse();
        assertThat(repository.existeCruce(cancha, 0, H10, H12, null)).isFalse(); // general (null) = día 0
    }

    @Test
    void existeCruce_tarifaGeneralSoloSeCruzaConOtraGeneral() {
        Long cancha = data.cancha();
        data.tarifa(cancha, null, "06:00", "23:00", "50.00", true);

        assertThat(repository.existeCruce(cancha, 0, H10, H12, null)).isTrue();
        assertThat(repository.existeCruce(cancha, 3, H10, H12, null)).isFalse();
    }

    @Test
    void existeCruce_ignoraTarifasInactivas() {
        Long cancha = data.cancha();
        data.tarifa(cancha, 1, "10:00", "12:00", "40.00", false);

        assertThat(repository.existeCruce(cancha, 1, H10, H12, null)).isFalse();
    }

    @Test
    void existeCruce_conExcluirId_noSeCruzaConsigoMisma() {
        Long cancha = data.cancha();
        Long tarifa = data.tarifa(cancha, 1, "10:00", "12:00", "40.00", true);

        assertThat(repository.existeCruce(cancha, 1, H10, H12, tarifa)).isFalse();
        assertThat(repository.existeCruce(cancha, 1, H10, H12, tarifa + 1000)).isTrue();
    }

    @Test
    void existeCruce_otraCancha_noSeCruza() {
        Long cancha = data.cancha();
        Long otra = data.cancha();
        data.tarifa(cancha, 1, "10:00", "12:00", "40.00", true);

        assertThat(repository.existeCruce(otra, 1, H10, H12, null)).isFalse();
    }

    // ---------- constraints de la base ----------

    @Test
    void dosTarifasActivasQueSeCruzan_laBaseLasRechazaPorExclusion() {
        Long cancha = data.cancha();
        data.tarifa(cancha, 1, "10:00", "12:00", "40.00", true);

        assertThatThrownBy(() -> data.tarifa(cancha, 1, "11:00", "13:00", "45.00", true))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("ex_tarifa_sin_cruce");
    }

    @Test
    void tarifaInactivaQueSeCruza_laBaseLaPermite() {
        Long cancha = data.cancha();
        data.tarifa(cancha, 1, "10:00", "12:00", "40.00", true);

        assertThat(data.tarifa(cancha, 1, "11:00", "13:00", "45.00", false)).isNotNull();
    }
}
