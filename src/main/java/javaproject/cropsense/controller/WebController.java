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

@Controller
public class WebController {

    private final CropPredictionService predictionService;
    private final FarmDataRepository farmDataRepository;

    public WebController(CropPredictionService predictionService,
                         FarmDataRepository farmDataRepository) {
        this.predictionService  = predictionService;
        this.farmDataRepository = farmDataRepository;
    }

    // Show the input form
    @GetMapping("/")
    public String showForm() {
        return "index";  // loads index.html
    }

    // Handle form submission
    @PostMapping("/predict")
    public String predict(
            @RequestParam double nitrogen,
            @RequestParam double phosphorus,
            @RequestParam double potassium,
            @RequestParam double temperature,
            @RequestParam double humidity,
            @RequestParam double ph,
            @RequestParam double rainfall,
            Model model) throws Exception {

        // Step 1: Build features object
        CropFeatures features = new CropFeatures(
                nitrogen, phosphorus, potassium,
                temperature, humidity, ph, rainfall);

        // Step 2: Get ML prediction
        PredictionResult result = predictionService.predict(features);

        // Step 3: Save to database
        FarmData record = new FarmData(
                nitrogen, phosphorus, potassium,
                temperature, humidity, ph, rainfall,
                result.getRecommendedCrop());
        farmDataRepository.save(record);

        // Step 4: Send result to result.html
        model.addAttribute("crop",       result.getRecommendedCrop());
        model.addAttribute("confidence", String.format("%.1f", result.getConfidence() * 100));
        model.addAttribute("topCrops",   result.getTopCrops().subList(0, Math.min(3, result.getTopCrops().size())));
        model.addAttribute("nitrogen",   nitrogen);
        model.addAttribute("rainfall",   rainfall);
        model.addAttribute("ph",         ph);

        return "result";  // loads result.html
    }
}