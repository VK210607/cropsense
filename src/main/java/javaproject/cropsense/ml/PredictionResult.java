package javaproject.cropsense.ml;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Output DTO returned by the CropSense ML prediction engine.
 *
 * Contains:
 *  - The top recommended crop name
 *  - Confidence score (0.0 – 1.0) for that recommendation
 *  - Full probability distribution across all 22 supported crops
 *    (sorted descending by probability)
 *  - A ranked list of the top-N crop names for quick access
 */
public class PredictionResult {

    /** The crop with the highest predicted probability. */
    private String recommendedCrop;

    /** Confidence (probability) of the recommended crop (0.0 – 1.0). */
    private double confidence;

    /**
     * Probability distribution across all crop classes, sorted descending.
     * Keys = crop name, Values = probability (0.0 – 1.0, sum ≈ 1.0).
     */
    private Map<String, Double> cropProbabilities;

    /**
     * Ranked list of top-N crop names (ordered by probability).
     * Populated by the service based on caller's requested count.
     */
    private List<String> topCrops;

    // ─── Constructors ────────────────────────────────────────────────────

    public PredictionResult() {}

    public PredictionResult(String recommendedCrop,
                            double confidence,
                            Map<String, Double> cropProbabilities,
                            List<String> topCrops) {
        this.recommendedCrop   = recommendedCrop;
        this.confidence        = confidence;
        this.cropProbabilities = cropProbabilities;
        this.topCrops          = topCrops;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────

    public String getRecommendedCrop() { return recommendedCrop; }
    public void setRecommendedCrop(String recommendedCrop) {
        this.recommendedCrop = recommendedCrop;
    }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public Map<String, Double> getCropProbabilities() {
        return Collections.unmodifiableMap(cropProbabilities);
    }
    public void setCropProbabilities(Map<String, Double> cropProbabilities) {
        this.cropProbabilities = cropProbabilities;
    }

    public List<String> getTopCrops() {
        return Collections.unmodifiableList(topCrops);
    }
    public void setTopCrops(List<String> topCrops) { this.topCrops = topCrops; }

    // ─── Utility ─────────────────────────────────────────────────────────

    /**
     * Returns a human-readable summary of the prediction.
     */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("✅ Recommended Crop : %s (%.1f%% confidence)%n",
                recommendedCrop, confidence * 100));
        sb.append("🌾 Top Alternatives :\n");
        int rank = 1;
        for (Map.Entry<String, Double> entry : cropProbabilities.entrySet()) {
            if (rank > 5) break;
            sb.append(String.format("   %d. %-15s %.2f%%%n",
                    rank++, entry.getKey(), entry.getValue() * 100));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("PredictionResult{crop='%s', confidence=%.4f}",
                recommendedCrop, confidence);
    }
}
