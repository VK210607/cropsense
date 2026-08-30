package javaproject.cropsense.repository;

import javaproject.cropsense.model.FarmData;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final FarmDataRepository repository;

    public DataSeeder(FarmDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed 5 historical farm records on startup
        repository.save(new FarmData(90, 42, 43, 20.9, 82.0, 6.5, 202.9, "Rice"));
        repository.save(new FarmData(80, 55, 68, 21.0, 59.0, 6.8, 65.0,  "Maize"));
        repository.save(new FarmData(115,32, 42, 26.0, 62.0, 6.5, 95.0,  "Cotton"));
        repository.save(new FarmData(21, 125,200, 12.0, 83.0, 6.5, 72.0, "Grapes"));
        repository.save(new FarmData(105,30, 32, 26.0, 62.0, 6.5,178.0,  "Coffee"));

        System.out.println("✅ DataSeeder: 5 historical farm records loaded into H2 database.");
    }
}