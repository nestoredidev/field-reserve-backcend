package nestoredidev.cajacancha.infrastructure.adapter.output.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nestoredidev.common.domain.EstadoCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "cancha", name = "caja")
public class CajaCanchaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "monto_apertura", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoApertura;

    @Column(name = "monto_cierre_contado", precision = 10, scale = 2)
    private BigDecimal montoCierreContado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoCaja estado;

    @Column(length = 255)
    private String observacion;

    @Column(name = "abierta_por", nullable = false)
    private Long abiertaPor;

    @Column(name = "cerrada_por")
    private Long cerradaPor;

    @Column(name = "abierta_at", nullable = false)
    private OffsetDateTime abiertaAt;

    @Column(name = "cerrada_at")
    private OffsetDateTime cerradaAt;
}
