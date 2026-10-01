package com.yukar1n.x_tracer.client.catalog;

public class OreCatalogEntry {
    public final String name;

    public OreCatalogEntry(String name) {
        this.name = name;
    }

    public String searchText() {
        return name.toLowerCase();
    }
}
