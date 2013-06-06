package org.lwes.oem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.lwes.Event;
import org.lwes.FieldAccessor;
import org.lwes.util.CharacterEncoding;
import org.lwes.util.EncodedString;

/**
 * A very common pattern for reading an event is to pull out fields into
 * variables or a simple object. ObjectEventMapper simplifies this process
 * and also speeds up the scan, as it is all done in one pass.
 */
public final class ObjectEventMapper<T> {
    private static final CharacterEncoding ENCODING = Event.ENCODING_STRINGS[Event.DEFAULT_ENCODING];
    private final T                        object;
    private final AnnotatedField[]         fields;
    
    public static <T> ObjectEventMapper<T> construct(T o) {
        return new ObjectEventMapper<T>(o);
    }

    private ObjectEventMapper(T o) {
        this.object = o;
        
        final List<AnnotatedField> list = new ArrayList<AnnotatedField>();
        for (Field field : object.getClass().getFields()) {
            final LWESField annotation = field.getAnnotation(LWESField.class);
            final String name = annotation==null ? field.getName() : annotation.name();
            final byte[] nameBytes = EncodedString.getBytes(name, ENCODING);
            list.add(new AnnotatedField(nameBytes, field));
        }
        this.fields = list.toArray(new AnnotatedField[list.size()]);
    }
    
    public void load(Event event) throws Exception {
        clear();
        int remaining = fields.length;
        for (FieldAccessor fa : event) {
            for (AnnotatedField field : fields) {
                if (! isNameMatch(field.nameBytes, fa)) continue;
                field.field.set(object, fa.getValue());
                --remaining;
                if (remaining==0) return;
            }
        }
    }

    private void clear() throws Exception {
        for (AnnotatedField field : fields) {
            field.clear(object);
        }
    }

    private boolean isNameMatch(byte[] nameBytes, FieldAccessor fa) {
        if (nameBytes.length != fa.getNameBytesLength()) return false;
        final byte[] fieldNameBytes = fa.getNameBytesArray();
        for (int i=0, j=fa.getNameBytesOffset(), n=nameBytes.length; i<n; ++i, ++j) {
            if (nameBytes[i] != fieldNameBytes[j]) return false;
        }
        return true;
    }

    private static final class AnnotatedField {
        public final byte[] nameBytes;
        public final Field  field;
        public final Object defaultValue;
        
        public AnnotatedField(byte[] nameBytes, Field  field) {
            this.nameBytes = nameBytes;
            this.field     = field;
            field.setAccessible(true);
            if (field.getType().isPrimitive()) {
                if (field.getType() == Byte.TYPE) {
                    this.defaultValue = (byte) 0;
                } else if (field.getType() == Short.TYPE) { 
                    this.defaultValue = (short) 0;
                } else if (field.getType() == Integer.TYPE) {
                    this.defaultValue = 0;
                } else if (field.getType() == Long.TYPE) { 
                    this.defaultValue = (long) 0;
                } else if (field.getType() == Float.TYPE) { 
                    this.defaultValue = 0F;
                } else if (field.getType() == Double.TYPE) { 
                    this.defaultValue = 0.0;
                } else if (field.getType() == Boolean.TYPE) { 
                    this.defaultValue = false;
                } else {
                    throw new UnsupportedOperationException("Unexpected type for "+field+": "+field.getType());
                }
            } else {
                this.defaultValue = null;
            }
        }
        
        public void clear(Object object) throws Exception {
            this.field.set(object, defaultValue);
        }
    }
}
