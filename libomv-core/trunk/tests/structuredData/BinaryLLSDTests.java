/**
 * Copyright (c) 2007-2008, openmetaverse.org
 * Portions Copyright (c) 2012, Frederick Martian
 * All rights reserved.
 *
 * - Redistribution and use in source and binary forms, with or without
 *   modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Neither the name of the openmetaverse.org nor the names
 *   of its contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package structuredData;

/* 
 * This tests are based upon the description at
 * 
 * http://wiki.secondlife.com/wiki/LLSD
 * 
 * and (partially) generated by the (supposed) reference implementation at
 * 
 * http://svn.secondlife.com/svn/linden/release/indra/lib/python/indra/base/llsd.py
 */

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import junit.framework.Assert;
import junit.framework.TestCase;
import libomv.StructuredData.OSD;
import libomv.StructuredData.OSD.OSDFormat;
import libomv.StructuredData.OSD.OSDType;
import libomv.StructuredData.OSDArray;
import libomv.StructuredData.OSDInteger;
import libomv.StructuredData.OSDMap;
import libomv.StructuredData.OSDParser;
import libomv.StructuredData.OSDString;
import libomv.types.UUID;
import libomv.utils.Helpers;

public class BinaryLLSDTests extends TestCase
{
	private static final byte[] llsdBinaryHead = { '<','?','l','l','s','d','/','b','i','n','a','r','y','?','>','\n'};

    // Testvalues for Undef:
    private static byte[] binaryUndefValue = { 0x21 };
    private static byte[] binaryUndef = concat(llsdBinaryHead, binaryUndefValue);

    public void testDeserializeUndef() throws IOException, ParseException
    {
        OSD llsdUndef = OSDParser.deserialize(binaryUndef, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.Unknown, llsdUndef.getType());
    }

    public void testSerializeUndef() throws IOException
    {
        OSD llsdUndef = new OSD();
        byte[] binaryUndefSerialized = OSDParser.serializeToBytes(llsdUndef, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryUndef, binaryUndefSerialized));
    }

    private static byte[] binaryTrueValue = { 0x31 };
    private static byte[] binaryTrue = concat(llsdBinaryHead, binaryTrueValue);


    private static byte[] binaryFalseValue = { 0x30 };
    private static byte[] binaryFalse = concat(llsdBinaryHead, binaryFalseValue);

    public void testDeserializeBool() throws IOException, ParseException
    {
        OSD llsdTrue = OSDParser.deserialize(binaryTrue, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.Boolean, llsdTrue.getType());
        Assert.assertTrue(llsdTrue.AsBoolean());

        OSD llsdFalse = OSDParser.deserialize(binaryFalse, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.Boolean, llsdFalse.getType());
        Assert.assertFalse(llsdFalse.AsBoolean());
    }
    
    public void testSerializeBool() throws IOException
    {
        OSD llsdTrue = OSD.FromBoolean(true);
        byte[] binaryTrueSerialized = OSDParser.serializeToBytes(llsdTrue, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryTrue, binaryTrueSerialized));

        OSD llsdFalse = OSD.FromBoolean(false);
        byte[] binaryFalseSerialized = OSDParser.serializeToBytes(llsdFalse, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryFalse, binaryFalseSerialized));
    }

    private static byte[] binaryZeroIntValue = { 0x69, 0x0, 0x0, 0x0, 0x0 };
    private static byte[] binaryZeroInt = concat(llsdBinaryHead, binaryZeroIntValue);

    private static byte[] binaryAnIntValue = { 0x69, 0x0, 0x12, (byte) 0xd7, (byte) 0x9b };
    private static byte[] binaryAnInt = concat(llsdBinaryHead, binaryAnIntValue);

    public void testDeserializeInteger() throws IOException, ParseException
    {
        OSD llsdZeroInteger = OSDParser.deserialize(binaryZeroInt, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.Integer, llsdZeroInteger.getType());
        Assert.assertEquals(0, llsdZeroInteger.AsInteger());


        OSD llsdAnInteger = OSDParser.deserialize(binaryAnInt, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.Integer, llsdAnInteger.getType());
        Assert.assertEquals(1234843, llsdAnInteger.AsInteger());
    }

    public void testSerializeInteger() throws IOException
    {
        OSD llsdZeroInt = OSD.FromInteger(0);
        byte[] binaryZeroIntSerialized = OSDParser.serializeToBytes(llsdZeroInt, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryZeroInt, binaryZeroIntSerialized));

        binaryZeroIntSerialized = OSDParser.serializeToBytes(llsdZeroInt, OSDFormat.Binary, false);
        Assert.assertTrue(Arrays.equals(binaryZeroIntValue, binaryZeroIntSerialized));

        OSD llsdAnInt = OSD.FromInteger(1234843);
        byte[] binaryAnIntSerialized = OSDParser.serializeToBytes(llsdAnInt, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryAnInt, binaryAnIntSerialized));

        binaryAnIntSerialized = OSDParser.serializeToBytes(llsdAnInt, OSDFormat.Binary, false);
        Assert.assertTrue(Arrays.equals(binaryAnIntValue, binaryAnIntSerialized));
    }

    private static byte[] binaryRealValue = { 0x72, 0x41, 0x2c, (byte) 0xec, (byte) 0xf6, 0x77, (byte) 0xce, (byte) 0xd9, 0x17 };
    private static byte[] binaryReal = concat(llsdBinaryHead, binaryRealValue);

    public void testDeserializeReal() throws IOException, ParseException
    {
        OSD llsdReal = OSDParser.deserialize(binaryReal, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.Real, llsdReal.getType());
        double real = llsdReal.AsReal();
        Assert.assertTrue(947835.234d == real);
    }

    public void testSerializeReal() throws IOException
    {
        OSD llsdReal = OSD.FromReal(947835.234d);
        byte[] binaryRealSerialized = OSDParser.serializeToBytes(llsdReal, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryReal, binaryRealSerialized));

        binaryRealSerialized = OSDParser.serializeToBytes(llsdReal, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryReal, binaryRealSerialized));
    }
    
    private static byte[] binaryAUUIDValue = { 0x75, (byte) 0x97, (byte) 0xf4, (byte) 0xae, (byte) 0xca, (byte) 0x88, (byte) 0xa1, 0x42, 
    	                                       (byte) 0xa1, (byte) 0xb3, (byte) 0x85, (byte) 0xb9, 0x7b, 0x18, (byte) 0xab, (byte) 0xb2, 0x55 };
    private static byte[] binaryAUUID = concat(llsdBinaryHead, binaryAUUIDValue);

    private static byte[] binaryZeroUUIDValue = { 0x75, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 };
    private static byte[] binaryZeroUUID = concat(llsdBinaryHead, binaryZeroUUIDValue);


    public void testDeserializeUUID() throws IOException, ParseException
    {
        OSD llsdAUUID = OSDParser.deserialize(binaryAUUID, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.UUID, llsdAUUID.getType());
        Assert.assertEquals("97f4aeca-88a1-42a1-b385-b97b18abb255", llsdAUUID.AsString());

        OSD llsdZeroUUID = OSDParser.deserialize(binaryZeroUUID, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.UUID, llsdZeroUUID.getType());
        Assert.assertEquals("00000000-0000-0000-0000-000000000000", llsdZeroUUID.AsString());
    }

    public void testSerializeUUID() throws IOException
    {
        OSD llsdAUUID = OSD.FromUUID(new UUID("97f4aeca-88a1-42a1-b385-b97b18abb255"));
        byte[] binaryAUUIDSerialized = OSDParser.serializeToBytes(llsdAUUID, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryAUUID, binaryAUUIDSerialized));

        binaryAUUIDSerialized = OSDParser.serializeToBytes(llsdAUUID, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryAUUID, binaryAUUIDSerialized));

        OSD llsdZeroUUID = OSD.FromUUID(new UUID("00000000-0000-0000-0000-000000000000"));
        byte[] binaryZeroUUIDSerialized = OSDParser.serializeToBytes(llsdZeroUUID, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryZeroUUID, binaryZeroUUIDSerialized));

        binaryZeroUUIDSerialized = OSDParser.serializeToBytes(llsdZeroUUID, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryZeroUUID, binaryZeroUUIDSerialized));
    }

    private static byte[] binaryBinStringValue = { 0x62, 0x0, 0x0, 0x0, 0x34, // this line is the encoding header
            0x74, 0x65, 0x73, 0x74, 0x69, 0x6e, 0x67, 0x20, 0x61, 0x20, 0x73, 
            0x69, 0x6d, 0x70, 0x6c, 0x65, 0x20, 0x62, 0x69, 0x6e, 0x61, 0x72, 0x79, 0x20, 0x63, 0x6f,
            0x6e, 0x76, 0x65, 0x72, 0x73, 0x69, 0x6f, 0x6e, 0x20, 0x66, 0x6f, 0x72, 0x20, 0x74, 0x68,
            0x69, 0x73, 0x20, 0x73, 0x74, 0x72, 0x69, 0x6e, 0x67, 0xa, 0xd };
    private static byte[] binaryBinString = concat(llsdBinaryHead, binaryBinStringValue);

    public void testDeserializeLLSDBinary() throws IOException, ParseException
    {
        OSD llsdBytes = OSDParser.deserialize(binaryBinString, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.Binary, llsdBytes.getType());
        byte[] contentBinString = { 0x74, 0x65, 0x73, 0x74, 0x69, 0x6e, 0x67, 0x20, 0x61, 0x20, 0x73, 
                0x69, 0x6d, 0x70, 0x6c, 0x65, 0x20, 0x62, 0x69, 0x6e, 0x61, 0x72, 0x79, 0x20, 0x63, 0x6f,
                0x6e, 0x76, 0x65, 0x72, 0x73, 0x69, 0x6f, 0x6e, 0x20, 0x66, 0x6f, 0x72, 0x20, 0x74, 0x68,
                0x69, 0x73, 0x20, 0x73, 0x74, 0x72, 0x69, 0x6e, 0x67, 0xa, 0xd };
        Assert.assertTrue(Arrays.equals(contentBinString, llsdBytes.AsBinary()));
    }

    public void testSerializeLLSDBinary() throws IOException
    {
        byte[] contentBinString = { 0x74, 0x65, 0x73, 0x74, 0x69, 0x6e, 0x67, 0x20, 0x61, 0x20, 0x73, 
                0x69, 0x6d, 0x70, 0x6c, 0x65, 0x20, 0x62, 0x69, 0x6e, 0x61, 0x72, 0x79, 0x20, 0x63, 0x6f,
                0x6e, 0x76, 0x65, 0x72, 0x73, 0x69, 0x6f, 0x6e, 0x20, 0x66, 0x6f, 0x72, 0x20, 0x74, 0x68,
                0x69, 0x73, 0x20, 0x73, 0x74, 0x72, 0x69, 0x6e, 0x67, 0xa, 0xd };
        OSD llsdBinary = OSD.FromBinary(contentBinString);
        byte[] binaryBinarySerialized = OSDParser.serializeToBytes(llsdBinary, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryBinString, binaryBinarySerialized));
    }

    private static byte[] binaryEmptyStringValue = { 0x73, 0x0, 0x0, 0x0, 0x0 };
    private static byte[] binaryEmptyString = concat(llsdBinaryHead, binaryEmptyStringValue);
    private static byte[] binaryLongStringValue = { 0x73, 0x0, 0x0, 0x0, 0x25, 
                                0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
                                0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c,
                                0x6d, 0x6e, 0x6f, 0x70, 0x71, 0x72,
                                0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
                                0x79, 0x7a, 0x30, 0x31, 0x32, 0x33,
                                0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30 };
    private static byte[] binaryLongString = concat(llsdBinaryHead, binaryLongStringValue);

    public void testDeserializeString() throws IOException, ParseException
    {
        OSD llsdEmptyString = OSDParser.deserialize(binaryEmptyString, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.String, llsdEmptyString.getType());
        String contentEmptyString = "";
        Assert.assertEquals(contentEmptyString, llsdEmptyString.AsString());

        OSD llsdLongString = OSDParser.deserialize(binaryLongString, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.String, llsdLongString.getType());
        String contentLongString = "abcdefghijklmnopqrstuvwxyz01234567890";
        Assert.assertEquals(contentLongString, llsdLongString.AsString());
    }

    public void testSerializeString() throws IOException, ParseException, XmlPullParserException
    {
        OSD llsdString = OSD.FromString("abcdefghijklmnopqrstuvwxyz01234567890");
        byte[] binaryLongStringSerialized = OSDParser.serializeToBytes(llsdString, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryLongString, binaryLongStringSerialized));

        // A test with some utf8 characters
        String contentAStringXML = "<x>&#x196;&#x214;&#x220;&#x228;&#x246;&#x252;</x>";
        Reader reader1 = new StringReader(contentAStringXML);
        XmlPullParser parser1 = XmlPullParserFactory.newInstance().newPullParser();
        parser1.setInput(reader1);
		parser1.nextTag();
		parser1.require(XmlPullParser.START_TAG, null, "x");
        String contentAString = parser1.nextText();

        OSD llsdAString = OSD.FromString(contentAString);
        byte[] binaryAString = OSDParser.serializeToBytes(llsdAString, OSDFormat.Binary);
        OSD llsdAStringDS = OSDParser.deserialize(binaryAString);
        Assert.assertEquals(OSDType.String, llsdAStringDS.getType());
        Assert.assertEquals(contentAString, llsdAStringDS.AsString());

        // we also test for a 4byte character.
        String xml = "<x>&#x10137;</x>";
        Reader reader2 = new StringReader(xml);
        XmlPullParser parser2 = XmlPullParserFactory.newInstance().newPullParser();
        parser2.setInput(reader2);
		parser2.nextTag();
		parser2.require(XmlPullParser.START_TAG, null, "x");
        String content = parser2.nextText();

        OSD llsdStringOne = OSD.FromString(content);
        byte[] binaryAStringOneSerialized = OSDParser.serializeToBytes(llsdStringOne, OSDFormat.Binary);
        OSD llsdStringOneDS = OSDParser.deserialize(binaryAStringOneSerialized);
        Assert.assertEquals(OSDType.String, llsdStringOneDS.getType());
        Assert.assertEquals(content, llsdStringOneDS.AsString());
    }

    // Be careful. The current and above mentioned reference implementation has a bug that
    // doesnt allow proper binary Uri encoding.
    // We compare here to a fixed version of Uri encoding
    private static byte[] binaryURIValue = { 0x6c, 0x0, 0x0, 0x0, 0x18, // this line is the encoding header
        0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x77, 0x77, 0x77, 0x2e, 0x74,
        0x65, 0x73, 0x74, 0x75, 0x72, 0x6c, 0x2e, 0x74, 0x65, 0x73, 0x74, 0x2f };
    private static byte[] binaryURI = concat(llsdBinaryHead, binaryURIValue);

    public void testDeserializeURI() throws IOException, ParseException, URISyntaxException
    {
        OSD llsdURI = OSDParser.deserialize(binaryURI, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.URI, llsdURI.getType());
        URI uri = new URI("http://www.testurl.test/");
        Assert.assertEquals(uri, llsdURI.AsUri());
    }

    public void testSerializeURI() throws IOException, URISyntaxException
    {
        OSD llsdUri = OSD.FromUri(new URI("http://www.testurl.test/"));
        byte[] binaryURISerialized = OSDParser.serializeToBytes(llsdUri, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryURI, binaryURISerialized));
    }

    // Here is a problem.
    // The reference implementation does serialize to a local timestamp and not to a universal timestamp,
    // which means, this implementation and the reference implementation only work the same in the universal
    // timezone. Therefore this binaryDateTimeValue is generated in the UTC timezone by the reference
    // implementation.
    private static byte[] binaryDateTimeValue = { 100, 0, 0, (byte) 192, (byte) 141, (byte) 167, (byte) 222, (byte) 209, 65 };
    private static byte[] binaryDateTime = concat(llsdBinaryHead, binaryDateTimeValue);

    public void testDeserializeDateTime() throws IOException, ParseException
    {
        OSD llsdDateTime = OSDParser.deserialize(binaryDateTime, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.Date, llsdDateTime.getType());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2008, 0, 1, 20, 10, 31);
        Date dateLocal = llsdDateTime.AsDate();
        Assert.assertEquals(cal.getTimeInMillis(), dateLocal.getTime());
    }

    public void testSerializeDateTime() throws IOException, ParseException
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2008, 0, 1, 20, 10, 31);
        OSD llsdDate = OSD.FromDate(cal.getTime());
        byte[] binaryDateSerialized = OSDParser.serializeToBytes(llsdDate, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryDateTime, binaryDateSerialized));

        // check if a *local* time can be serialized and deserialized
        Calendar calOne = Calendar.getInstance();
        calOne.clear();
        calOne.set(2009, 12, 30, 8, 25, 10);
        OSD llsdDateOne = OSD.FromDate(calOne.getTime());
        byte[] binaryDateOneSerialized = OSDParser.serializeToBytes(llsdDateOne, OSDFormat.Binary);
        OSD llsdDateOneDS = OSDParser.deserialize(binaryDateOneSerialized, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Date, llsdDateOneDS.getType());
        Assert.assertEquals(calOne.getTime(), llsdDateOneDS.AsDate());

        Calendar calTwo = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calTwo.clear();
        calTwo.set(2010, 11, 11, 10, 8, 20);
        OSD llsdDateTwo = OSD.FromDate(calTwo.getTime());
        byte[] binaryDateTwoSerialized = OSDParser.serializeToBytes(llsdDateTwo, OSDFormat.Binary);
        OSD llsdDateTwoDS = OSDParser.deserialize(binaryDateTwoSerialized, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Date, llsdDateOneDS.getType());
        Assert.assertEquals(calTwo.getTime(), llsdDateTwoDS.AsDate());
    }

    // Data for empty array { }
    private static byte[] binaryEmptyArrayValue = { 0x5b, 0x0, 0x0, 0x0, 0x0, 0x5d };
    // Encoding header + num of elements + tail
    private static byte[] binaryEmptyArray = concat(llsdBinaryHead, binaryEmptyArrayValue);
    // Data for simple array { 0 }
    private static byte[] binarySimpleArrayValue = { 0x5b, 0x0, 0x0, 0x0, 0x1, // Encoding header + num of elements
                                 0x69, 0x0, 0x0, 0x0, 0x0, 0x5d };
    private static byte[] binarySimpleArray = concat(llsdBinaryHead, binarySimpleArrayValue);

    // Data for simple array { 0, 0 }
    private static byte[] binarySimpleArrayTwoValue = { 0x5b, 0x0, 0x0, 0x0, 0x2, // Encoding header + num of elements
                                 0x69, 0x0, 0x0, 0x0, 0x0, 
                                 0x69, 0x0, 0x0, 0x0, 0x0, 0x5d };
    private static byte[] binarySimpleArrayTwo = concat(llsdBinaryHead, binarySimpleArrayTwoValue);

    public void testDeserializeArray() throws IOException, ParseException
    {
        OSD llsdEmptyArray = OSDParser.deserialize(binaryEmptyArray, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Array, llsdEmptyArray.getType());
        OSDArray llsdEmptyArrayArray = (OSDArray)llsdEmptyArray;
        Assert.assertEquals(0, llsdEmptyArrayArray.size());


        OSD llsdSimpleArray = OSDParser.deserialize(binarySimpleArray, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Array, llsdSimpleArray.getType());
        OSDArray llsdArray = (OSDArray)llsdSimpleArray;
        Assert.assertEquals(OSDType.Integer, llsdArray.get(0).getType());
        Assert.assertEquals(0, llsdArray.get(0).AsInteger());


        OSD llsdSimpleArrayTwo = OSDParser.deserialize(binarySimpleArrayTwo, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Array, llsdSimpleArrayTwo.getType());
        OSDArray llsdArrayTwo = (OSDArray)llsdSimpleArrayTwo;
        Assert.assertEquals(2, llsdArrayTwo.size());

        Assert.assertEquals(OSDType.Integer, llsdArrayTwo.get(0).getType());
        Assert.assertEquals(0, llsdArrayTwo.get(0).AsInteger());
        Assert.assertEquals(OSDType.Integer, llsdArrayTwo.get(1).getType());
        Assert.assertEquals(0, llsdArrayTwo.get(1).AsInteger());
    }

    public void testSerializeArray() throws IOException
    {
        OSDArray llsdEmptyArray = new OSDArray();
        byte[] binaryEmptyArraySerialized = OSDParser.serializeToBytes(llsdEmptyArray, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryEmptyArray, binaryEmptyArraySerialized));

        binaryEmptyArraySerialized = OSDParser.serializeToBytes(llsdEmptyArray, OSDFormat.Binary, false);
        Assert.assertTrue(Arrays.equals(binaryEmptyArrayValue, binaryEmptyArraySerialized));

        OSDArray llsdSimpleArray = new OSDArray();
        llsdSimpleArray.add(OSD.FromInteger(0));
        byte[] binarySimpleArraySerialized = OSDParser.serializeToBytes(llsdSimpleArray, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binarySimpleArray, binarySimpleArraySerialized));

        binarySimpleArraySerialized = OSDParser.serializeToBytes(llsdSimpleArray, OSDFormat.Binary, false);
        Assert.assertTrue(Arrays.equals(binarySimpleArrayValue, binarySimpleArraySerialized));

        OSDArray llsdSimpleArrayTwo = new OSDArray();
        llsdSimpleArrayTwo.add(OSD.FromInteger(0));
        llsdSimpleArrayTwo.add(OSD.FromInteger(0));
        byte[] binarySimpleArrayTwoSerialized = OSDParser.serializeToBytes(llsdSimpleArrayTwo, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binarySimpleArrayTwo, binarySimpleArrayTwoSerialized));

        binarySimpleArrayTwoSerialized = OSDParser.serializeToBytes(llsdSimpleArrayTwo, OSDFormat.Binary, false);
        Assert.assertTrue(Arrays.equals(binarySimpleArrayTwoValue, binarySimpleArrayTwoSerialized));
    }

    // Data for empty dictionary { }
    private static byte[] binaryEmptyMapValue = { 0x7b, 0x0, 0x0, 0x0, 0x0, 0x7d };
    private static byte[] binaryEmptyMap = concat(llsdBinaryHead, binaryEmptyMapValue);

    // Data for simple dictionary { test = 0 }
    private static byte[] binarySimpleMapValue = { 0x7b, 0x0, 0x0, 0x0, 0x1, // Encoding header + num of elements
                            0x6b, 0x0, 0x0, 0x0, 0x4, // 'k' + keylength 
                            0x74, 0x65, 0x73, 0x74,  // key 'test' 
                            0x69, 0x0, 0x0, 0x0, 0x0, // i + '0'
                            0x7d };
    private static byte[] binarySimpleMap = concat(llsdBinaryHead, binarySimpleMapValue);

    // Data for simple dictionary { t0st = 241, tes1 = "aha", test = undef }
    private static byte[] binarySimpleMapTwoValue = { 0x7b, 0x0, 0x0, 0x0, 0x3, // Encoding header + num of elements
        0x6b, 0x0, 0x0, 0x0, 0x4, // 'k' + keylength 
        0x74, 0x65, 0x73, 0x74,  // key 'test'
        0x21, // undef
        0x6b, 0x0, 0x0, 0x0, 0x4, // k + keylength 
        0x74, 0x65, 0x73, 0x31, // key 'tes1' 
        0x73, 0x0, 0x0, 0x0, 0x3, // string head + length
        0x61, 0x68, 0x61, // 'aha' 
        0x6b, 0x0, 0x0, 0x0, 0x4, // k + keylength 
        0x74, 0x30, 0x73, 0x74,  // key 't0st'
        0x69, 0x0, 0x0, 0x0, (byte) 0xf1, // integer 241
        0x7d };
    private static byte[] binarySimpleMapTwo = concat(llsdBinaryHead, binarySimpleMapTwoValue);

    public void testDeserializeDictionary() throws IOException, ParseException
    {
        OSDMap llsdEmptyMap = (OSDMap)OSDParser.deserialize(binaryEmptyMap, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Map, llsdEmptyMap.getType());
        Assert.assertEquals(0, llsdEmptyMap.size());

        OSDMap llsdSimpleMap = (OSDMap)OSDParser.deserialize(binarySimpleMap, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Map, llsdSimpleMap.getType());
        Assert.assertEquals(1, llsdSimpleMap.size());
        Assert.assertEquals(OSDType.Integer, llsdSimpleMap.get("test").getType());
        Assert.assertEquals(0, llsdSimpleMap.get("test").AsInteger());

        OSDMap llsdSimpleMapTwo = (OSDMap)OSDParser.deserialize(binarySimpleMapTwo, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Map, llsdSimpleMapTwo.getType());
        Assert.assertEquals(3, llsdSimpleMapTwo.size());
        Assert.assertEquals(OSDType.Unknown, llsdSimpleMapTwo.get("test").getType());
        Assert.assertEquals(OSDType.String, llsdSimpleMapTwo.get("tes1").getType());
        Assert.assertEquals("aha", llsdSimpleMapTwo.get("tes1").AsString());
        Assert.assertEquals(OSDType.Integer, llsdSimpleMapTwo.get("t0st").getType());
        Assert.assertEquals(241, llsdSimpleMapTwo.get("t0st").AsInteger());
    }

    public void testSerializeDictionary() throws IOException, ParseException, XmlPullParserException
    {
        OSDMap llsdEmptyMap = new OSDMap();
        byte[] binaryEmptyMapSerialized = OSDParser.serializeToBytes(llsdEmptyMap, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binaryEmptyMap, binaryEmptyMapSerialized));

        OSDMap llsdSimpleMap = new OSDMap();
        llsdSimpleMap.put("test", OSD.FromInteger(0));
        byte[] binarySimpleMapSerialized = OSDParser.serializeToBytes(llsdSimpleMap, OSDFormat.Binary);
        Assert.assertTrue(Arrays.equals(binarySimpleMap, binarySimpleMapSerialized));

        OSDMap llsdSimpleMapTwo = new OSDMap();
        llsdSimpleMapTwo.put("t0st", OSD.FromInteger(241));
        llsdSimpleMapTwo.put("tes1", OSD.FromString("aha"));
        llsdSimpleMapTwo.put("test", new OSD());
        byte[] binarySimpleMapTwoSerialized = OSDParser.serializeToBytes(llsdSimpleMapTwo, OSDFormat.Binary);

        // We dont compare here to the original serialized value, because, as maps dont preserve order,
        // the original serialized value is not *exactly* the same. Instead we compare to a deserialized
        // version created by this deserializer.
        OSDMap llsdSimpleMapDeserialized = (OSDMap)OSDParser.deserialize(binarySimpleMapTwoSerialized, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Map, llsdSimpleMapDeserialized.getType());
        Assert.assertEquals(3, llsdSimpleMapDeserialized.size());
        Assert.assertEquals(OSDType.Integer, llsdSimpleMapDeserialized.get("t0st").getType());
        Assert.assertEquals(241, llsdSimpleMapDeserialized.get("t0st").AsInteger());
        Assert.assertEquals(OSDType.String, llsdSimpleMapDeserialized.get("tes1").getType());
        Assert.assertEquals("aha", llsdSimpleMapDeserialized.get("tes1").AsString());
        Assert.assertEquals(OSDType.Unknown, llsdSimpleMapDeserialized.get("test").getType());

        // we also test for a 4byte key character.
        String xml = "<x>&#x10137;</x>";
        Reader reader = new StringReader(xml);
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(reader);
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "x");
        String content = parser.nextText();

        OSDMap llsdSimpleMapThree = new OSDMap();
        OSD llsdSimpleValue = OSD.FromString(content);
        llsdSimpleMapThree.put(content, llsdSimpleValue);
        Assert.assertEquals(content, llsdSimpleMapThree.get(content).AsString());

        byte[] binarySimpleMapThree = OSDParser.serializeToBytes(llsdSimpleMapThree, OSDFormat.Binary);
        OSDMap llsdSimpleMapThreeDS = (OSDMap)OSDParser.deserialize(binarySimpleMapThree, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Map, llsdSimpleMapThreeDS.getType());
        Assert.assertEquals(1, llsdSimpleMapThreeDS.size());
        Assert.assertEquals(content, llsdSimpleMapThreeDS.get(content).AsString());
    }

    private static byte[] binaryNestedValue = { 0x5b, 0x0, 0x0, 0x0, 0x3, 
        0x7b, 0x0, 0x0, 0x0, 0x2, 
        0x6b, 0x0, 0x0, 0x0, 0x4, 
        0x74, 0x65, 0x73, 0x74, 
        0x73, 0x0, 0x0, 0x0, 0x4, 
        0x77, 0x68, 0x61, 0x74, 
        0x6b, 0x0, 0x0, 0x0, 0x4, 
        0x74, 0x30, 0x73, 
        0x74, 0x5b, 0x0, 0x0, 0x0, 0x2,
        0x69, 0x0, 0x0, 0x0, 0x1,
        0x69, 0x0, 0x0, 0x0, 0x2,
        0x5d, 0x7d, 0x69, 0x0, 0x0, 0x0, 
        0x7c, 0x69, 0x0, 0x0, 0x3, (byte) 0xdb, 
        0x5d };
    private static byte[] binaryNested = concat(llsdBinaryHead, binaryNestedValue);

    public void testDeserializeNestedComposite() throws IOException, ParseException
    {
        OSD llsdNested = OSDParser.deserialize(binaryNested, Helpers.UTF8_ENCODING);
        Assert.assertEquals(OSDType.Array, llsdNested.getType());
        OSDArray llsdArray = (OSDArray)llsdNested;
        Assert.assertEquals(3, llsdArray.size());

        OSDMap llsdMap = (OSDMap)llsdArray.get(0);
        Assert.assertEquals(OSDType.Map, llsdMap.getType());
        Assert.assertEquals(2, llsdMap.size());

        OSDArray llsdNestedArray = (OSDArray)llsdMap.get("t0st");
        Assert.assertEquals(OSDType.Array, llsdNestedArray.getType());
        OSDInteger llsdNestedIntOne = (OSDInteger)llsdNestedArray.get(0);
        Assert.assertEquals(OSDType.Integer, llsdNestedIntOne.getType());
        Assert.assertEquals(1, llsdNestedIntOne.AsInteger());
        OSDInteger llsdNestedIntTwo = (OSDInteger)llsdNestedArray.get(1);
        Assert.assertEquals(OSDType.Integer, llsdNestedIntTwo.getType());
        Assert.assertEquals(2, llsdNestedIntTwo.AsInteger());

        OSDString llsdString = (OSDString)llsdMap.get("test");
        Assert.assertEquals(OSDType.String, llsdString.getType());
        Assert.assertEquals("what", llsdString.AsString());

        OSDInteger llsdIntOne = (OSDInteger)llsdArray.get(1);
        Assert.assertEquals(OSDType.Integer, llsdIntOne.getType());
        Assert.assertEquals(124, llsdIntOne.AsInteger());
        OSDInteger llsdIntTwo = (OSDInteger)llsdArray.get(2);
        Assert.assertEquals(OSDType.Integer, llsdIntTwo.getType());
        Assert.assertEquals(987, llsdIntTwo.AsInteger());
    }

    public void testSerializeNestedComposite() throws IOException, ParseException
    {
        OSDArray llsdNested = new OSDArray();
        OSDMap llsdMap = new OSDMap();
        OSDArray llsdArray = new OSDArray();
        llsdArray.add(OSD.FromInteger(1));
        llsdArray.add(OSD.FromInteger(2));
        llsdMap.put("t0st", llsdArray);
        llsdMap.put("test", OSD.FromString("what"));
        llsdNested.add(llsdMap);
        llsdNested.add(OSD.FromInteger(124));
        llsdNested.add(OSD.FromInteger(987));

        byte[] binaryNestedSerialized = OSDParser.serializeToBytes(llsdNested, OSDFormat.Binary);
        // Because maps don't preserve order, we compare here to a deserialized value. 
        OSDArray llsdNestedDeserialized = (OSDArray)OSDParser.deserialize(binaryNestedSerialized, Helpers.ASCII_ENCODING);
        Assert.assertEquals(OSDType.Array, llsdNestedDeserialized.getType());
        Assert.assertEquals(3, llsdNestedDeserialized.size());

        OSDMap llsdMapDeserialized = (OSDMap)llsdNestedDeserialized.get(0);
        Assert.assertEquals(OSDType.Map, llsdMapDeserialized.getType());
        Assert.assertEquals(2, llsdMapDeserialized.size());
        Assert.assertEquals(OSDType.Array, llsdMapDeserialized.get("t0st").getType());

        OSDArray llsdNestedArray = (OSDArray)llsdMapDeserialized.get("t0st");
        Assert.assertEquals(OSDType.Array, llsdNestedArray.getType());
        Assert.assertEquals(2, llsdNestedArray.size());
        Assert.assertEquals(OSDType.Integer, llsdNestedArray.get(0).getType());
        Assert.assertEquals(1, llsdNestedArray.get(0).AsInteger());
        Assert.assertEquals(OSDType.Integer, llsdNestedArray.get(1).getType());
        Assert.assertEquals(2, llsdNestedArray.get(1).AsInteger());

        Assert.assertEquals(OSDType.String, llsdMapDeserialized.get("test").getType());
        Assert.assertEquals("what", llsdMapDeserialized.get("test").AsString());

        Assert.assertEquals(OSDType.Integer, llsdNestedDeserialized.get(1).getType());
        Assert.assertEquals(124, llsdNestedDeserialized.get(1).AsInteger());

        Assert.assertEquals(OSDType.Integer, llsdNestedDeserialized.get(2).getType());
        Assert.assertEquals(987, llsdNestedDeserialized.get(2).AsInteger());
    }

    public void testSerializeLongMessage() throws IOException, ParseException
    {
        // each 80 chars
        String sOne = "asdklfjasadlfkjaerotiudfgjkhsdklgjhsdklfghasdfklhjasdfkjhasdfkljahsdfjklaasdfkj8";
        String sTwo = "asdfkjlaaweoiugsdfjkhsdfg,.mnasdgfkljhrtuiohfgl�kajsdfoiwghjkdlaaaaseldkfjgheus9";

        OSD stringOne = OSD.FromString( sOne );
        OSD stringTwo = OSD.FromString(sTwo);

        OSDMap llsdMap = new OSDMap();
        llsdMap.put("testOne", stringOne);
        llsdMap.put("testTwo", stringTwo);
        llsdMap.put("testThree", stringOne);
        llsdMap.put("testFour", stringTwo);
        llsdMap.put("testFive", stringOne);
        llsdMap.put("testSix", stringTwo);
        llsdMap.put("testSeven", stringOne);
        llsdMap.put("testEight", stringTwo);
        llsdMap.put("testNine", stringOne);
        llsdMap.put("testTen", stringTwo);

        byte[] binaryData = OSDParser.serializeToBytes(llsdMap, OSDFormat.Binary);

        OSDMap llsdMapDS = (OSDMap)OSDParser.deserialize(binaryData);
        Assert.assertEquals(OSDType.Map, llsdMapDS.getType());
        Assert.assertEquals(10, llsdMapDS.size());
        Assert.assertEquals(sOne, llsdMapDS.get("testOne").AsString());
        Assert.assertEquals(sTwo, llsdMapDS.get("testTwo").AsString());
        Assert.assertEquals(sOne, llsdMapDS.get("testThree").AsString());
        Assert.assertEquals(sTwo, llsdMapDS.get("testFour").AsString());
        Assert.assertEquals(sOne, llsdMapDS.get("testFive").AsString());
        Assert.assertEquals(sTwo, llsdMapDS.get("testSix").AsString());
        Assert.assertEquals(sOne, llsdMapDS.get("testSeven").AsString());
        Assert.assertEquals(sTwo, llsdMapDS.get("testEight").AsString());
        Assert.assertEquals(sOne, llsdMapDS.get("testNine").AsString());
        Assert.assertEquals(sTwo, llsdMapDS.get("testTen").AsString());
    }

    private static byte[] concat(byte[] first, byte[] second)
    {
	    byte[] result = Arrays.copyOf(first, first.length + second.length);
	    System.arraycopy(second, 0, result, first.length, second.length);
	    return result;
    }
}
