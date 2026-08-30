package javaproject.cropsense.ml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Pillar 3 — ML Demo Runner
 *
 * A {@link CommandLineRunner} that fires after Spring Boot fully starts up.
 * It exercises the {@link CropPredictionService} with several real-world
 * test scenarios and prints the results to the console.
 *
 * Purpose:
 *   - Verify that Pillar 3 works end-to-end without needing the web layer.
 *   - Serve as a usage example for the prediction API.
 *   - Can be disabled by removing the @Component annotation once
 *     Pillar 4 (web controllers) is in place.
 */
@Component
public class CropMLRunner implements CommandLineRunner {

    private static final Logger log = Logger.getLogger(CropMLRunner.class.getName());

    private final CropPredictionService predictionService;

    @Autowired
    public CropMLRunner(CropPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @Override
    public void run(String... args) throws Exception {

        log.info("\n\n" +
                "╔══════════════════════════════════════════════════╗\n" +
                "║   CropSense — Pillar 3 ML Demo (CommandLine)     ║\n" +
                "╚══════════════════════════════════════════════════╝");

        if (!predictionService.isModelReady()) {
            log.severe("ML model is not ready. Skipping demo.");
            return;
        }

        log.info("Supported crops (" + predictionService.getSupportedCropCount() + " total): "
                + predictionService.getSupportedCrops());

        // ─── Test Case 1: Ideal Rice Conditions ──────────────────────────
        runTest("Test 1 — Ideal Rice Conditions",
                new CropFeatures(
                        90,   // N
                        42,   // P
                        43,   // K
                        20.9, // temperature (°C)
                        82.0, // humidity (%)
                        6.5,  // pH
                        202.9 // rainfall (mm)
                ));

        // ─── Test Case 2: Ideal Maize Conditions ─────────────────────────
        runTest("Test 2 — Ideal Maize Conditions",
                new CropFeatures(80, 55, 68, 21.0, 59.0, 6.8, 65.0));

        // ─── Test Case 3: Ideal Cotton Conditions ────────────────────────
        runTest("Test 3 — Ideal Cotton Conditions",
                new CropFeatures(115, 32, 42, 26.0, 62.0, 6.5, 95.0));

        // ─── Test Case 4: Ideal Coffee Conditions ────────────────────────
        runTest("Test 4 — Ideal Coffee Conditions",
                new CropFeatures(105, 30, 32, 26.0, 62.0, 6.5, 178.0));

        // ─── Test Case 5: Grapes (very distinctive high K, low temp) ─────
        runTest("Test 5 — Ideal Grapes Conditions",
                new CropFeatures(21, 125, 200, 12.0, 83.0, 6.5, 72.0));

        // ─── Test Case 6: Apple (near-zero N, high K, mid temp) ──────────
        runTest("Test 6 — Ideal Apple Conditions",
                new CropFeatures(3, 125, 200, 22.5, 92.0, 6.5, 110.0));

        // ─── Test Case 7: Mixed / ambiguous conditions ───────────────────
        runTest("Test 7 — Ambiguous / Mixed Conditions",
                new CropFeatures(60, 55, 55, 25.0, 75.0, 6.5, 130.0));

        // ─── Cross-Validation Report ──────────────────────────────────────
        log.info("\n=== Running 10-Fold Cross-Validation ===");
        log.info("(This exercises all crops — may take a few seconds)\n");
        try {
            String report = predictionService.evaluateModel();
            // Print the full evaluation report line-by-line for readability
            for (String line : report.split("\\r?\\n")) {
                log.info(line);
            }
        } catch (Exception e) {
            log.warning("Cross-validation failed: " + e.getMessage());
        }

        log.info("\n✅ Pillar 3 demo complete.\n");
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private void runTest(String testName, CropFeatures features) {
        log.info("\n─── " + testName + " ───");
        log.info("Input: " + features);
        try {
            PredictionResult result = predictionService.predict(features);
            log.info(result.summary());

            List<String> top3 = predictionService.getTopCropRecommendations(features, 3);
            log.info("Top-3 quick list: " + top3);

        } catch (Exception e) {
            log.severe("Prediction failed for [" + testName + "]: " + e.getMessage());
        }
    }
}
