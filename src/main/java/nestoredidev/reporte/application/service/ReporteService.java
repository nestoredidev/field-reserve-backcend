package nestoredidev.reporte.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.common.exception.InvalidRequestException;
import nestoredidev.reporte.application.ports.input.ReporteServicePort;
import nestoredidev.reporte.application.ports.input.dto.GananciaProductoDto;
import nestoredidev.reporte.application.ports.input.dto.ResumenGeneralDto;
import nestoredidev.reporte.application.ports.output.ReportePersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService implements ReporteServicePort {
    private final ReportePersistencePort persistence;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<GananciaProductoDto> gananciaPorProducto(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde == null ? LocalDate.now(clock) : desde;
        LocalDate h = hasta == null ? d : hasta;
        validar(d, h);
        return persistence.gananciaPorProducto(inicio(d), inicio(h.plusDays(1)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenGeneralDto resumenGeneral(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde == null ? LocalDate.now(clock) : desde;
        LocalDate h = hasta == null ? d : hasta;
        validar(d, h);
        OffsetDateTime ini = inicio(d);
        OffsetDateTime fin = inicio(h.plusDays(1));
        return new ResumenGeneralDto(d, h, persistence.resumenCancha(ini, fin), persistence.resumenBodega(ini, fin));
    }

    private void validar(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new InvalidRequestException("hasta no puede ser anterior a desde");
        }
    }

    private OffsetDateTime inicio(LocalDate fecha) {
        return fecha.atStartOfDay(clock.getZone()).toOffsetDateTime();
    }
}
