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

import libomv.types.PacketHeader;
import libomv.types.PacketFrequency;
import libomv.types.UUID;
import libomv.types.Vector3;

public class ObjectDuplicateOnRayPacket extends Packet
{
    public class AgentDataBlock
    {
        public UUID AgentID = null;
        public UUID SessionID = null;
        public UUID GroupID = null;
        public Vector3 RayStart = null;
        public Vector3 RayEnd = null;
        public boolean BypassRaycast = false;
        public boolean RayEndIsIntersection = false;
        public boolean CopyCenters = false;
        public boolean CopyRotates = false;
        public UUID RayTargetID = null;
        public int DuplicateFlags = 0;

        public int getLength(){
            return 96;
        }

        public AgentDataBlock() { }
        public AgentDataBlock(ByteBuffer bytes)
        {
            AgentID = new UUID(bytes);
            SessionID = new UUID(bytes);
            GroupID = new UUID(bytes);
            RayStart = new Vector3(bytes); 
            RayEnd = new Vector3(bytes); 
            BypassRaycast = (bytes.get() != 0) ? (boolean)true : (boolean)false;
            RayEndIsIntersection = (bytes.get() != 0) ? (boolean)true : (boolean)false;
            CopyCenters = (bytes.get() != 0) ? (boolean)true : (boolean)false;
            CopyRotates = (bytes.get() != 0) ? (boolean)true : (boolean)false;
            RayTargetID = new UUID(bytes);
            DuplicateFlags = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            AgentID.GetBytes(bytes);
            SessionID.GetBytes(bytes);
            GroupID.GetBytes(bytes);
            RayStart.GetBytes(bytes);
            RayEnd.GetBytes(bytes);
            bytes.put((byte)((BypassRaycast) ? 1 : 0));
            bytes.put((byte)((RayEndIsIntersection) ? 1 : 0));
            bytes.put((byte)((CopyCenters) ? 1 : 0));
            bytes.put((byte)((CopyRotates) ? 1 : 0));
            RayTargetID.GetBytes(bytes);
            bytes.putInt(DuplicateFlags);
        }

        public String toString()
        {
            String output = "-- AgentData --\n";
            try {
                output += "AgentID: " + AgentID.toString() + "\n";
                output += "SessionID: " + SessionID.toString() + "\n";
                output += "GroupID: " + GroupID.toString() + "\n";
                output += "RayStart: " + RayStart.toString() + "\n";
                output += "RayEnd: " + RayEnd.toString() + "\n";
                output += "BypassRaycast: " + Boolean.toString(BypassRaycast) + "\n";
                output += "RayEndIsIntersection: " + Boolean.toString(RayEndIsIntersection) + "\n";
                output += "CopyCenters: " + Boolean.toString(CopyCenters) + "\n";
                output += "CopyRotates: " + Boolean.toString(CopyRotates) + "\n";
                output += "RayTargetID: " + RayTargetID.toString() + "\n";
                output += "DuplicateFlags: " + Integer.toString(DuplicateFlags) + "\n";
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

        public int getLength(){
            return 4;
        }

        public ObjectDataBlock() { }
        public ObjectDataBlock(ByteBuffer bytes)
        {
            ObjectLocalID = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putInt(ObjectLocalID);
        }

        public String toString()
        {
            String output = "-- ObjectData --\n";
            try {
                output += "ObjectLocalID: " + Integer.toString(ObjectLocalID) + "\n";
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
    public PacketType getType() { return PacketType.ObjectDuplicateOnRay; }
    public AgentDataBlock AgentData;
    public ObjectDataBlock[] ObjectData;

    public ObjectDuplicateOnRayPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)91);
        header.setReliable(true);
        AgentData = new AgentDataBlock();
        ObjectData = new ObjectDataBlock[0];
    }

    public ObjectDuplicateOnRayPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        AgentData = new AgentDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        ObjectData = new ObjectDataBlock[count];
        for (int j = 0; j < count; j++)
        { ObjectData[j] = new ObjectDataBlock(bytes); }
     }

    public ObjectDuplicateOnRayPacket(PacketHeader head, ByteBuffer bytes)
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
        String output = "--- ObjectDuplicateOnRay ---\n";
        output += AgentData.toString() + "\n";
        for (int j = 0; j < ObjectData.length; j++)
        {
            output += ObjectData[j].toString() + "\n";
        }
        return output;
    }
}
