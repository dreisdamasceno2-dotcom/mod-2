package com.example.melhorias.client;

import com.example.melhorias.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Corretor local de digitação para a busca de itens. Roda 100% offline.
 *
 * Para cada palavra digitada que NÃO existe em nenhum nome de item, procura a palavra mais
 * parecida (nomes no idioma do jogo + ids em inglês, inclusive de outros mods) e troca por ela
 * antes da busca normal do jogo. Assim a busca original continua mostrando todos os resultados.
 * Trata: letras trocadas, faltando, repetidas, falta de acento e palavras parciais.
 */
public final class FuzzySearch {
    private FuzzySearch() {}

    private static final class Word {
        final String norm;
        String orig;
        int freq = 1;
        Word(String norm, String orig) { this.norm = norm; this.orig = orig; }
    }

    private static String cacheKey = "";
    private static Map<String, Word> vocab = new HashMap<>();
    private static String nameCorpus = "";
    private static String idCorpus = "";

    /** Nunca lança exceção: se algo falhar, devolve a busca original. */
    public static String correct(String query) {
        try {
            return doCorrect(query);
        } catch (Throwable t) {
            return query;
        }
    }

    private static String doCorrect(String query) {
        if (query == null || query.isEmpty() || !ModConfig.get().fuzzySearch) return query;
        char first = query.charAt(0);
        if (first == '#' || first == '$' || first == '@' || first == '/') return query; // tags etc.
        if (query.indexOf(':') >= 0) return query;

        ensureIndex();

        String[] parts = query.split(" ", -1);
        boolean changed = false;
        for (int i = 0; i < parts.length; i++) {
            String fixed = correctToken(parts[i]);
            if (!fixed.equals(parts[i])) {
                parts[i] = fixed;
                changed = true;
            }
        }
        if (!changed) return query;

        String result = String.join(" ", parts);
        // Se só existe como id em inglês (ex.: "diamond sword"), usa o formato do id.
        String lower = result.toLowerCase(Locale.ROOT);
        if (!nameCorpus.contains(lower) && idCorpus.contains(lower.replace(' ', '_'))) {
            return lower.replace(' ', '_');
        }
        return result;
    }

    private static String correctToken(String tok) {
        String t = tok.toLowerCase(Locale.ROOT);
        if (t.length() < 4) return tok;
        for (int i = 0; i < t.length(); i++) {
            if (!Character.isLetter(t.charAt(i))) return tok;
        }
        // Já existe em algum nome: a busca normal resolve.
        if (nameCorpus.contains(t) || idCorpus.contains(t)) return tok;

        String tn = norm(t);

        // 1) Faltou acento / palavra parcial sem acento
        Word best = null;
        for (Word w : vocab.values()) {
            if (w.norm.startsWith(tn)) best = better(best, w);
        }
        if (best != null) {
            return best.orig.substring(0, Math.min(best.orig.length(), t.length()));
        }

        // 2) Erro de digitação
        int maxD = tn.length() >= 9 ? 3 : tn.length() >= 6 ? 2 : 1;
        int bestD = Integer.MAX_VALUE;
        Word bw = null;
        for (Word w : vocab.values()) {
            if (w.norm.length() < tn.length() - maxD) continue;
            int d = osa(tn, w.norm);
            if (tn.length() >= 5 && w.norm.length() > tn.length()) {
                d = Math.min(d, osa(tn, w.norm.substring(0, tn.length()))); // palavra incompleta
            }
            if (d > maxD) continue;
            if (d < bestD) {
                bestD = d;
                bw = w;
            } else if (d == bestD && bw != null) {
                bw = better(bw, w);
            }
        }
        return bw != null ? bw.orig : tok;
    }

    private static Word better(Word a, Word b) {
        if (a == null) return b;
        if (b.freq > a.freq) return b;
        if (b.freq == a.freq && b.norm.length() < a.norm.length()) return b;
        return a;
    }

    // ------------------------------------------------------------------ índice

    private static void ensureIndex() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String key = mc.options.language + "|" + Registries.ITEM.size();
        if (key.equals(cacheKey)) return;

        Map<String, Word> map = new HashMap<>();
        StringBuilder names = new StringBuilder("\n");
        StringBuilder ids = new StringBuilder("\n");

        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            String name = item.getName().getString().toLowerCase(Locale.ROOT);
            Identifier id = Registries.ITEM.getId(item);
            names.append(name).append('\n');
            ids.append(id.getPath()).append('\n');
            addWords(map, name);
            addWords(map, id.getPath().replace('_', ' '));
        }

        vocab = map;
        nameCorpus = names.toString();
        idCorpus = ids.toString();
        cacheKey = key;
    }

    private static void addWords(Map<String, Word> map, String text) {
        for (String tok : text.split("[^\\p{L}]+")) {
            if (tok.length() < 3) continue;
            String n = norm(tok);
            Word w = map.get(n);
            if (w == null) {
                map.put(n, new Word(n, tok));
            } else {
                w.freq++;
                if (w.orig.equals(w.norm) && !tok.equals(n)) w.orig = tok; // guarda a versão com acento
            }
        }
    }

    private static String norm(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Distância de edição que também conta letras trocadas de lugar como 1 erro. */
    private static int osa(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int v = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    v = Math.min(v, d[i - 2][j - 2] + 1);
                }
                d[i][j] = v;
            }
        }
        return d[n][m];
    }
}
