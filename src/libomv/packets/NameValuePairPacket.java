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
import libomv.types.PacketHeader;
import libomv.types.PacketFrequency;
import libomv.types.UUID;
import libomv.types.OverflowException;

public class NameValuePairPacket extends Packet
{
    public class TaskDataBlock
    {
        public UUID ID = null;

        public int getLength(){
            return 16;
        }

        public TaskDataBlock() { }
        public TaskDataBlock(ByteBuffer bytes)
        {
            ID = new UUID(bytes);
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            ID.GetBytes(bytes);
        }

        public String toString()
        {
            String output = "-- TaskData --\n";
            try {
                output += "ID: " + ID.toString() + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public TaskDataBlock createTaskDataBlock() {
         return new TaskDataBlock();
    }

    public class NameValueDataBlock
    {
        private byte[] _nvpair;
        public byte[] getNVPair() {
            return _nvpair;
        }

        public void setNVPair(byte[] value) throws Exception {
            if (value == null) {
                _nvpair = null;
            }
            if (value.length > 1024) {
                throw new OverflowException("Value exceeds 1024 characters");
            }
            else {
                _nvpair = new byte[value.length];
                System.arraycopy(value, 0, _nvpair, 0, value.length);
            }
        }


        public int getLength(){
            int length = 0;
            if (getNVPair() != null) { length += 2 + getNVPair().length; }
            return length;
        }

        public NameValueDataBlock() { }
        public NameValueDataBlock(ByteBuffer bytes)
        {
            int length;
            length = (int)(bytes.getShort()) & 0xFFFF;
            _nvpair = new byte[length];
            bytes.get(_nvpair); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putShort((short)_nvpair.length);
            bytes.put(_nvpair);
        }

        public String toString()
        {
            String output = "-- NameValueData --\n";
            try {
                output += Helpers.FieldToString(_nvpair, "NVPair") + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public NameValueDataBlock createNameValueDataBlock() {
         return new NameValueDataBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.NameValuePair; }
    public TaskDataBlock TaskData;
    public NameValueDataBlock[] NameValueData;

    public NameValuePairPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)329);
        header.setReliable(true);
        TaskData = new TaskDataBlock();
        NameValueData = new NameValueDataBlock[0];
    }

    public NameValuePairPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        TaskData = new TaskDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        NameValueData = new NameValueDataBlock[count];
        for (int j = 0; j < count; j++)
        { NameValueData[j] = new NameValueDataBlock(bytes); }
     }

    public NameValuePairPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        TaskData = new TaskDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        NameValueData = new NameValueDataBlock[count];
        for (int j = 0; j < count; j++)
        { NameValueData[j] = new NameValueDataBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length += TaskData.getLength();
        length++;
        for (int j = 0; j < NameValueData.length; j++) { length += NameValueData[j].getLength(); }
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
        TaskData.ToBytes(bytes);
        bytes.put((byte)NameValueData.length);
        for (int j = 0; j < NameValueData.length; j++) { NameValueData[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- NameValuePair ---\n";
        output += TaskData.toString() + "\n";
        for (int j = 0; j < NameValueData.length; j++)
        {
            output += NameValueData[j].toString() + "\n";
        }
        return output;
    }
}
