package javaproject.cropsense.ml;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.RandomForest;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Pillar 3 — Crop Prediction Service
 *
 * Core ML engine of CropSense. On application startup it:
 *   1. Loads the ARFF training dataset from the classpath.
 *   2. Attempts to load a pre-saved model from disk (if it exists).
 *   3. Falls back to training a fresh RandomForest if no saved model is found.
 *   4. Optionally saves the newly trained model for faster future startups.
 *
 * Public API:
 *   {@link #predict(CropFeatures)}                          — full result with probabilities
 *   {@link #getTopCropRecommendations(CropFeatures, int)}   — ranked list of top-N crops
 *   {@link #evaluateModel()}                               — 10-fold CV report string
 *   {@link #isModelReady()}                                — health check
 */
@Service
public class CropPredictionService {

    private static final Logger log = Logger.getLogger(CropPredictionService.class.getName());

    // ─── Configuration ───────────────────────────────────────────────────

    /** Classpath path to the ARFF training file. */
    private static final String ARFF_RESOURCE  = "ml/crop_training_data.arff";

    /** Where to save/load the trained model file. */
    private static final String MODEL_FILE_PATH = "models/cropsense_rf.model";

    /** Number of trees in the Random Forest. */
    private static final int    NUM_TREES       = 100;

    /** Random seed for reproducibility. */
    private static final int    RANDOM_SEED     = 42;

    // ─── State ───────────────────────────────────────────────────────────

    private final ModelTrainer modelTrainer;

    /** Trained Weka RandomForest classifier. */
    private RandomForest classifier;

    /**
     * Empty copy of the training dataset — preserves attribute/class structure
     * so we can create new Instances for incoming prediction requests.
     */
    private Instances dataStructure;

    /** The full training dataset (kept for cross-validation). */
    private Instances trainingData;

    // ─── Constructor ─────────────────────────────────────────────────────

    @Autowired
    public CropPredictionService(ModelTrainer modelTrainer) {
        this.modelTrainer = modelTrainer;
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────

    /**
     * Initialises the ML engine at application startup.
     * Runs after all Spring beans are wired.
     */
    @PostConstruct
    public void initialize() {
        log.info("╔══════════════════════════════════════════╗");
        log.info("║  CropSense Pillar 3 — ML Engine Loading  ║");
        log.info("╚══════════════════════════════════════════╝");

        try {
            // Step 1: Load training dataset from classpath
            log.info("Loading training dataset: " + ARFF_RESOURCE);
            trainingData = modelTrainer.loadDatasetFromClasspath(ARFF_RESOURCE);
            log.info(String.format("Dataset loaded — %d instances, %d attributes, %d classes",
                    trainingData.numInstances(),
                    trainingData.numAttributes(),
                    trainingData.numClasses()));

            // Preserve empty structure for building prediction instances later
            dataStructure = new Instances(trainingData, 0);

            // Step 2: Try loading a pre-saved model to save startup time
            if (modelTrainer.modelExists(MODEL_FILE_PATH)) {
                log.info("Found saved model at: " + MODEL_FILE_PATH + " — loading...");
                classifier = (RandomForest) modelTrainer.loadModel(MODEL_FILE_PATH);
                log.info("Saved model loaded successfully.");
            } else {
                // Step 3: Train a new RandomForest
                log.info(String.format(
                        "No saved model found. Training RandomForest (%d trees, seed=%d)...",
                        NUM_TREES, RANDOM_SEED));
                classifier = modelTrainer.trainRandomForest(trainingData, NUM_TREES, RANDOM_SEED);
                log.info("RandomForest training complete.");

                // Step 4: Persist the freshly trained model
                try {
                    modelTrainer.saveModel(classifier, MODEL_FILE_PATH);
                    log.info("Model saved to: " + MODEL_FILE_PATH);
                } catch (Exception saveEx) {
                    log.warning("Could not save model (non-fatal): " + saveEx.getMessage());
                }
            }

            log.info("✅ CropSense ML Engine ready — supports " +
                    dataStructure.classAttribute().numValues() + " crop types.");

        } catch (Exception e) {
            log.severe("❌ ML Engine failed to initialize: " + e.getMessage());
            throw new RuntimeException("CropPredictionService initialisation failed", e);
        }
    }

    // ─── Public Prediction API ───────────────────────────────────────────

    /**
     * Predicts the most suitable crop for the given agronomic features.
     *
     * @param features the 7 soil/climate features
     * @return {@link PredictionResult} with recommended crop, confidence, and full distribution
     * @throws Exception if the classifier cannot process the input
     */
    public PredictionResult predict(CropFeatures features) throws Exception {
        checkModelReady();

        Instance instance = toWekaInstance(features);

        // Get probability distribution over all classes
        double[] distribution = classifier.distributionForInstance(instance);

        // Determine the class with the highest probability
        int    bestIndex   = argmax(distribution);
        String bestCrop    = dataStructure.classAttribute().value(bestIndex);
        double confidence  = distribution[bestIndex];

        // Build full probability map (crop → probability)
        Map<String, Double> probMap = new LinkedHashMap<>();
        for (int i = 0; i < distribution.length; i++) {
            probMap.put(dataStructure.classAttribute().value(i), distribution[i]);
        }
        // Sort descending by probability
        Map<String, Double> sortedProbMap = sortDescendingByValue(probMap);

        // Build ranked list of all crop names
        List<String> topCrops = new ArrayList<>(sortedProbMap.keySet());

        return new PredictionResult(bestCrop, confidence, sortedProbMap, topCrops);
    }

    /**
     * Returns the top-N recommended crops for the given features.
     *
     * @param features the 7 soil/climate features
     * @param n        number of top crops to return (clamped to number of classes)
     * @return list of crop names ranked by suitability
     * @throws Exception if prediction fails
     */
    public List<String> getTopCropRecommendations(CropFeatures features, int n)
            throws Exception {
        PredictionResult result = predict(features);
        List<String> all = new ArrayList<>(result.getCropProbabilities().keySet());
        int limit = Math.max(1, Math.min(n, all.size()));
        return Collections.unmodifiableList(all.subList(0, limit));
    }

    /**
     * Runs 10-fold cross-validation on the training data and returns the
     * evaluation report as a formatted string.
     *
     * Note: This is slow for large datasets; call only when needed.
     *
     * @return formatted evaluation report
     * @throws Exception if evaluation fails
     */
    public String evaluateModel() throws Exception {
        checkModelReady();
        log.info("Running 10-fold cross-validation (this may take a moment)...");
        return modelTrainer.evaluateWithCrossValidation(classifier, trainingData, 10);
    }

    /**
     * Returns true if the ML classifier has been successfully initialised.
     */
    public boolean isModelReady() {
        return classifier != null && dataStructure != null;
    }

    /**
     * Returns the number of crop classes the model can predict.
     */
    public int getSupportedCropCount() {
        return dataStructure == null ? 0 : dataStructure.classAttribute().numValues();
    }

    /**
     * Returns all crop names the model was trained on, in alphabetical order.
     */
    public List<String> getSupportedCrops() {
        if (dataStructure == null) return Collections.emptyList();
        List<String> crops = new ArrayList<>();
        for (int i = 0; i < dataStructure.classAttribute().numValues(); i++) {
            crops.add(dataStructure.classAttribute().value(i));
        }
        Collections.sort(crops);
        return crops;
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    /**
     * Converts a {@link CropFeatures} object into a Weka {@link Instance}
     * compatible with the trained model's attribute structure.
     */
    private Instance toWekaInstance(CropFeatures features) {
        // 7 feature attributes + 1 class attribute = 8 values total
        double[] values = new double[dataStructure.numAttributes()];
        values[0] = features.getNitrogenContent();
        values[1] = features.getPhosphorusContent();
        values[2] = features.getPotassiumContent();
        values[3] = features.getTemperature();
        values[4] = features.getHumidity();
        values[5] = features.getPhLevel();
        values[6] = features.getRainfall();
        values[7] = Utils.missingValue();   // class label — unknown, to be predicted

        DenseInstance instance = new DenseInstance(1.0, values);
        instance.setDataset(dataStructure);  // attach dataset structure for attribute info
        return instance;
    }

    /**
     * Returns the index of the maximum value in an array.
     */
    private int argmax(double[] arr) {
        int best = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[best]) best = i;
        }
        return best;
    }

    /**
     * Sorts a String→Double map by value in descending order.
     */
    private Map<String, Double> sortDescendingByValue(Map<String, Double> map) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(map.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        Map<String, Double> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }

    /**
     * Throws an {@link IllegalStateException} if the model has not been initialised.
     */
    private void checkModelReady() {
        if (!isModelReady()) {
            throw new IllegalStateException(
                "CropPredictionService is not ready. " +
                "Model may have failed to initialize — check application logs.");
        }
    }
}
