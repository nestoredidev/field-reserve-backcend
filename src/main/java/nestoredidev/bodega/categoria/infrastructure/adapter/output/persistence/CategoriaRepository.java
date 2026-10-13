package nestoredidev.bodega.categoria.infrastructure.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Long> {

    @Query("select count(c) > 0 from CategoriaEntity c where lower(c.nombre) = lower(:nombre) and c.id <> :excluirId")
    boolean existsByNombre(@Param("nombre") String nombre, @Param("excluirId") Long excluirId);

    @Query(value = "select count(*) > 0 from bodega.producto where categoria_id = :id", nativeQuery = true)
    boolean tieneProductos(@Param("id") Long id);
}
