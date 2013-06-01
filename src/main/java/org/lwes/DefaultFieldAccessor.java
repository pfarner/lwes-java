package org.lwes;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.lwes.util.EncodedString;

public class DefaultFieldAccessor implements FieldAccessor {
    private byte[]    array;
    private int       nameOffset, nameLength, encoding;
    private String    name;
    private FieldType type;
    private Object    value;
    
    public DefaultFieldAccessor() { }
    
    public DefaultFieldAccessor(byte[] array, int offset, int length, int encoding, FieldType type, Object value) {
        setNameBytes(array, offset, length, encoding);
        setType(type);
        setValue(value);
    }
    
    public byte[] getNameArray() {
        return array;
    }
    
    public int getNameOffset() {
      return nameOffset;
    }

    public int getNameLength() {
      return nameLength;
    }

    public String getName() {
        if (name==null) {
          name = EncodedString.bytesToString(array, nameOffset, nameLength,
              Event.ENCODING_STRINGS[encoding]);
        }
        return name;
    }

    protected void setNameBytes(byte[] array, int offset, int length, int encoding) {
        this.array      = array;
        this.nameOffset = offset;
        this.nameLength = length;
        this.encoding   = encoding;
        this.name       = null;
    }

    public FieldType getType() {
        return type;
    }

    protected void setType(FieldType type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    protected void setValue(Object value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object obj) {
      return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
