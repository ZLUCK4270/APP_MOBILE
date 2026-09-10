package com.example.android.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResiduoClassifier {

    public static class ClassificationResult {
        public String categoria;
        public String propiedad;
        
        public ClassificationResult(String categoria, String propiedad) {
            this.categoria = categoria;
            this.propiedad = propiedad;
        }
    }

    private static final Map<String, List<String>> KNOWLEDGE_BASE = new HashMap<>();

    static {
        KNOWLEDGE_BASE.put("Plásticos Peligrosos", Arrays.asList("plastic", "bottle", "cup", "container", "jug"));
        KNOWLEDGE_BASE.put("Metales Pesados", Arrays.asList("metal", "can", "aluminum", "tin", "steel", "iron"));
        KNOWLEDGE_BASE.put("Cartón Industrial", Arrays.asList("cardboard", "box", "paper", "carton"));
        KNOWLEDGE_BASE.put("Químicos", Arrays.asList("spray", "liquid", "cleaner", "chemical", "bottle", "toxic", "detergent", "soap"));
        KNOWLEDGE_BASE.put("Biológicos", Arrays.asList("syringe", "medical", "glove", "mask", "blood", "bandage"));
    }

    public static ClassificationResult classify(String mlLabel) {
        if (mlLabel == null || mlLabel.isEmpty()) return null;
        String label = mlLabel.toLowerCase();

        // 1. Mapear a Categoría
        String matchedCategory = null;
        for (Map.Entry<String, List<String>> entry : KNOWLEDGE_BASE.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (label.contains(keyword)) {
                    matchedCategory = entry.getKey();
                    break;
                }
            }
            if (matchedCategory != null) break;
        }

        if (matchedCategory == null) return null; // Unrecognized

        // 2. Asignar Propiedad (Reciclable, Tóxico, Peligroso)
        String property = "";
        switch (matchedCategory) {
            case "Plásticos Peligrosos": // Fallback if regular plastic
                property = "♻️ Reciclable"; // Plastic is generally recyclable
                break;
            case "Metales Pesados":
                property = "♻️ Reciclable";
                break;
            case "Cartón Industrial":
                property = "♻️ Reciclable";
                break;
            case "Químicos":
                property = "⚠️ Producto Tóxico";
                break;
            case "Biológicos":
                property = "☣️ Peligroso";
                break;
            default:
                property = "Desconocido";
        }

        return new ClassificationResult(matchedCategory, property);
    }
}
