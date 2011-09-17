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
import libomv.types.OverflowException;

public class ScriptDataReplyPacket extends Packet
{
    public class DataBlockBlock
    {
        public long Hash = 0;
        private byte[] _reply;
        public byte[] getReply() {
            return _reply;
        }

        public void setReply(byte[] value) throws Exception {
            if (value == null) {
                _reply = null;
            }
            if (value.length > 1024) {
                throw new OverflowException("Value exceeds 1024 characters");
            }
            else {
                _reply = new byte[value.length];
                System.arraycopy(value, 0, _reply, 0, value.length);
            }
        }


        public int getLength(){
            int length = 8;
            if (getReply() != null) { length += 2 + getReply().length; }
            return length;
        }

        public DataBlockBlock() { }
        public DataBlockBlock(ByteBuffer bytes)
        {
            int length;
            Hash = bytes.getLong(); 
            length = (int)(bytes.getShort()) & 0xFFFF;
            _reply = new byte[length];
            bytes.get(_reply); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putLong(Hash);
            bytes.putShort((short)_reply.length);
            bytes.put(_reply);
        }

        public String toString()
        {
            String output = "-- DataBlock --\n";
            try {
                output += "Hash: " + Long.toString(Hash) + "\n";
                output += Helpers.FieldToString(_reply, "Reply") + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public DataBlockBlock createDataBlockBlock() {
         return new DataBlockBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.ScriptDataReply; }
    public DataBlockBlock[] DataBlock;

    public ScriptDataReplyPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)338);
        header.setReliable(true);
        DataBlock = new DataBlockBlock[0];
    }

    public ScriptDataReplyPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        int count = (int)bytes.get() & 0xFF;
        DataBlock = new DataBlockBlock[count];
        for (int j = 0; j < count; j++)
        { DataBlock[j] = new DataBlockBlock(bytes); }
     }

    public ScriptDataReplyPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        int count = (int)bytes.get() & 0xFF;
        DataBlock = new DataBlockBlock[count];
        for (int j = 0; j < count; j++)
        { DataBlock[j] = new DataBlockBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length++;
        for (int j = 0; j < DataBlock.length; j++) { length += DataBlock[j].getLength(); }
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
        bytes.put((byte)DataBlock.length);
        for (int j = 0; j < DataBlock.length; j++) { DataBlock[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- ScriptDataReply ---\n";
        for (int j = 0; j < DataBlock.length; j++)
        {
            output += DataBlock[j].toString() + "\n";
        }
        return output;
    }
}
