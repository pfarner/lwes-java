package org.lwes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwes.util.EncodedString;

public final class OEM<T> {
  private final AnnotatedField[] fields;

  public OEM(Class<T> cl) {
    final List<AnnotatedField> fields = new ArrayList<AnnotatedField>();
    for (Field field : cl.getFields()) {
      final byte[] nameBytes =
          EncodedString.getBytes(field.getName(), Event.ENCODING_STRINGS[Event.DEFAULT_ENCODING]);
      fields.add(new AnnotatedField(nameBytes, field));
    }
    this.fields = fields.toArray(new AnnotatedField[fields.size()]);
  }
  
  public void parseInto(Event event, Object struct) throws Exception {
    int remaining = fields.length;
    for (FieldAccessor fa : event) {
      nextField:
      for (AnnotatedField field : fields) {
        if (field.nameBytes.length != fa.getNameLength()) continue;
        final byte[] nameBytes = fa.getNameArray();
        for (int i=0, j=fa.getNameOffset(), n=field.nameBytes.length; i<n; ++i, ++j) {
          if (field.nameBytes[i] != nameBytes[j]) continue nextField;
        }
        field.field.set(struct, fa.getValue());
        --remaining;
        if (remaining==0) return;
      }
    }
  }
  
//  public static void main(String... args) throws Exception {
//    final OEM<TestStruct> oem = new OEM<TestStruct>(TestStruct.class);
//    final ArrayEvent event = new ArrayEvent("OX3::Click");
//    event.setInt32("e_ox3_trax_time", 1300000000);
//    event.setString("e_ox3_trax_id", "deadbeefdeadbeef");
//    event.setInt64("e_id", 9);
//    
//    final TestStruct struct = new TestStruct();
//    oem.parseInto(event, struct);
//    
//    System.out.println(struct);
//  }
  
  private static final class AnnotatedField {
    public final byte[] nameBytes;
    public final Field  field;
    
    public AnnotatedField(byte[] nameBytes, Field  field) {
      this.nameBytes = nameBytes;
      this.field     = field;
    }
  }
}
