package nestoredidev.reserva.infrastructure.adapter.output.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.reserva.domain.model.TipoPagoReserva;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "cancha", name = "pago_reserva")
public class PagoReservaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "caja_id", nullable = false)
    private Long cajaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPagoReserva tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MetodoPago metodo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "nro_operacion", length = 30)
    private String nroOperacion;

    @Column(name = "voucher_url", length = 500)
    private String voucherUrl;

    @Column(name = "registrado_por", nullable = false)
    private Long registradoPor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;
}
