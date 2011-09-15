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

import libomv.types.PacketFrequency;
import libomv.types.PacketHeader;
import libomv.types.UUID;
import libomv.types.Vector3;

public class SoundTriggerPacket extends Packet
{
    public class SoundDataBlock
    {
        public UUID SoundID = null;
        public UUID OwnerID = null;
        public UUID ObjectID = null;
        public UUID ParentID = null;
        public long Handle = 0;
        public Vector3 Position = null;
        public float Gain = 0;

        public int getLength(){
            return 88;
        }

        public SoundDataBlock() { }
        public SoundDataBlock(ByteBuffer bytes)
        {
            SoundID = new UUID(bytes);
            OwnerID = new UUID(bytes);
            ObjectID = new UUID(bytes);
            ParentID = new UUID(bytes);
            Handle = bytes.getLong(); 
            Position = new Vector3(bytes); 
            Gain = bytes.getFloat();
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            SoundID.GetBytes(bytes);
            OwnerID.GetBytes(bytes);
            ObjectID.GetBytes(bytes);
            ParentID.GetBytes(bytes);
            bytes.putLong(Handle);
            Position.GetBytes(bytes);
            bytes.putFloat(Gain);
        }

        public String toString()
        {
            String output = "-- SoundData --\n";
            try {
                output += "SoundID: " + SoundID.toString() + "\n";
                output += "OwnerID: " + OwnerID.toString() + "\n";
                output += "ObjectID: " + ObjectID.toString() + "\n";
                output += "ParentID: " + ParentID.toString() + "\n";
                output += "Handle: " + Long.toString(Handle) + "\n";
                output += "Position: " + Position.toString() + "\n";
                output += "Gain: " + Float.toString(Gain) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public SoundDataBlock createSoundDataBlock() {
         return new SoundDataBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.SoundTrigger; }
    public SoundDataBlock SoundData;

    public SoundTriggerPacket()
    {
        hasVariableBlocks = false;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)29);
        header.setReliable(true);
        SoundData = new SoundDataBlock();
    }

    public SoundTriggerPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        SoundData = new SoundDataBlock(bytes);
     }

    public SoundTriggerPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        SoundData = new SoundDataBlock(bytes);
    }

    public int getLength()
    {
        int length = header.getLength();
        length += SoundData.getLength();
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
        SoundData.ToBytes(bytes);
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- SoundTrigger ---\n";
        output += SoundData.toString() + "\n";
        return output;
    }
}
