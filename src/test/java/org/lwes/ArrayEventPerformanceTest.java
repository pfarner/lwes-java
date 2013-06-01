package org.lwes;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.lwes.util.EncodedString;

public class ArrayEventPerformanceTest {
  private static final int NUM_EVENTS = 1000, NUM_PASSES = 10000;
  private ArrayEvent[]     events;
  private int              numFields;
  private ThreadMXBean     tmx;
  private long             t0;

  @Before
  public void before() {
    final RandomEventGenerator generator = new RandomEventGenerator();
    events = new ArrayEvent[NUM_EVENTS];
    numFields = 0;
    for (int i=0; i<NUM_EVENTS; ++i) {
      events[i] = new ArrayEvent();
      generator.fillRandomEvent(events[i]);
      events[i].setInt32("e_ox3_trax_time", 1300000000);
      events[i].setString("e_ox3_trax_id", "deadbeefdeadbeef");
      events[i].setInt64("e_id", 9);
      numFields += events[i].getNumEventAttributes();
    }
    tmx = ManagementFactory.getThreadMXBean();
    t0  = tmx.getCurrentThreadCpuTime();
  }
  
  @After
  public void after() {
    events = null;
    tmx    = null;
  }

  @Test @Ignore
  public void testFieldNotFound() {
    final String nonexistentFieldName = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    final byte[] key = EncodedString.getBytes(nonexistentFieldName, Event.ENCODING_STRINGS[Event.DEFAULT_ENCODING]);
    for (int p=0; p<NUM_PASSES; ++p) {
      for (ArrayEvent event : events) {
        event.get(key);
      }
    }
    final long dt = tmx.getCurrentThreadCpuTime() - t0;
    System.out.printf("Scanned %d events averaging %1.1f fields %d times in %f seconds, or %f ns/event, or %f ns/field\n",
        NUM_EVENTS, numFields/(double)NUM_EVENTS, NUM_PASSES,
        dt / 1000000000., dt / (double) (NUM_EVENTS * NUM_PASSES),
        dt / (double) (numFields * NUM_PASSES));
  }
  
  @Test
  public void oem() throws Exception {
    final OEM<TestStruct> oem = new OEM<TestStruct>(TestStruct.class);
    t0  = tmx.getCurrentThreadCpuTime();
    
    final TestStruct struct = new TestStruct();
    for (int p=0; p<NUM_PASSES; ++p) {
      for (ArrayEvent event : events) {
        oem.parseInto(event, struct);
      }
    }
    final long dt = tmx.getCurrentThreadCpuTime() - t0;
    System.out.printf("OEM unpacked %d events averaging %1.1f fields %d times in %f seconds, or %f ns/event, or %f ns/field\n",
        NUM_EVENTS, numFields/(double)NUM_EVENTS, NUM_PASSES,
        dt / 1000000000., dt / (double) (NUM_EVENTS * NUM_PASSES),
        dt / (double) (numFields * NUM_PASSES));
    System.out.println(struct);
  }
  
  @Test
  public void direct() throws Exception {
    for (Event event : events) {
      event.setInt32("e_ox3_trax_time", 1300000000);
      event.setString("e_ox3_trax_id", "deadbeefdeadbeef");
      event.setInt64("e_id", 9);
    }
    t0  = tmx.getCurrentThreadCpuTime();
    
    for (int p=0; p<NUM_PASSES; ++p) {
      for (ArrayEvent event : events) {
        event.getInt32("e_ox3_trax_time");
        event.getString("e_ox3_trax_id");
        event.getInt64("e_id");
      }
    }
    final long dt = tmx.getCurrentThreadCpuTime() - t0;
    System.out.printf("Gets unpacked %d events averaging %1.1f fields %d times in %f seconds, or %f ns/event, or %f ns/field\n",
        NUM_EVENTS, numFields/(double)NUM_EVENTS, NUM_PASSES,
        dt / 1000000000., dt / (double) (NUM_EVENTS * NUM_PASSES),
        dt / (double) (numFields * NUM_PASSES));
  }

  public static final class TestStruct {
    public int    e_ox3_trax_time;
    public String e_ox3_trax_id;
    public long   e_id;
    
    @Override
    public String toString() {
      return e_ox3_trax_time+"\t"+e_ox3_trax_id+"\t"+e_id;
    }
  }
}
