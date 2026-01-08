package org.atypical.carabassa.core.model.enums;

import com.drew.imaging.FileType;

import java.util.HashMap;
import java.util.Map;

public enum ItemType {

    IMAGE("I"), VIDEO("V");

    private static final Map<String, ItemType> codes = new HashMap<>();
    private static final Map<String, ItemType> names = new HashMap<>();

    static {
        for (ItemType type : ItemType.values()) {
            codes.put(type.getCode(), type);
            names.put(type.normalized(), type);
        }
    }

    private final String code;

    ItemType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ItemType fromCode(String code) {
        return codes.get(code);
    }

    public static ItemType fromFileType(FileType fileType) {
        if (fileType.getMimeType() != null) {
            String name = fileType.getMimeType().split("/")[0];
            return names.get(name);
        } else {
            return null;
        }
    }

    public String normalized() {
        return this.name().toLowerCase();
    }

}
