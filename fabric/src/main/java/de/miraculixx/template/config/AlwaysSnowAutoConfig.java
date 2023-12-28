package de.miraculixx.template.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

/**
 * Remove if useConfig is false in gradle.properties
 * <p>
 * Wiki: <a href="https://shedaniel.gitbook.io/cloth-config/auto-config/creating-a-config-class">...</a>
 * <p>
 * Only use on client side mods
 */
@Config(name = "firstTemplate")
public class AlwaysSnowAutoConfig implements ConfigData {

    public boolean value1 = true;
    public String value2 = "Hey";
}
