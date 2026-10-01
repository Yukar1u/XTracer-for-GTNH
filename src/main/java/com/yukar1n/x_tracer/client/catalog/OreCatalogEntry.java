package com.yukar1n.x_tracer.client.catalog;

public class OreCatalogEntry {
    public final String name;
    public final String matchKey;

    public OreCatalogEntry(String name) {
        this.name = name;
        this.matchKey = com.yukar1n.x_tracer.SelectorLogic.matchKey(name);
    }

    public String searchText() {
        return matchKey;
    }
}
