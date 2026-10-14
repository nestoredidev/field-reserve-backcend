package nestoredidev.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de los tests de repository: PostgreSQL real (Testcontainers) con el esquema de producción
 * ({@code db/schema.sql}, copia de bd.sql sin datos iniciales) para que vistas, constraints de exclusión
 * y consultas nativas se comporten igual que en la aplicación. El contenedor es único para toda la JVM y
 * cada test corre en una transacción que se revierte al terminar.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractRepositoryTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16")).withInitScript("db/schema.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @PersistenceContext
    protected EntityManager em;

    protected TestData data;

    @BeforeEach
    void initData() {
        data = new TestData(em);
    }

    protected void flushAndClear() {
        em.flush();
        em.clear();
    }
}
