package com.yukar1n.x_tracer;

import java.util.Arrays;
import net.minecraftforge.oredict.OreDictionary;

public final class OreNames {
    private OreNames() {}

    public static String resolve(String value) {
        String name = SelectorLogic.canonical(value, Arrays.asList(OreDictionary.getOreNames()));
        // Query only existing original registration names; getOres can register unknown names.
        return name != null && OreDictionary.doesOreNameExist(name)
            && !OreDictionary.getOres(name).isEmpty() ? name : null;
    }
}
