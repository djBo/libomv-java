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
import libomv.types.PacketHeader;
import libomv.types.UUID;
import libomv.types.OverflowException;

public class UUIDNameReplyPacket extends Packet
{
    public class UUIDNameBlockBlock
    {
        public UUID ID = null;
        private byte[] _firstname;
        public byte[] getFirstName() {
            return _firstname;
        }

        public void setFirstName(byte[] value) throws Exception {
            if (value == null) {
                _firstname = null;
            }
            if (value.length > 255) {
                throw new OverflowException("Value exceeds 255 characters");
            }
            else {
                _firstname = new byte[value.length];
                System.arraycopy(value, 0, _firstname, 0, value.length);
            }
        }

        private byte[] _lastname;
        public byte[] getLastName() {
            return _lastname;
        }

        public void setLastName(byte[] value) throws Exception {
            if (value == null) {
                _lastname = null;
            }
            if (value.length > 255) {
                throw new OverflowException("Value exceeds 255 characters");
            }
            else {
                _lastname = new byte[value.length];
                System.arraycopy(value, 0, _lastname, 0, value.length);
            }
        }


        public int getLength(){
            int length = 16;
            if (getFirstName() != null) { length += 1 + getFirstName().length; }
            if (getLastName() != null) { length += 1 + getLastName().length; }
            return length;
        }

        public UUIDNameBlockBlock() { }
        public UUIDNameBlockBlock(ByteBuffer bytes)
        {
            int length;
            ID = new UUID(bytes);
            length = (int)(bytes.get()) & 0xFF;
            _firstname = new byte[length];
            bytes.get(_firstname); 
            length = (int)(bytes.get()) & 0xFF;
            _lastname = new byte[length];
            bytes.get(_lastname); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            ID.GetBytes(bytes);
            bytes.put((byte)_firstname.length);
            bytes.put(_firstname);
            bytes.put((byte)_lastname.length);
            bytes.put(_lastname);
        }

        public String toString()
        {
            String output = "-- UUIDNameBlock --\n";
            try {
                output += "ID: " + ID.toString() + "\n";
                output += Helpers.FieldToString(_firstname, "FirstName") + "\n";
                output += Helpers.FieldToString(_lastname, "LastName") + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public UUIDNameBlockBlock createUUIDNameBlockBlock() {
         return new UUIDNameBlockBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.UUIDNameReply; }
    public UUIDNameBlockBlock[] UUIDNameBlock;

    public UUIDNameReplyPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)236);
        header.setReliable(true);
        UUIDNameBlock = new UUIDNameBlockBlock[0];
    }

    public UUIDNameReplyPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        int count = (int)bytes.get() & 0xFF;
        UUIDNameBlock = new UUIDNameBlockBlock[count];
        for (int j = 0; j < count; j++)
        { UUIDNameBlock[j] = new UUIDNameBlockBlock(bytes); }
     }

    public UUIDNameReplyPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        int count = (int)bytes.get() & 0xFF;
        UUIDNameBlock = new UUIDNameBlockBlock[count];
        for (int j = 0; j < count; j++)
        { UUIDNameBlock[j] = new UUIDNameBlockBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length++;
        for (int j = 0; j < UUIDNameBlock.length; j++) { length += UUIDNameBlock[j].getLength(); }
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
        bytes.put((byte)UUIDNameBlock.length);
        for (int j = 0; j < UUIDNameBlock.length; j++) { UUIDNameBlock[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- UUIDNameReply ---\n";
        for (int j = 0; j < UUIDNameBlock.length; j++)
        {
            output += UUIDNameBlock[j].toString() + "\n";
        }
        return output;
    }
}
