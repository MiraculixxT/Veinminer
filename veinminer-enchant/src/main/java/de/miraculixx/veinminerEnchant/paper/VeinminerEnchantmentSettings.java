package de.miraculixx.veinminerEnchant.paper;

public class VeinminerEnchantmentSettings {
    private int minCost = 15;
    private int maxCost = 65;
    private int anvilCost = 7;

    public VeinminerEnchantmentSettings() {

    }

    public void setMinCost(int minCost) {
        this.minCost = minCost;
    }

    public void setMaxCost(int maxCost) {
        this.maxCost = maxCost;
    }

    public void setAnvilCost(int anvilCost) {
        this.anvilCost = anvilCost;
    }

    public int getMinCost() {
        return minCost;
    }

    public int getMaxCost() {
        return maxCost;
    }

    public int getAnvilCost() {
        return anvilCost;
    }
}
