package javaproject.cropsense.ml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;

import java.io.File;
import java.io.InputStream;
import java.util.Random;

/**
 * Pillar 3 — Model Trainer
 *
 * Responsible for:
 *   1. Loading ARFF datasets from the classpath.
 *   2. Training a Weka RandomForest classifier.
 *   3. Running k-fold cross-validation for evaluation.
 *   4. Serializing and deserializing trained models to/from disk.
 *
 * Used by {@link CropPredictionService} during application startup.
 */
@Component
public class ModelTrainer {

    // ─── Dataset Loading ─────────────────────────────────────────────────

    /**
     * Loads a Weka ARFF dataset from the classpath.
     * Sets the last attribute as the class (label) attribute.
     *
     * @param classpathResource  path relative to resources/, e.g. "ml/crop_training_data.arff"
     * @return loaded Instances with class index set
     * @throws Exception if the resource cannot be read or parsed
     */
    public Instances loadDatasetFromClasspath(String classpathResource) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathResource);
        try (InputStream is = resource.getInputStream()) {
            ArffLoader loader = new ArffLoader();
            loader.setSource(is);
            Instances data = loader.getDataSet();
            // Treat the last attribute as the class label
            data.setClassIndex(data.numAttributes() - 1);
            return data;
        }
    }

    // ─── Model Training ──────────────────────────────────────────────────

    /**
     * Trains a RandomForest classifier on the supplied dataset.
     *
     * @param data     fully labelled Instances with class index set
     * @param numTrees number of trees in the forest (default: 100)
     * @param seed     random seed for reproducibility
     * @return trained RandomForest ready for prediction
     * @throws Exception if training fails
     */
    public RandomForest trainRandomForest(Instances data, int numTrees, int seed)
            throws Exception {
        RandomForest rf = new RandomForest();
        rf.setNumIterations(numTrees);
        rf.setSeed(seed);
        // Use all available CPU cores for training
        rf.setNumExecutionSlots(Runtime.getRuntime().availableProcessors());
        rf.buildClassifier(data);
        return rf;
    }

    // ─── Model Evaluation ────────────────────────────────────────────────

    /**
     * Evaluates a classifier using stratified k-fold cross-validation
     * and returns a formatted report string.
     *
     * @param classifier trained or untrained classifier
     * @param data       the full dataset
     * @param folds      number of CV folds (typically 10)
     * @return multi-line evaluation report
     * @throws Exception if evaluation fails
     */
    public String evaluateWithCrossValidation(Classifier classifier,
                                              Instances data,
                                              int folds) throws Exception {
        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(classifier, data, folds, new Random(42));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== %d-Fold Cross-Validation ===%n", folds));
        sb.append(String.format("  Correctly Classified  : %.2f%%%n", eval.pctCorrect()));
        sb.append(String.format("  Incorrectly Classified: %.2f%%%n", eval.pctIncorrect()));
        sb.append(String.format("  Kappa Statistic       : %.4f%n",   eval.kappa()));
        sb.append(String.format("  Mean Absolute Error   : %.4f%n",   eval.meanAbsoluteError()));
        sb.append(String.format("  Root Mean Sq. Error   : %.4f%n",   eval.rootMeanSquaredError()));
        sb.append(String.format("  Total Instances       : %.0f%n",   eval.numInstances()));
        sb.append(System.lineSeparator());
        sb.append("=== Detailed Accuracy By Class ===").append(System.lineSeparator());
        sb.append(eval.toClassDetailsString());
        sb.append(System.lineSeparator());
        sb.append("=== Confusion Matrix ===").append(System.lineSeparator());
        sb.append(eval.toMatrixString());
        return sb.toString();
    }

    // ─── Model Persistence ───────────────────────────────────────────────

    /**
     * Serializes a trained Weka classifier to a file.
     *
     * @param model    any trained Weka Classifier
     * @param filePath absolute or relative path for the output .model file
     * @throws Exception if serialization fails
     */
    public void saveModel(Classifier model, String filePath) throws Exception {
        File file = new File(filePath);
        // Create parent directories if they don't exist
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        SerializationHelper.write(filePath, model);
    }

    /**
     * Deserializes a Weka classifier from a previously saved .model file.
     *
     * @param filePath path to the .model file
     * @return the deserialized Classifier
     * @throws Exception if deserialization fails or file not found
     */
    public Classifier loadModel(String filePath) throws Exception {
        return (Classifier) SerializationHelper.read(filePath);
    }

    /**
     * Returns true if a serialized model file already exists at the given path.
     *
     * @param filePath path to check
     * @return true if the file exists and is readable
     */
    public boolean modelExists(String filePath) {
        File f = new File(filePath);
        return f.exists() && f.isFile() && f.canRead();
    }
}
