package com.example.melhorias;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuração salva em config/melhorias.json (criada automaticamente na primeira execução). */
public class ModConfig {
    // ---- Aldeões
    /** false = só atualiza ofertas ainda não travadas por negociação. */
    public boolean allowRefreshAfterTrade = false;

    // ---- Busca
    public boolean fuzzySearch = true;

    // ---- Barcos (1.0 = comportamento original)
    public boolean boatImprovements = true;
    public double boatMaxSpeed = 1.15;      // multiplicador da velocidade máxima
    public double boatSteering = 1.0;       // sensibilidade da direção
    public double boatAcceleration = 1.3;   // aceleração
    public double boatDeceleration = 1.0;   // quanto o barco freia ao soltar o acelerador
    public double boatDrift = 0.35;         // 0 = gruda no rumo, 1 = derrapa como o original

    // ---- Pesca
    public boolean fishingUi = true;

    private static ModConfig instance = new ModConfig();

    public static ModConfig get() { return instance; }

    public static void load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path path = FabricLoader.getInstance().getConfigDir().resolve("melhorias.json");
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path)) {
                    ModConfig c = gson.fromJson(r, ModConfig.class);
                    if (c != null) instance = c;
                }
            }
            instance.sanitize();
            try (Writer w = Files.newBufferedWriter(path)) {
                gson.toJson(instance, w);
            }
        } catch (Exception e) {
            MelhoriasMod.LOGGER.error("Falha ao ler/gravar a configuração", e);
        }
    }

    private void sanitize() {
        boatMaxSpeed = clamp(boatMaxSpeed, 0.5, 2.0);
        boatSteering = clamp(boatSteering, 0.5, 2.5);
        boatAcceleration = clamp(boatAcceleration, 0.5, 3.0);
        boatDeceleration = clamp(boatDeceleration, 0.3, 3.0);
        boatDrift = clamp(boatDrift, 0.0, 1.0);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
