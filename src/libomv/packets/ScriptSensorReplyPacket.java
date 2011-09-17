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
import libomv.types.Vector3;
import libomv.types.Quaternion;
import libomv.types.OverflowException;

public class ScriptSensorReplyPacket extends Packet
{
    public class RequesterBlock
    {
        public UUID SourceID = null;

        public int getLength(){
            return 16;
        }

        public RequesterBlock() { }
        public RequesterBlock(ByteBuffer bytes)
        {
            SourceID = new UUID(bytes);
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            SourceID.GetBytes(bytes);
        }

        public String toString()
        {
            String output = "-- Requester --\n";
            try {
                output += "SourceID: " + SourceID.toString() + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public RequesterBlock createRequesterBlock() {
         return new RequesterBlock();
    }

    public class SensedDataBlock
    {
        public UUID ObjectID = null;
        public UUID OwnerID = null;
        public UUID GroupID = null;
        public Vector3 Position = null;
        public Vector3 Velocity = null;
        public Quaternion Rotation = null;
        private byte[] _name;
        public byte[] getName() {
            return _name;
        }

        public void setName(byte[] value) throws Exception {
            if (value == null) {
                _name = null;
            }
            if (value.length > 255) {
                throw new OverflowException("Value exceeds 255 characters");
            }
            else {
                _name = new byte[value.length];
                System.arraycopy(value, 0, _name, 0, value.length);
            }
        }

        public int Type = 0;
        public float Range = 0;

        public int getLength(){
            int length = 92;
            if (getName() != null) { length += 1 + getName().length; }
            return length;
        }

        public SensedDataBlock() { }
        public SensedDataBlock(ByteBuffer bytes)
        {
            int length;
            ObjectID = new UUID(bytes);
            OwnerID = new UUID(bytes);
            GroupID = new UUID(bytes);
            Position = new Vector3(bytes); 
            Velocity = new Vector3(bytes); 
            Rotation = new Quaternion(bytes, true); 
            length = (int)(bytes.get()) & 0xFF;
            _name = new byte[length];
            bytes.get(_name); 
            Type = bytes.getInt(); 
            Range = bytes.getFloat();
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            ObjectID.GetBytes(bytes);
            OwnerID.GetBytes(bytes);
            GroupID.GetBytes(bytes);
            Position.GetBytes(bytes);
            Velocity.GetBytes(bytes);
            Rotation.GetBytes(bytes);
            bytes.put((byte)_name.length);
            bytes.put(_name);
            bytes.putInt(Type);
            bytes.putFloat(Range);
        }

        public String toString()
        {
            String output = "-- SensedData --\n";
            try {
                output += "ObjectID: " + ObjectID.toString() + "\n";
                output += "OwnerID: " + OwnerID.toString() + "\n";
                output += "GroupID: " + GroupID.toString() + "\n";
                output += "Position: " + Position.toString() + "\n";
                output += "Velocity: " + Velocity.toString() + "\n";
                output += "Rotation: " + Rotation.toString() + "\n";
                output += Helpers.FieldToString(_name, "Name") + "\n";
                output += "Type: " + Integer.toString(Type) + "\n";
                output += "Range: " + Float.toString(Range) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public SensedDataBlock createSensedDataBlock() {
         return new SensedDataBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.ScriptSensorReply; }
    public RequesterBlock Requester;
    public SensedDataBlock[] SensedData;

    public ScriptSensorReplyPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)248);
        header.setReliable(true);
        Requester = new RequesterBlock();
        SensedData = new SensedDataBlock[0];
    }

    public ScriptSensorReplyPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        Requester = new RequesterBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        SensedData = new SensedDataBlock[count];
        for (int j = 0; j < count; j++)
        { SensedData[j] = new SensedDataBlock(bytes); }
     }

    public ScriptSensorReplyPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        Requester = new RequesterBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        SensedData = new SensedDataBlock[count];
        for (int j = 0; j < count; j++)
        { SensedData[j] = new SensedDataBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length += Requester.getLength();
        length++;
        for (int j = 0; j < SensedData.length; j++) { length += SensedData[j].getLength(); }
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
        Requester.ToBytes(bytes);
        bytes.put((byte)SensedData.length);
        for (int j = 0; j < SensedData.length; j++) { SensedData[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- ScriptSensorReply ---\n";
        output += Requester.toString() + "\n";
        for (int j = 0; j < SensedData.length; j++)
        {
            output += SensedData[j].toString() + "\n";
        }
        return output;
    }
}
