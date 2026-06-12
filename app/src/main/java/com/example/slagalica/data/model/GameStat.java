package com.example.slagalica.data.model;

/**
 * One per-game average row on the profile (Sunny pop .stat-row): the game name,
 * a points-range label (e.g. "30–42"), a 0–100 bar fill and which game colour
 * (g0..g3) to paint the bar. These are DB-backed values surfaced through
 * {@link UserStats}.
 */
public class GameStat {

    private final String name;
    private final String valueLabel;
    private final int percent;
    private final int colorIndex; // 0=g0, 1=g1, 2=g2, 3=g3

    public GameStat(String name, String valueLabel, int percent, int colorIndex) {
        this.name = name;
        this.valueLabel = valueLabel;
        this.percent = percent;
        this.colorIndex = colorIndex;
    }

    public String getName() { return name; }
    public String getValueLabel() { return valueLabel; }
    public int getPercent() { return percent; }
    public int getColorIndex() { return colorIndex; }
}
