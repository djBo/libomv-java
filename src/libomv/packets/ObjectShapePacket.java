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

public class ObjectShapePacket extends Packet
{
    public class AgentDataBlock
    {
        public UUID AgentID = null;
        public UUID SessionID = null;

        public int getLength(){
            return 32;
        }

        public AgentDataBlock() { }
        public AgentDataBlock(ByteBuffer bytes)
        {
            AgentID = new UUID(bytes);
            SessionID = new UUID(bytes);
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            AgentID.GetBytes(bytes);
            SessionID.GetBytes(bytes);
        }

        public String toString()
        {
            String output = "-- AgentData --\n";
            try {
                output += "AgentID: " + AgentID.toString() + "\n";
                output += "SessionID: " + SessionID.toString() + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public AgentDataBlock createAgentDataBlock() {
         return new AgentDataBlock();
    }

    public class ObjectDataBlock
    {
        public int ObjectLocalID = 0;
        public byte PathCurve = 0;
        public byte ProfileCurve = 0;
        public short PathBegin = 0;
        public short PathEnd = 0;
        public byte PathScaleX = 0;
        public byte PathScaleY = 0;
        public byte PathShearX = 0;
        public byte PathShearY = 0;
        public byte PathTwist = 0;
        public byte PathTwistBegin = 0;
        public byte PathRadiusOffset = 0;
        public byte PathTaperX = 0;
        public byte PathTaperY = 0;
        public byte PathRevolutions = 0;
        public byte PathSkew = 0;
        public short ProfileBegin = 0;
        public short ProfileEnd = 0;
        public short ProfileHollow = 0;

        public int getLength(){
            return 27;
        }

        public ObjectDataBlock() { }
        public ObjectDataBlock(ByteBuffer bytes)
        {
            ObjectLocalID = bytes.getInt(); 
            PathCurve = bytes.get(); 
            ProfileCurve = bytes.get(); 
            PathBegin = bytes.getShort(); 
            PathEnd = bytes.getShort(); 
            PathScaleX = bytes.get(); 
            PathScaleY = bytes.get(); 
            PathShearX = bytes.get(); 
            PathShearY = bytes.get(); 
            PathTwist = bytes.get(); 
            PathTwistBegin = bytes.get(); 
            PathRadiusOffset = bytes.get(); 
            PathTaperX = bytes.get(); 
            PathTaperY = bytes.get(); 
            PathRevolutions = bytes.get(); 
            PathSkew = bytes.get(); 
            ProfileBegin = bytes.getShort(); 
            ProfileEnd = bytes.getShort(); 
            ProfileHollow = bytes.getShort(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putInt(ObjectLocalID);
            bytes.put(PathCurve);
            bytes.put(ProfileCurve);
            bytes.putShort(PathBegin);
            bytes.putShort(PathEnd);
            bytes.put(PathScaleX);
            bytes.put(PathScaleY);
            bytes.put(PathShearX);
            bytes.put(PathShearY);
            bytes.put(PathTwist);
            bytes.put(PathTwistBegin);
            bytes.put(PathRadiusOffset);
            bytes.put(PathTaperX);
            bytes.put(PathTaperY);
            bytes.put(PathRevolutions);
            bytes.put(PathSkew);
            bytes.putShort(ProfileBegin);
            bytes.putShort(ProfileEnd);
            bytes.putShort(ProfileHollow);
        }

        public String toString()
        {
            String output = "-- ObjectData --\n";
            try {
                output += "ObjectLocalID: " + Integer.toString(ObjectLocalID) + "\n";
                output += "PathCurve: " + Byte.toString(PathCurve) + "\n";
                output += "ProfileCurve: " + Byte.toString(ProfileCurve) + "\n";
                output += "PathBegin: " + Short.toString(PathBegin) + "\n";
                output += "PathEnd: " + Short.toString(PathEnd) + "\n";
                output += "PathScaleX: " + Byte.toString(PathScaleX) + "\n";
                output += "PathScaleY: " + Byte.toString(PathScaleY) + "\n";
                output += "PathShearX: " + Byte.toString(PathShearX) + "\n";
                output += "PathShearY: " + Byte.toString(PathShearY) + "\n";
                output += "PathTwist: " + Byte.toString(PathTwist) + "\n";
                output += "PathTwistBegin: " + Byte.toString(PathTwistBegin) + "\n";
                output += "PathRadiusOffset: " + Byte.toString(PathRadiusOffset) + "\n";
                output += "PathTaperX: " + Byte.toString(PathTaperX) + "\n";
                output += "PathTaperY: " + Byte.toString(PathTaperY) + "\n";
                output += "PathRevolutions: " + Byte.toString(PathRevolutions) + "\n";
                output += "PathSkew: " + Byte.toString(PathSkew) + "\n";
                output += "ProfileBegin: " + Short.toString(ProfileBegin) + "\n";
                output += "ProfileEnd: " + Short.toString(ProfileEnd) + "\n";
                output += "ProfileHollow: " + Short.toString(ProfileHollow) + "\n";
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
    public PacketType getType() { return PacketType.ObjectShape; }
    public AgentDataBlock AgentData;
    public ObjectDataBlock[] ObjectData;

    public ObjectShapePacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)98);
        header.setReliable(true);
        AgentData = new AgentDataBlock();
        ObjectData = new ObjectDataBlock[0];
    }

    public ObjectShapePacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        AgentData = new AgentDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        ObjectData = new ObjectDataBlock[count];
        for (int j = 0; j < count; j++)
        { ObjectData[j] = new ObjectDataBlock(bytes); }
     }

    public ObjectShapePacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        AgentData = new AgentDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        ObjectData = new ObjectDataBlock[count];
        for (int j = 0; j < count; j++)
        { ObjectData[j] = new ObjectDataBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length += AgentData.getLength();
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
        AgentData.ToBytes(bytes);
        bytes.put((byte)ObjectData.length);
        for (int j = 0; j < ObjectData.length; j++) { ObjectData[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- ObjectShape ---\n";
        output += AgentData.toString() + "\n";
        for (int j = 0; j < ObjectData.length; j++)
        {
            output += ObjectData[j].toString() + "\n";
        }
        return output;
    }
}
