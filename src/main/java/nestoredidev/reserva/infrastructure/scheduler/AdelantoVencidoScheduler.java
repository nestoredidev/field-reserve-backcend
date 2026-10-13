package nestoredidev.reserva.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nestoredidev.reserva.application.ports.input.ReservaServicePort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** RN-RES-14: cada 10 minutos cancela las reservas PENDIENTE sin adelanto a tiempo. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdelantoVencidoScheduler {
    private final ReservaServicePort reservaService;

    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    public void cancelarVencidas() {
        int canceladas = reservaService.cancelarVencidas();
        if (canceladas > 0) {
            log.info("Reservas canceladas por adelanto no pagado: {}", canceladas);
        }
    }
}
