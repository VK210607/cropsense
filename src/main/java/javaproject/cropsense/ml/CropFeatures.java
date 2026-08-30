package javaproject.cropsense.ml;

/**
 * Input DTO for the CropSense ML prediction engine.
 *
 * Represents the 7 soil and climate features used to recommend
 * the most suitable crop for a given field.
 *
 * Feature ranges (based on training data):
 *   N           : 0   – 140   (kg/ha, soil nitrogen ratio)
 *   P           : 5   – 145   (kg/ha, soil phosphorus ratio)
 *   K           : 5   – 210   (kg/ha, soil potassium ratio)
 *   temperature : 8   – 43    (°C, average ambient temperature)
 *   humidity    : 14  – 99    (%, relative humidity)
 *   ph          : 3.5 – 9.9   (soil pH)
 *   rainfall    : 20  – 300   (mm, annual rainfall)
 */
public class CropFeatures {

    /** Soil Nitrogen content (kg/ha) */
    private double nitrogenContent;

    /** Soil Phosphorus content (kg/ha) */
    private double phosphorusContent;

    /** Soil Potassium content (kg/ha) */
    private double potassiumContent;

    /** Average ambient temperature (°C) */
    private double temperature;

    /** Relative humidity (%) */
    private double humidity;

    /** Soil pH level */
    private double phLevel;

    /** Annual rainfall (mm) */
    private double rainfall;

    // ─── Constructors ────────────────────────────────────────────────────

    public CropFeatures() {}

    public CropFeatures(double nitrogenContent, double phosphorusContent,
                        double potassiumContent, double temperature,
                        double humidity, double phLevel, double rainfall) {
        this.nitrogenContent   = nitrogenContent;
        this.phosphorusContent = phosphorusContent;
        this.potassiumContent  = potassiumContent;
        this.temperature       = temperature;
        this.humidity          = humidity;
        this.phLevel           = phLevel;
        this.rainfall          = rainfall;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────

    public double getNitrogenContent()   { return nitrogenContent; }
    public void setNitrogenContent(double n) { this.nitrogenContent = n; }

    public double getPhosphorusContent()   { return phosphorusContent; }
    public void setPhosphorusContent(double p) { this.phosphorusContent = p; }

    public double getPotassiumContent()   { return potassiumContent; }
    public void setPotassiumContent(double k) { this.potassiumContent = k; }

    public double getTemperature()   { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity()   { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getPhLevel()   { return phLevel; }
    public void setPhLevel(double phLevel) { this.phLevel = phLevel; }

    public double getRainfall()   { return rainfall; }
    public void setRainfall(double rainfall) { this.rainfall = rainfall; }

    @Override
    public String toString() {
        return String.format(
            "CropFeatures{N=%.1f, P=%.1f, K=%.1f, temp=%.1f°C, " +
            "humidity=%.1f%%, pH=%.2f, rainfall=%.1fmm}",
            nitrogenContent, phosphorusContent, potassiumContent,
            temperature, humidity, phLevel, rainfall
        );
    }
}
