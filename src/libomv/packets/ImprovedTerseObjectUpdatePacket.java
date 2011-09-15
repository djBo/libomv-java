/**
 * Copyright (c) 2006, Second Life Reverse Engineering Team
 * Portions Copyright (c) 2006, Lateral Arts Limited
 * Portions Copyright (c) 2009-2011, Frederick Martian
 * All rights reserved.
 *
 * - Redistribution and use in source and binary forms, with or without 
 *   modification, are permitted provided that the following conditions are met:
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
package libomv.packets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import libomv.utils.Helpers;
import libomv.types.PacketFrequency;
import libomv.types.OverflowException;
import libomv.types.PacketHeader;

public class ImprovedTerseObjectUpdatePacket extends Packet
{
    public class RegionDataBlock
    {
        public long RegionHandle = 0;
        public short TimeDilation = 0;

        public int getLength(){
            return 10;
        }

        public RegionDataBlock() { }
        public RegionDataBlock(ByteBuffer bytes)
        {
            RegionHandle = bytes.getLong(); 
            TimeDilation = bytes.getShort(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putLong(RegionHandle);
            bytes.putShort(TimeDilation);
        }

        public String toString()
        {
            String output = "-- RegionData --\n";
            try {
                output += "RegionHandle: " + Long.toString(RegionHandle) + "\n";
                output += "TimeDilation: " + Short.toString(TimeDilation) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public RegionDataBlock createRegionDataBlock() {
         return new RegionDataBlock();
    }

    public class ObjectDataBlock
    {
        private byte[] _data;
        public byte[] getData() {
            return _data;
        }

        public void setData(byte[] value) throws Exception {
            if (value == null) {
                _data = null;
            }
            if (value.length > 255) {
                throw new OverflowException("Value exceeds 255 characters");
            }
            else {
                _data = new byte[value.length];
                System.arraycopy(value, 0, _data, 0, value.length);
            }
        }

        private byte[] _textureentry;
        public byte[] getTextureEntry() {
            return _textureentry;
        }

        public void setTextureEntry(byte[] value) throws Exception {
            if (value == null) {
                _textureentry = null;
            }
            if (value.length > 1024) {
                throw new OverflowException("Value exceeds 1024 characters");
            }
            else {
                _textureentry = new byte[value.length];
                System.arraycopy(value, 0, _textureentry, 0, value.length);
            }
        }


        public int getLength(){
            int length = 0;
            if (getData() != null) { length += 1 + getData().length; }
            if (getTextureEntry() != null) { length += 2 + getTextureEntry().length; }
            return length;
        }

        public ObjectDataBlock() { }
        public ObjectDataBlock(ByteBuffer bytes)
        {
            int length;
            length = (int)(bytes.get()) & 0xFF;
            _data = new byte[length];
            bytes.get(_data); 
            length = (int)(bytes.getShort()) & 0xFFFF;
            _textureentry = new byte[length];
            bytes.get(_textureentry); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.put((byte)_data.length);
            bytes.put(_data);
            bytes.putShort((short)_textureentry.length);
            bytes.put(_textureentry);
        }

        public String toString()
        {
            String output = "-- ObjectData --\n";
            try {
                output += Helpers.FieldToString(_data, "Data") + "\n";
                output += Helpers.FieldToString(_textureentry, "TextureEntry") + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public ObjectDataBlock createObjectDataBlock() {
         return new ObjectDataBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.ImprovedTerseObjectUpdate; }
    public RegionDataBlock RegionData;
    public ObjectDataBlock[] ObjectData;

    public ImprovedTerseObjectUpdatePacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)15);
        header.setReliable(true);
        RegionData = new RegionDataBlock();
        ObjectData = new ObjectDataBlock[0];
    }

    public ImprovedTerseObjectUpdatePacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        RegionData = new RegionDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        ObjectData = new ObjectDataBlock[count];
        for (int j = 0; j < count; j++)
        { ObjectData[j] = new ObjectDataBlock(bytes); }
     }

    public ImprovedTerseObjectUpdatePacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        RegionData = new RegionDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        ObjectData = new ObjectDataBlock[count];
        for (int j = 0; j < count; j++)
        { ObjectData[j] = new ObjectDataBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length += RegionData.getLength();
        length++;
        for (int j = 0; j < ObjectData.length; j++) { length += ObjectData[j].getLength(); }
        if (header.AckList.length > 0) {
            length += header.AckList.length * 4 + 1;
        }
        return length;
    }

    public ByteBuffer ToBytes() throws Exception
    {
        ByteBuffer bytes = ByteBuffer.allocate(getLength());
        header.ToBytes(bytes);
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        RegionData.ToBytes(bytes);
        bytes.put((byte)ObjectData.length);
        for (int j = 0; j < ObjectData.length; j++) { ObjectData[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- ImprovedTerseObjectUpdate ---\n";
        output += RegionData.toString() + "\n";
        for (int j = 0; j < ObjectData.length; j++)
        {
            output += ObjectData[j].toString() + "\n";
        }
        return output;
    }
}
