package com.yukar1n.x_tracer.server;

import com.yukar1n.x_tracer.ClientConfig;
import com.yukar1n.x_tracer.storage.ContainerReference;
import com.yukar1n.x_tracer.storage.TerminalProfile;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ScanRequest {
    public final UUID playerId;
    public final int radius;
    public final String mode;
    public final List<String> blockTargets;
    public final List<String> oreTargets;
    public final String gtTarget;
    public final boolean drop;
    public final boolean remoteInput;
    public final List<ContainerReference> bindings;

    private ScanRequest(UUID playerId, int radius, String mode, List<String> blockTargets,
                        List<String> oreTargets, String gtTarget, boolean drop,
                        boolean remoteInput, List<ContainerReference> bindings) {
        this.playerId = playerId;
        this.radius = radius;
        this.mode = mode;
        this.blockTargets = Collections.unmodifiableList(new ArrayList<>(blockTargets));
        this.oreTargets = Collections.unmodifiableList(new ArrayList<>(oreTargets));
        this.gtTarget = gtTarget;
        this.drop = drop;
        this.remoteInput = remoteInput;
        this.bindings = Collections.unmodifiableList(new ArrayList<>(bindings));
    }

    public static ScanRequest from(UUID playerId, ClientConfig config) {
        return new ScanRequest(playerId, config.radius, config.curMatchMode,
            config.targetList, config.targetOreNameList, config.targetGtName,
            "True".equals(config.getDrop), false, Collections.<ContainerReference>emptyList());
    }

    public static ScanRequest from(UUID playerId, ClientConfig config, ItemStack terminal) {
        TerminalProfile.initializeFromConfig(terminal, config);
        return new ScanRequest(playerId, TerminalProfile.radius(terminal, config), config.curMatchMode,
            config.targetList, config.targetOreNameList, config.targetGtName,
            TerminalProfile.drop(terminal, config), TerminalProfile.remoteInput(terminal),
            TerminalProfile.pruneBindings(terminal));
    }
}
