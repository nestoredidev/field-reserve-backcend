package nestoredidev.reporte.infrastructure.adapter.input.rest;

import lombok.RequiredArgsConstructor;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.utils.pagination.PageResponse;
import nestoredidev.reporte.application.ports.input.ReporteServicePort;
import nestoredidev.reporte.application.ports.input.dto.GananciaProductoDto;
import nestoredidev.reporte.application.ports.input.dto.ResumenGeneralDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v2/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {
    private final ReporteServicePort service;

    @GetMapping("/ganancia-productos")
    public ResponseEntity<PageResponse<GananciaProductoDto>> gananciaProductos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ApiResponses.list(service.gananciaPorProducto(desde, hasta), "Ganancia por producto");
    }

    @GetMapping("/resumen-general")
    public ResponseEntity<PageResponse<ResumenGeneralDto>> resumenGeneral(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ApiResponses.ok(service.resumenGeneral(desde, hasta), "Resumen general");
    }
}
