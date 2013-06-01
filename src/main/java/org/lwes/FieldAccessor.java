package org.lwes;

public interface FieldAccessor {
    byte[]    getNameArray();
    int       getNameOffset();
    int       getNameLength();
    String    getName();
    FieldType getType();
    Object    getValue();
}
