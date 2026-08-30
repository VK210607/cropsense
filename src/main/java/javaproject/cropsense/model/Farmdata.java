package javaproject.cropsense.model;

import jakarta.persistence.*;

@Entity
@Table(name = "farm_data")
public class FarmData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double nitrogen;
    private double phosphorus;
    private double potassium;
    private double temperature;
    private double humidity;
    private double ph;
    private double rainfall;
    private String predictedCrop;

    // Default constructor (required by JPA)
    public FarmData() {}

    // Full constructor
    public FarmData(double nitrogen, double phosphorus, double potassium,
                    double temperature, double humidity, double ph,
                    double rainfall, String predictedCrop) {
        this.nitrogen     = nitrogen;
        this.phosphorus   = phosphorus;
        this.potassium    = potassium;
        this.temperature  = temperature;
        this.humidity     = humidity;
        this.ph           = ph;
        this.rainfall     = rainfall;
        this.predictedCrop = predictedCrop;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public double getNitrogen() { return nitrogen; }
    public void setNitrogen(double nitrogen) { this.nitrogen = nitrogen; }
    public double getPhosphorus() { return phosphorus; }
    public void setPhosphorus(double phosphorus) { this.phosphorus = phosphorus; }
    public double getPotassium() { return potassium; }
    public void setPotassium(double potassium) { this.potassium = potassium; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public double getPh() { return ph; }
    public void setPh(double ph) { this.ph = ph; }
    public double getRainfall() { return rainfall; }
    public void setRainfall(double rainfall) { this.rainfall = rainfall; }
    public String getPredictedCrop() { return predictedCrop; }
    public void setPredictedCrop(String predictedCrop) { this.predictedCrop = predictedCrop; }
}