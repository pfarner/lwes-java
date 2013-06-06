package org.lwes;

public interface FieldAccessor {
    String    getName();
    byte[]    getNameBytesArray();
    int       getNameBytesOffset();
    int       getNameBytesLength();
    FieldType getType();
    Object    getValue();
}
