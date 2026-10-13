package nestoredidev;

import org.springframework.boot.SpringApplication;

public class TestFieldReserveBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(FieldReserveBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
