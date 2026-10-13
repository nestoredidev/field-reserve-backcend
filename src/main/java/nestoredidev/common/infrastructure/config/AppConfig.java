package nestoredidev.common.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

/** RN-G03: todo se interpreta en America/Lima. */
@Configuration
@EnableScheduling
public class AppConfig {
    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    @PostConstruct
    void defaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(LIMA));
    }

    @Bean
    public Clock clock() {
        return Clock.system(LIMA);
    }
}
