package javaproject.cropsense.controller;

import javaproject.cropsense.ml.CropFeatures;
import javaproject.cropsense.ml.CropPredictionService;
import javaproject.cropsense.ml.PredictionResult;
import javaproject.cropsense.model.FarmData;
import javaproject.cropsense.repository.FarmDataRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class WebController {

    private final CropPredictionService predictionService;
    private final FarmDataRepository farmDataRepository;

    public WebController(CropPredictionService predictionService,
                         FarmDataRepository farmDataRepository) {
        this.predictionService  = predictionService;
        this.farmDataRepository = farmDataRepository;
    }

    // ── Landing Dashboard ──────────────────────────────────────────────
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("totalPredictions", farmDataRepository.count());
        return "index";
    }

    // ── Prediction Form ────────────────────────────────────────────────
    @GetMapping("/predict")
    public String showPredictForm() {
        return "predict";
    }

    // ── Handle Farmer-Friendly Form Submission ─────────────────────────
    @PostMapping("/predict")
    public String predict(
            @RequestParam String soilColor,
            @RequestParam String soilType,
            @RequestParam String soilPh,
            @RequestParam double temperature,
            @RequestParam String humidityLevel,
            @RequestParam String rainfallLevel,
            Model model) throws Exception {

        // ── Convert farmer inputs to ML model values ──────────────────

        // Nitrogen: based on soil color (darker = more organic matter = more N)
        double nitrogen = switch (soilColor) {
            case "light"      -> 20.0;   // Light / Sandy coloured
            case "medium"     -> 55.0;   // Brown / Loamy
            case "dark_brown" -> 85.0;   // Dark Brown
            case "black"      -> 120.0;  // Black / Very rich
            default           -> 55.0;
        };

        // Phosphorus: based on soil type
        double phosphorus = switch (soilType) {
            case "sandy"        -> 18.0;
            case "loamy"        -> 55.0;
            case "clay"         -> 85.0;
            case "black_cotton" -> 110.0;
            case "red"          -> 35.0;
            default             -> 55.0;
        };

        // Potassium: also based on soil type
        double potassium = switch (soilType) {
            case "sandy"        -> 15.0;
            case "loamy"        -> 55.0;
            case "clay"         -> 90.0;
            case "black_cotton" -> 140.0;
            case "red"          -> 42.0;
            default             -> 55.0;
        };

        // pH: based on soil reaction
        double ph = switch (soilPh) {
            case "acidic"            -> 4.8;
            case "slightly_acidic"   -> 5.8;
            case "neutral"           -> 6.8;
            case "slightly_alkaline" -> 7.5;
            case "alkaline"          -> 8.5;
            default                  -> 6.8;
        };

        // Humidity: from farmer's feel description
        double humidity = switch (humidityLevel) {
            case "dry"       -> 30.0;
            case "moderate"  -> 55.0;
            case "humid"     -> 78.0;
            case "very_humid"-> 92.0;
            default          -> 55.0;
        };

        // Rainfall: annual pattern
        double rainfall = switch (rainfallLevel) {
            case "very_low"  -> 30.0;
            case "low"       -> 80.0;
            case "moderate"  -> 140.0;
            case "high"      -> 210.0;
            case "very_high" -> 270.0;
            default          -> 140.0;
        };

        // ── Run ML Prediction ─────────────────────────────────────────
        CropFeatures features = new CropFeatures(
                nitrogen, phosphorus, potassium,
                temperature, humidity, ph, rainfall);

        PredictionResult result = predictionService.predict(features);

        // ── Save to Database ──────────────────────────────────────────
        FarmData record = new FarmData(
                nitrogen, phosphorus, potassium,
                temperature, humidity, ph, rainfall,
                result.getRecommendedCrop());
        farmDataRepository.save(record);

        // ── Friendly label helpers for result page ────────────────────
        String soilColorLabel = switch (soilColor) {
            case "light"      -> "Light / Sandy";
            case "medium"     -> "Brown / Loamy";
            case "dark_brown" -> "Dark Brown";
            case "black"      -> "Black / Rich";
            default -> soilColor;
        };
        String soilTypeLabel = switch (soilType) {
            case "sandy"        -> "Sandy Soil";
            case "loamy"        -> "Loamy Soil";
            case "clay"         -> "Clay Soil";
            case "black_cotton" -> "Black Cotton Soil";
            case "red"          -> "Red Soil";
            default -> soilType;
        };
        String soilPhLabel = switch (soilPh) {
            case "acidic"            -> "Acidic";
            case "slightly_acidic"   -> "Slightly Acidic";
            case "neutral"           -> "Neutral";
            case "slightly_alkaline" -> "Slightly Alkaline";
            case "alkaline"          -> "Alkaline";
            default -> soilPh;
        };
        String humidityLabel = switch (humidityLevel) {
            case "dry"        -> "Dry";
            case "moderate"   -> "Moderate";
            case "humid"      -> "Humid";
            case "very_humid" -> "Very Humid";
            default -> humidityLevel;
        };
        String rainfallLabel = switch (rainfallLevel) {
            case "very_low"  -> "Very Low (Arid)";
            case "low"       -> "Low";
            case "moderate"  -> "Moderate";
            case "high"      -> "High";
            case "very_high" -> "Very Heavy (Monsoon)";
            default -> rainfallLevel;
        };

        // ── Pass to result.html ───────────────────────────────────────
        model.addAttribute("crop",           result.getRecommendedCrop());
        model.addAttribute("confidence",     String.format("%.1f", result.getConfidence() * 100));
        model.addAttribute("topCrops",       result.getTopCrops().subList(0, Math.min(3, result.getTopCrops().size())));
        model.addAttribute("soilColorLabel", soilColorLabel);
        model.addAttribute("soilTypeLabel",  soilTypeLabel);
        model.addAttribute("soilPhLabel",    soilPhLabel);
        model.addAttribute("temperature",    temperature);
        model.addAttribute("humidityLabel",  humidityLabel);
        model.addAttribute("rainfallLabel",  rainfallLabel);

        return "result";
    }

    // ── Prediction History ─────────────────────────────────────────────
    @GetMapping("/history")
    public String history(Model model) {
        List<FarmData> records = farmDataRepository.findAll();
        model.addAttribute("records",     records);
        model.addAttribute("totalCount",  records.size());
        return "history";
    }
}