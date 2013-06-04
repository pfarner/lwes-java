/*======================================================================*
 * Copyright (c) 2010, Frank Maritato All rights reserved.              *
 *                                                                      *
 * Licensed under the New BSD License (the "License"); you may not use  *
 * this file except in compliance with the License.  Unless required    *
 * by applicable law or agreed to in writing, software distributed      *
 * under the License is distributed on an "AS IS" BASIS, WITHOUT        *
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.     *
 * See the License for the specific language governing permissions and  *
 * limitations under the License. See accompanying LICENSE file.        *
 *======================================================================*/

package org.lwes;
/**
 * @author fmaritato
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.lwes.db.EventTemplateDB;
import org.lwes.util.IPAddress;

import junit.framework.Assert;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class EventTest {

    private EventTemplateDB eventTemplate;
    
    protected abstract Event createEvent();
    
    protected final Event createEvent(String name) {
        final Event event = createEvent();
        event.setEventName(name);
        return event;
    }

    @Before
    public void setUp() {
        eventTemplate = new EventTemplateDB();
        eventTemplate.setESFFile(new File("src/test/java/org/lwes/EventTest.esf"));
        eventTemplate.initialize();
    }

    @Test
    public void testRemove() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        evt.setString("test", "value");
        assertTrue(evt.isSet("test"));
        assertEquals("value", evt.get("test"));
        evt.setString("test", null);
        assertFalse(evt.isSet("test"));
        assertNull(evt.get("test"));
    }

    @Test
    public void testGetAttributeNames() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        evt.setString("str", "string");

        boolean success = false;
        Enumeration<String> en = evt.getEventAttributeNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement();
            if ("str".equals(name)) {
                success = true;
            }
        }
        assertTrue(success);
    }

    @Test
    public void testGetInetAddress() throws EventSystemException, UnknownHostException {
        Event evt = createEvent();
        evt.setEventName("Test");
        final InetAddress localhost = InetAddress.getLocalHost();
        evt.setIPAddress("ip", localhost);
        InetAddress a = evt.getInetAddress("ip");
        assertNotNull(a);
        assertEquals(localhost, a);
    }

    @Test
    public void testGetInetAddressAsBytes() throws EventSystemException, UnknownHostException {
        Event evt = createEvent();
        evt.setEventName("Test");
        final InetAddress localhost = InetAddress.getLocalHost();
        evt.setByteArray("ip", localhost.getAddress());
        byte[] iparr = evt.getByteArray("ip");
        InetAddress a = InetAddress.getByAddress(iparr);
        assertNotNull(a);
        assertEquals(localhost, a);
    }

    @Test
    public void testIsSet() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        assertFalse(evt.isSet("notset"));

        evt.setInt32("set", 32);
        assertTrue(evt.isSet("set"));
    }

    @Test
    public void testToString() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        evt.setInt32("set", 32);
        assertEquals("Test { \tenc = 1; \tset = 32; }", evt.toOneLineString());
        assertEquals("Test\n{\n\tenc = 1;\n\tset = 32;\n}", evt.toString());
    }

    @Test
    public void testNullValue() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        Short s = evt.getInt16("a");
        assertNull(s);
        evt.setInt16("a", (short) 1);
        s = evt.getInt16("a");
        assertNotNull(s);
        assertEquals("short value incorrect", (short) 1, s.shortValue());
    }

    @Test
    public void testUnsignedTypesValidate() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        try {
            evt.setUInt16("SiteID", 0);
            eventTemplate.validate(evt);
        }
        catch (EventSystemException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateEventName() throws EventSystemException {
        boolean exceptionThrown = false;
        Event evt = createEvent();
        evt.setEventName("Test2");
        try {
            eventTemplate.validate(evt);
        }
        catch (ValidationExceptions e) {
            exceptionThrown = true;
        }
        assertTrue("No exception for invalid event", exceptionThrown);
    }

    @Test
    public void testValidateField() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        try {
            evt.setString("field1", "avalue");
            eventTemplate.validate(evt);
        }
        catch (EventSystemException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateBadTypeField() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        try {
            evt.setInt16("field1", (short) 15);
            eventTemplate.validate(evt);
        }
        catch (ValidationExceptions e) {
            List<EventSystemException> exc = e.getAllExceptions();
            assertEquals("Wrong num of exceptions", 1, exc.size());
            assertEquals("Wrong exception",
                         "org.lwes.NoSuchAttributeTypeException",
                         exc.get(0).getClass().getName());
        }
    }

    @Test
    public void testValidateBadField() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        try {
            evt.setInt16("field3", (short) 15);
            eventTemplate.validate(evt);
        }
        catch (ValidationExceptions e) {
            List<EventSystemException> exc = e.getAllExceptions();
            assertEquals("Wrong num of exceptions", 1, exc.size());
            assertEquals("Wrong exception",
                         "org.lwes.NoSuchAttributeException",
                         exc.get(0).getClass().getName());
        }
    }

    @Test
    public void testSerialize() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        evt.setString("attr_s", "str_value");
        evt.setInt32("attr_i", 1);
        byte[] encoded = Base64.encodeBase64(evt.serialize());
        assertEquals("BFRlc3QAAwNlbmMCAAEGYXR0cl9zBQAJc3RyX3ZhbHVlBmF0dHJfaQQAAAAB", new String(encoded));
    }

    @Test
    public void testEventAccessors() throws EventSystemException, UnknownHostException {
        Event evt = createEvent();
        evt.setEventName("Test");

        evt.setInt16("int16", (short) 1);
        evt.setInt32("int32", 1337);
        evt.setInt64("int64", 1337133713371337l);
        evt.setBoolean("bool", true);
        evt.setString("str", "string");
        evt.setUInt16("uint16", 1337); // uint16 in java is just an int
        evt.setUInt32("uint32", 0xffffffffL); // uint32 in java is a long
        evt.setUInt64("uint64", 1337133713371337l); // uint64 is a BigInteger
        evt.setIPAddress("ipaddr", InetAddress.getByName("localhost"));

        Short s = evt.getInt16("int16");
        assertNotNull(s);
        assertEquals("int16 wrong", 1, s.shortValue());
        Integer i = evt.getInt32("int32");
        assertNotNull(i);
        assertEquals("int32 wrong", 1337, i.intValue());
        Long l = evt.getInt64("int64");
        assertNotNull(l);
        assertEquals("int64 wrong", 1337133713371337l, l.longValue());
        assertEquals("bool wrong", true, evt.getBoolean("bool"));
        assertEquals("str wrong", "string", evt.getString("str"));
        i = evt.getUInt16("uint16");
        assertNotNull(i);
        assertEquals("uint16 wrong", 1337, i.intValue());
        l = evt.getUInt32("uint32");
        assertEquals("uint32 wrong", 0xffffffffL, l.longValue());
        assertEquals("uint64 wrong",
                     BigInteger.valueOf(1337133713371337l),
                     evt.getUInt64("uint64"));

        final Event evt2 = createEvent();
        evt2.deserialize(evt.serialize());
        assertEquals("Event changed under [de]serialization:\n"+evt+"\n"+evt2, evt, evt2);
    }

    @Test
    public void testEventSize() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");

        for (int i = 0; i < 5000; i++) {
            evt.setInt32("" + i, i);
        }

        byte[] bytes = evt.serialize();
        assertEquals("number of bytes wrong?", 48904, bytes.length);

        boolean exceptionThrown = false;
        try {
            for (int i = 5001; i < 10000; i++) {
                evt.setInt32("" + i, i);
            }
        }
        catch (EventSystemException e) {
            exceptionThrown = true;
            assertEquals("Different exception",
                         "org.lwes.EventSystemException",
                         e.getClass().getName());
        }
        assertTrue("Size exception was not thrown", exceptionThrown);
    }
    
    @Test
    public void testLengthRestriction() throws EventSystemException {
        final ArrayEvent event = new ArrayEvent("Event");
        event.setByteArray("field", new byte[65483]);
        try {
            event.setByteArray("field", new byte[65484]);
            fail("Should have failed when creating such a large event");
        }
        catch (EventSystemException e) {
            if (!e.getMessage().contains("causing an overrun")) {
                throw e;
            }
        }
    }
    
    @Test
    public void testMaximallyLongEventNames() throws EventSystemException {
        createEvent().setEventName(
                "       010       020       030       040       050       060       070       080       090       100       110       120    127");
    }
    
    @Test(expected=EventSystemException.class)
    public void testOverlyLongEventNames() throws EventSystemException {
        createEvent().setEventName(
                "       010       020       030       040       050       060       070       080       090       100       110       120     128");
    }
    
    @Test
    public void testMaximallyLongFieldName() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        
        final String name = "       010       020       030       040       050       060       070       080       090       100       110       120       130       140       150       160       170       180       190       200       210       220       230       240       250  255";
        evt.setString(name, "irrelevant");
        
        evt.deserialize(evt.serialize());
    }
    
    @Test(expected=EventSystemException.class)
    public void testOverlyLongFieldName() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Test");
        final String name = "       010       020       030       040       050       060       070       080       090       100       110       120       130       140       150       160       170       180       190       200       210       220       230       240       250   256";
        evt.setString(name, "irrelevant");
    }
    
    @Test
    public void testAllFieldTypes() throws EventSystemException {
        Event evt = createEvent();
        evt.setEventName("Everything");
        evt.setUInt16("field1", 1);
        evt.setInt16("field2", (short) 2);
        evt.setUInt32("field3", 3L);
        evt.setInt32("field4", 4);
        evt.setString("field5", "five");
        evt.setIPAddress("field6", new IPAddress("6.6.6.6"));
        evt.setInt64("field7", 7L);
        evt.setUInt64("field8", BigInteger.valueOf(8));
        evt.setBoolean("field9", true);
        evt.setByte("field10", (byte) 10);
        evt.setFloat("field11", 11F);
        evt.setDouble("field12", 12.);
        evt.setUInt16Array("field13", new int[] { 13 });
        evt.setInt16Array("field14", new short[] { 14 });
        evt.setUInt32Array("field15", new long[] { 15 });
        evt.setInt32Array("field16", new int[] { 16 });
        evt.setStringArray("field17", new String[] { "seventeen" });
        evt.setIPAddressArray("field18", new IPAddress[] { new IPAddress("18.18.18.18") });
        evt.setInt64Array("field19", new long[] { 19L });
        evt.setUInt64Array("field20", new BigInteger[] { BigInteger.valueOf(20) });
        evt.setBooleanArray("field21", new boolean[] { false });
        evt.setByteArray("field22", new byte[] { 22 });
        evt.setFloatArray("field23", new float[] { 23F });
        evt.setDoubleArray("field24", new double[] { 24. });
        eventTemplate.validate(evt);
        
        Event evt2 = new MapEvent("Everything", true, eventTemplate);
        for (int i=1; i<=24; ++i) {
            final String field = "field" + i, wrongField = "field" + (i==24 ? 1 : (i+1));
            final Object value = evt.get(field);
            try {
                final BaseType bt = eventTemplate.getBaseTypeForObjectAttribute(evt.getEventName(), wrongField, value);
                evt2.set(wrongField, bt.getType(), value);
                fail("Failed to detect a problem when storing a "+value.getClass().getName()+" as a "+evt.getType(field));
            } catch(NoSuchAttributeTypeException nsate) { }
        }
    }
    
    @Test
    public void testIntBounds() {
        final Event evt = createEvent();
        evt.setEventName("Test");
        
        evt.setByte("byte_min",  Byte.MIN_VALUE);
        evt.setByte("byte_zero", (byte) 0);
        evt.setByte("byte_one",  (byte) 1);
        evt.setByte("byte_max",  Byte.MAX_VALUE);
        
        evt.setInt16("int16_min",  Short.MIN_VALUE);
        evt.setInt16("int16_zero", (short) 0);
        evt.setInt16("int16_one",  (short) 1);
        evt.setInt16("int16_max",  Short.MAX_VALUE);
        
        evt.setInt32("int32_min",  Integer.MIN_VALUE);
        evt.setInt32("int32_zero", 0);
        evt.setInt32("int32_one",  1);
        evt.setInt32("int32_max",  Integer.MAX_VALUE);
        
        evt.setInt64("int64_min",  Long.MIN_VALUE);
        evt.setInt64("int64_zero", 0);
        evt.setInt64("int64_one",  1);
        evt.setInt64("int64_max",  Long.MAX_VALUE);
        
        evt.setUInt16("uint16_zero", 0);
        evt.setUInt16("uint16_one",  1);
        evt.setUInt16("uint16_max",  0xffff);
        
        evt.setUInt32("uint32_zero", 0);
        evt.setUInt32("uint32_one",  1);
        evt.setUInt32("uint32_max",  0xffffffffL);
        
        evt.setUInt64("uint64_zero", BigInteger.ZERO);
        evt.setUInt64("uint64_one",  BigInteger.ONE);
        evt.setUInt64("uint64_max",  BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE));
        
        evt.setInt16Array("int16[]", new short[] { Short.MIN_VALUE, 0, 1, Short.MAX_VALUE });
        evt.setInt32Array("int32[]", new int[] { Integer.MIN_VALUE, 0, 1, Integer.MAX_VALUE });
        evt.setInt64Array("int64[]", new long[] { Long.MIN_VALUE, 0, 1, Long.MAX_VALUE });
        evt.setUInt16Array("uint16[]", new int[] { 0, 1, 0xffff });
        evt.setUInt32Array("uint32[]", new long[] { 0, 1, 0xffffffffL });
        evt.setUInt64Array("uint64[]", new BigInteger[] { BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE) });
        evt.setUInt64Array("uint64[] prim", new long[] { 0, 1, -1 });
        
        final Event evt2 = createEvent();
        evt2.deserialize(evt.serialize());
        //assertEquals(evt, evt2);
        
        assertEquals(Byte.MIN_VALUE, evt.getByte("byte_min").byteValue());
        assertEquals((byte) 0, evt.getByte("byte_zero").byteValue());
        assertEquals((byte) 1, evt.getByte("byte_one").byteValue());
        assertEquals(Byte.MAX_VALUE, evt.getByte("byte_max").byteValue());
        
        assertEquals(Short.MIN_VALUE, evt.getInt16("int16_min").shortValue());
        assertEquals((short) 0, evt.getInt16("int16_zero").shortValue());
        assertEquals((short) 1, evt.getInt16("int16_one").shortValue());
        assertEquals(Short.MAX_VALUE, evt.getInt16("int16_max").shortValue());
        
        assertEquals(Integer.MIN_VALUE, evt.getInt32("int32_min").intValue());
        assertEquals(0, evt.getInt32("int32_zero").intValue());
        assertEquals(1, evt.getInt32("int32_one").intValue());
        assertEquals(Integer.MAX_VALUE, evt.getInt32("int32_max").intValue());
        
        assertEquals(Long.MIN_VALUE, evt.getInt64("int64_min").longValue());
        assertEquals(0, evt.getInt64("int64_zero").longValue());
        assertEquals(1, evt.getInt64("int64_one").longValue());
        assertEquals(Long.MAX_VALUE, evt.getInt64("int64_max").longValue());
        
        assertEquals(0, evt.getUInt16("uint16_zero").intValue());
        assertEquals(1, evt.getUInt16("uint16_one").intValue());
        assertEquals(0xffff, evt.getUInt16("uint16_max").intValue());
        
        assertEquals(0, evt.getUInt32("uint32_zero").longValue());
        assertEquals(1, evt.getUInt32("uint32_one").longValue());
        assertEquals(0xffffffffL, evt.getUInt32("uint32_max").longValue());
        
        assertEquals(BigInteger.ZERO, evt.getUInt64("uint64_zero"));
        assertEquals(BigInteger.ONE, evt.getUInt64("uint64_one"));
        assertEquals(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE), evt.getUInt64("uint64_max"));
        
        assertArrayEquals(new short[] { Short.MIN_VALUE, 0, 1, Short.MAX_VALUE }, evt.getInt16Array("int16[]"));
        assertArrayEquals(new int[] { Integer.MIN_VALUE, 0, 1, Integer.MAX_VALUE }, evt.getInt32Array("int32[]"));
        assertArrayEquals(new long[] { Long.MIN_VALUE, 0, 1, Long.MAX_VALUE }, evt.getInt64Array("int64[]"));
        assertArrayEquals(new int[] { 0, 1, 0xffff }, evt.getUInt16Array("uint16[]"));
        assertArrayEquals(new long[] { 0, 1, 0xffffffffL }, evt.getUInt32Array("uint32[]"));
        assertArrayEquals(new BigInteger[] { BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE) }, evt.getUInt64Array("uint64[]"));
        assertArrayEquals(new BigInteger[] { BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE) }, evt.getUInt64Array("uint64[] prim"));
    }
    
    @Test
    public void testMetaFields() {
        assertEquals(new TreeSet<String>(Arrays.asList("SenderIP,SenderPort,ReceiptTime,enc,SiteID".split(","))),
                new TreeSet<String>(eventTemplate.getMetaFields().keySet()));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testInvalidEncoding1() {
        Event evt = createEvent("Event");
        evt.setEncoding((short) -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidEncoding2() {
        Event evt = createEvent("Event");
        evt.setEncoding((short) 10);
    }

    @Test
    public void testValidEncoding() {
        Event evt = createEvent("Event");
        evt.setEncoding(Event.UTF_8);
        Assert.assertEquals(Event.UTF_8, evt.getEncoding());
        evt.setEncoding(Event.ISO_8859_1);
        Assert.assertEquals(Event.ISO_8859_1, evt.getEncoding());
    }

    @Test
    public void testBasicFunctions() throws EventSystemException {
        final byte[] bytes = new byte[]{4, 'T', 'e', 's', 't', 0, 2, 3, 'e', 'n', 'c', FieldType.INT16.token, 0, 1, 2, 'a', 'b', FieldType.INT16.token, -10, 12};

        final Event e1 = createEvent();
        e1.deserialize(bytes);

        assertTrue(Arrays.equals(bytes, e1.serialize()));
        assertEquals("Test { \tab = -2548; \tenc = 1; }", e1.toOneLineString());
        assertEquals("Test { \tab = -2548; \tenc = 1; }", e1.copy().toOneLineString());
        assertTrue(e1.isSet("ab"));

        final Event e2 = e1.copy();
        e2.clear("ab");
        assertEquals("Test { \tenc = 1; }", e2.toOneLineString());
        assertTrue(e1.isSet("ab"));
        assertFalse(e2.isSet("ab"));

        final Event e3 = e1.copy();
        e3.setEventName("Different");
        assertEquals("Different { \tab = -2548; \tenc = 1; }", e3.toOneLineString());
        e3.set("cd", FieldType.STRING, "value");
        assertEquals("Different { \tab = -2548; \tcd = value; \tenc = 1; }", e3.toOneLineString());
        e3.setEncoding(Event.ISO_8859_1);
        assertEquals("Different { \tab = -2548; \tcd = value; \tenc = 0; }", e3.toOneLineString());
        e3.set("enc", FieldType.INT16, Event.DEFAULT_ENCODING);
        assertEquals("Different { \tab = -2548; \tcd = value; \tenc = 1; }", e3.toOneLineString());
        e3.set("ab", FieldType.INT16, (short) -1234);
        assertEquals("Different { \tab = -1234; \tcd = value; \tenc = 1; }", e3.toOneLineString());

        final Enumeration<String> nameEnum = e3.getEventAttributeNames();
        final String[] names = new String[3];
        assertTrue(nameEnum.hasMoreElements());
        names[0] = nameEnum.nextElement();
        assertTrue(nameEnum.hasMoreElements());
        names[1] = nameEnum.nextElement();
        assertTrue(nameEnum.hasMoreElements());
        names[2] = nameEnum.nextElement();
        assertFalse(nameEnum.hasMoreElements());
        Arrays.sort(names);
        assertEquals("ab", names[0]);
        assertEquals("cd", names[1]);
        assertEquals("enc", names[2]);

        assertEquals(FieldType.INT16, e3.getType("ab"));
        assertEquals(FieldType.STRING, e3.getType("cd"));
        assertEquals(FieldType.INT16, e3.getType("enc"));
        assertNull(e3.getType("zzz"));

        final Event e4 = new MapEvent();
        e4.copyFrom(e3);
        assertEquals("Different { \tab = -1234; \tcd = value; \tenc = 1; }", e4.toOneLineString());

        final Event e5 = createEvent();
        e5.copyFrom(e3);
        assertEquals("Different { \tab = -1234; \tcd = value; \tenc = 1; }", e5.toOneLineString());
        e5.copyFrom(e4);
        assertEquals("Different { \tab = -1234; \tcd = value; \tenc = 1; }", e5.toOneLineString());

        e1.reset();
        assertEquals("", e1.toOneLineString());
        assertFalse(e1.isSet("ab"));

        System.gc();
    }

    @Test
    public void testGettersSetters() {
        Event evt = createEvent("Event");

        // Test the various getters/setters
        evt.set("key", FieldType.STRING, "value");
        Assert.assertEquals("value", evt.get("key"));
        evt.set("boolean", FieldType.BOOLEAN, true);
        Assert.assertTrue(evt.getBoolean("boolean"));
        evt.set("byte", FieldType.BYTE, Byte.parseByte("32"));
        Assert.assertEquals(32, (byte) evt.getByte("byte"));
        evt.set("double", FieldType.DOUBLE, 5.0);
        Assert.assertEquals(5.0, evt.getDouble("double"));
        evt.set("float", FieldType.FLOAT, 1.2f);
        Assert.assertEquals(1.2f, evt.getFloat("float"));
        evt.set("int16", FieldType.INT16, (short) 10);
        Assert.assertEquals(10, (short) evt.getInt16("int16"));
        evt.set("uint16", FieldType.UINT16, 10);
        Assert.assertEquals(10, (int) evt.getUInt16("uint16"));

        evt.set("int32", FieldType.INT32, 10);
        Assert.assertEquals(10, (int) evt.getInt32("int32"));
        evt.set("uint32", FieldType.UINT32, 10l);
        Assert.assertEquals(10, (long) evt.getUInt32("uint32"));

        evt.set("int64", FieldType.INT64, 10l);
        Assert.assertEquals(10, (long) evt.getInt64("int64"));

        evt.set("uint64", FieldType.UINT64, new BigInteger("10000000000000"));
        Assert.assertEquals(new BigInteger("10000000000000"), evt.getUInt64("uint64"));
    }

    @Test
    public void testArrayGettersSetters() {
        Event evt = createEvent("Event");

        evt.set("int32[]", FieldType.INT32_ARRAY, new int[]{10});
        Assert.assertEquals(10, evt.getInt32Array("int32[]")[0]);

        evt.set("boolean[]", FieldType.BOOLEAN_ARRAY, new boolean[]{true});
        Assert.assertTrue(evt.getBooleanArray("boolean[]")[0]);

        evt.set("byte[]", FieldType.BYTE_ARRAY, new byte[]{Byte.parseByte("32")});
        Assert.assertEquals(32, evt.getByteArray("byte[]")[0]);

        evt.set("double[]", FieldType.DOUBLE_ARRAY, new double[]{5.0});
        Assert.assertEquals(5.0, evt.getDoubleArray("double[]")[0]);

        evt.set("float[]", FieldType.FLOAT_ARRAY, new float[]{1.2f});
        Assert.assertEquals(1.2f, evt.getFloatArray("float[]")[0]);

        evt.set("int16[]", FieldType.INT16_ARRAY, new short[]{(short) 10});
        Assert.assertEquals(10, evt.getInt16Array("int16[]")[0]);

        evt.set("uint16[]", FieldType.UINT16_ARRAY, new int[]{10});
        Assert.assertEquals(10, evt.getUInt16Array("uint16[]")[0]);

        evt.set("uint32[]", FieldType.UINT32_ARRAY, new long[]{10l});
        Assert.assertEquals(10, evt.getUInt32Array("uint32[]")[0]);

        evt.set("int64[]", FieldType.INT64_ARRAY, new long[]{10l});
        Assert.assertEquals(10, evt.getInt64Array("int64[]")[0]);

        evt.set("uint64[]", FieldType.UINT64_ARRAY, new BigInteger[]{new BigInteger("10000000000000")});
        Assert.assertEquals(new BigInteger("10000000000000"), evt.getUInt64Array("uint64[]")[0]);
    }

    @Test
    public void testStringArray() {
        Event evt = createEvent("Event");

        evt.set("string[]", FieldType.STRING_ARRAY, new String[]{"value"});
        Assert.assertEquals("value", evt.getStringArray("string[]")[0]);

        evt.set("int64[]", FieldType.INT64_ARRAY, new long[]{10l});
        Assert.assertEquals(10, evt.getInt64Array("int64[]")[0]);
    }

    @Test(expected = EventSystemException.class)
    public void testInvalidEncodingType() throws EventSystemException {
        final Event event = createEvent("Event");
        event.set("enc", FieldType.INT32, Event.DEFAULT_ENCODING);
    }

    @Test
    public void testNInt64() {
        Event evt = createEvent("Event");
        evt.set("long[]", FieldType.NINT64_ARRAY, new Long[]{5000000000l, null, 8675309l});
        Long[] retrievedArray = evt.getLongObjArray("long[]");
        Assert.assertEquals(5000000000l, retrievedArray[0].longValue());
        Assert.assertNull(evt.getLongObjArray("long[]")[1]);
    }

    @Test
    public void testNDouble() {
        Event evt = createEvent("Event");
        evt.set("double[]", FieldType.NDOUBLE_ARRAY, new Double[]{1.23, 1.26, null});
        Assert.assertEquals(1.23, evt.getDoubleObjArray("double[]")[0]);
        Assert.assertNull(evt.getDoubleObjArray("double[]")[2]);
    }

    @Test
    public void testNFloat() {
        Event evt = createEvent("Event");
        evt.set("float[]", FieldType.NFLOAT_ARRAY, new Float[]{1.11f, null, 1.12f});
        Assert.assertEquals(1.11f, evt.getFloatObjArray("float[]")[0]);
        Assert.assertNull(evt.getFloatObjArray("float[]")[1]);
    }

    @Test
    public void testNInt16() {
        Event evt = createEvent("Event");
        evt.set("short[]", FieldType.NINT16_ARRAY, new Short[]{5, null, 10});
        Assert.assertEquals(5, evt.getShortObjArray("short[]")[0].shortValue());
        Assert.assertNull(evt.getShortObjArray("short[]")[1]);
    }

    @Test
    public void testNUint16() {
        Event evt = createEvent("Event");
        evt.set("int[]", FieldType.NUINT16_ARRAY, new Integer[]{5, null, 10});
        Assert.assertEquals(5, evt.getIntegerObjArray("int[]")[0].intValue());
        Assert.assertNull(evt.getIntegerObjArray("int[]")[1]);
    }

    @Test
    public void testNInt32() {
        Event evt = createEvent("Event");
        evt.set("int[]", FieldType.NINT32_ARRAY, new Integer[]{5, null, 10});
        Assert.assertEquals(5, evt.getIntegerObjArray("int[]")[0].intValue());
        Assert.assertNull(evt.getIntegerObjArray("int[]")[1]);
    }

    @Test
    public void testNUint32() {
        Event evt = createEvent("Event");
        evt.set("long[]", FieldType.NUINT32_ARRAY, new Long[]{5000l, null, 12345l});
        Assert.assertEquals(5000, evt.getLongObjArray("long[]")[0].longValue());
        Assert.assertNull(evt.getLongObjArray("long[]")[1]);
    }

    @Test
    public void testNUint64() {
        Event evt = createEvent("Event");
        evt.set("biginteger[]", FieldType.NUINT64_ARRAY,
                new BigInteger[]{
                        new BigInteger("5"), null, new BigInteger("10")
                });
        Assert.assertEquals(5, evt.getBigIntegerObjArray("biginteger[]")[0].intValue());
        Assert.assertNull(evt.getBigIntegerObjArray("biginteger[]")[1]);
    }

    @Test
    public void testNBoolean() {
        Event evt = createEvent("Event");
        evt.set("boolean[]", FieldType.NBOOLEAN_ARRAY, new Boolean[]{true, null, false});
        Assert.assertTrue(evt.getBooleanObjArray("boolean[]")[0]);
        Assert.assertNull(evt.getBooleanObjArray("boolean[]")[1]);
        Assert.assertFalse(evt.getBooleanObjArray("boolean[]")[2]);
    }

    @Test
    public void testNString() {
        Event evt = createEvent("Event");
        evt.set("string[]", FieldType.NSTRING_ARRAY, new String[]{"a", null, "bc"});
        Assert.assertEquals("a", evt.getStringObjArray("string[]")[0]);
        Assert.assertNull(evt.getStringObjArray("string[]")[1]);
        Assert.assertEquals("bc", evt.getStringObjArray("string[]")[2]);
    }

    @Test
    public void testNByte() {
        Event evt = createEvent("Event");
        evt.set("byte[]", FieldType.NBYTE_ARRAY, new Byte[]{0x32, null, 0x33});
        Assert.assertEquals(0x32, evt.getByteObjArray("byte[]")[0].byteValue());
        Assert.assertNull(evt.getByteObjArray("byte[]")[1]);
        Assert.assertEquals(0x33, evt.getByteObjArray("byte[]")[2].byteValue());
    }

    @Test
    public void testNIntegrated() {
        Event evt = createEvent("Event");

        evt.set("nint64[]", FieldType.NINT64_ARRAY, new Long[]{5000000000l, null, 8675309l});
        Long[] retrievedArray = evt.getLongObjArray("nint64[]");
        Assert.assertEquals(5000000000l, retrievedArray[0].longValue());
        Assert.assertNull(evt.getLongObjArray("nint64[]")[1]);

        evt.set("nuint16[]", FieldType.NUINT16_ARRAY, new Integer[]{5000, null, 8675});
        Assert.assertEquals(5000, evt.getIntegerObjArray("nuint16[]")[0].intValue());
        Assert.assertNull(evt.getIntegerObjArray("nuint16[]")[1]);

        evt.set("nint16[]", FieldType.NINT16_ARRAY, new Short[]{5000, null, 8675});
        Assert.assertEquals(5000, evt.getShortObjArray("nint16[]")[0].shortValue());
        Assert.assertNull(evt.getShortObjArray("nint16[]")[1]);

        evt.set("nuint32[]", FieldType.NUINT32_ARRAY, new Long[]{5000l, null, 12345l});
        Assert.assertEquals(5000, evt.getLongObjArray("nuint32[]")[0].longValue());
        Assert.assertNull(evt.getLongObjArray("nuint32[]")[1]);

        evt.set("byte[]", FieldType.NBYTE_ARRAY, new Byte[]{0x32, null, 0x33});
        Assert.assertEquals(0x32, evt.getByteObjArray("byte[]")[0].byteValue());
        Assert.assertNull(evt.getByteObjArray("byte[]")[1]);
        Assert.assertEquals(0x33, evt.getByteObjArray("byte[]")[2].byteValue());

        evt.set("Double[]", FieldType.NDOUBLE_ARRAY, new Double[]{
                0.15392470038762496, null, 0.8454572482640883, 0.4266316445138164, 0.9235260958754714
        });
        Assert.assertEquals(0.15392470038762496, evt.getDoubleObjArray("Double[]")[0]);
        Assert.assertNull(evt.getDoubleObjArray("Double[]")[1]);

        evt.set("revjhdttokuc", FieldType.STRING,
                "kzukphbcbghywpklojzauzmyapwdmmqctcxeoqbvzwzltdzanksdxzfkrgvkemsbiqxnjqdivsszxetvytxocrukyqiu");
        Assert.assertEquals(
                "kzukphbcbghywpklojzauzmyapwdmmqctcxeoqbvzwzltdzanksdxzfkrgvkemsbiqxnjqdivsszxetvytxocrukyqiu",
                evt.getString("revjhdttokuc"));

        evt.set("float[]", FieldType.NFLOAT_ARRAY, new Float[]{1.11f, null, 1.12f});
        Assert.assertEquals(1.11f, evt.getFloatObjArray("float[]")[0]);
        Assert.assertNull(evt.getFloatObjArray("float[]")[1]);

        evt.serialize();
    }

    @Test
    public void testResettingField() {
        Event evt = createEvent("Event");
        Short[] ar = new Short[]{1, null, 2};

        evt.set("nint16[]", FieldType.NINT16_ARRAY, ar);
        Assert.assertEquals(1, evt.getShortObjArray("nint16[]")[0].shortValue());
        Assert.assertNull(evt.getShortObjArray("nint16[]")[1]);

        evt.set("nint16[]", FieldType.NINT16_ARRAY, new Short[]{null});
        Assert.assertNull(evt.getShortObjArray("nint16[]")[0]);

        evt.serialize();
        
        evt.set("int32", FieldType.INT32, 17);
        Assert.assertEquals(17, evt.getInt32("int32").intValue());
        evt.set("int32", FieldType.INT32, 19);
        Assert.assertEquals(19, evt.getInt32("int32").intValue());
        
        evt.set("int32", FieldType.UINT32, 23L);  // Test changing type.
        Assert.assertEquals(23, evt.getUInt32("int32").intValue());
    }

    @Test
    public void testStringReset() {
        Event evt = createEvent("Event");
        evt.set("a", FieldType.STRING, "zzzzzz");
        Assert.assertEquals("zzzzzz", evt.get("a"));
        evt.set("a", FieldType.STRING, "ab");
        Assert.assertEquals("ab", evt.get("a"));

        evt.set("b", FieldType.STRING_ARRAY, new String[]{"a", "b"});
        Assert.assertEquals("b", evt.getStringArray("b")[1]);
        evt.set("b", FieldType.STRING_ARRAY, new String[]{"a", "b", "c"});
        Assert.assertEquals("c", evt.getStringArray("b")[2]);

        evt.serialize();
    }
    
    @Test
    public void testDataStreamSerialization() throws Exception {
        final Event evt = createEvent("Event");
        evt.set("a", FieldType.STRING, "zzzzzz");
        evt.set("b", FieldType.NINT64_ARRAY, new Long[] { 100L, null, 200L });
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream      dos  = new DataOutputStream(baos);
        evt.serialize(dos);
        dos.close();
        final byte[] bytes = baos.toByteArray();

        Assert.assertTrue(Arrays.equals(evt.serialize(), bytes));

        final Event           evt2 = createEvent();
        final DataInputStream dis  = new DataInputStream(new ByteArrayInputStream(bytes));
        evt2.deserialize(dis, bytes.length);
        
        Assert.assertEquals(evt, evt2);
        Assert.assertTrue(Arrays.equals(evt.serialize(), evt2.serialize()));
    }
}
