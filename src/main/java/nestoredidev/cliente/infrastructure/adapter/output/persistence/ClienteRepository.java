package nestoredidev.cliente.infrastructure.adapter.output.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {

    Optional<ClienteEntity> findByTelefono(String telefono);

    @Query("select count(c) > 0 from ClienteEntity c where c.telefono = :telefono "
            + "and (:excluirId is null or c.id <> :excluirId)")
    boolean existsByTelefono(@Param("telefono") String telefono, @Param("excluirId") Long excluirId);

    @Query("select c from ClienteEntity c where :q is null or lower(c.nombre) like lower(concat('%', cast(:q as string), '%')) "
            + "or c.telefono like concat('%', cast(:q as string), '%')")
    Page<ClienteEntity> buscar(@Param("q") String q, Pageable pageable);
}
