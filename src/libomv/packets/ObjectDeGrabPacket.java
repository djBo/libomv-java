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

public class ObjectDeGrabPacket extends Packet
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
        public int LocalID = 0;

        public int getLength(){
            return 4;
        }

        public ObjectDataBlock() { }
        public ObjectDataBlock(ByteBuffer bytes)
        {
            LocalID = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putInt(LocalID);
        }

        public String toString()
        {
            String output = "-- ObjectData --\n";
            try {
                output += "LocalID: " + Integer.toString(LocalID) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public ObjectDataBlock createObjectDataBlock() {
         return new ObjectDataBlock();
    }

    public class SurfaceInfoBlock
    {
        public Vector3 UVCoord = null;
        public Vector3 STCoord = null;
        public int FaceIndex = 0;
        public Vector3 Position = null;
        public Vector3 Normal = null;
        public Vector3 Binormal = null;

        public int getLength(){
            return 64;
        }

        public SurfaceInfoBlock() { }
        public SurfaceInfoBlock(ByteBuffer bytes)
        {
            UVCoord = new Vector3(bytes); 
            STCoord = new Vector3(bytes); 
            FaceIndex = bytes.getInt(); 
            Position = new Vector3(bytes); 
            Normal = new Vector3(bytes); 
            Binormal = new Vector3(bytes); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            UVCoord.GetBytes(bytes);
            STCoord.GetBytes(bytes);
            bytes.putInt(FaceIndex);
            Position.GetBytes(bytes);
            Normal.GetBytes(bytes);
            Binormal.GetBytes(bytes);
        }

        public String toString()
        {
            String output = "-- SurfaceInfo --\n";
            try {
                output += "UVCoord: " + UVCoord.toString() + "\n";
                output += "STCoord: " + STCoord.toString() + "\n";
                output += "FaceIndex: " + Integer.toString(FaceIndex) + "\n";
                output += "Position: " + Position.toString() + "\n";
                output += "Normal: " + Normal.toString() + "\n";
                output += "Binormal: " + Binormal.toString() + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public SurfaceInfoBlock createSurfaceInfoBlock() {
         return new SurfaceInfoBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.ObjectDeGrab; }
    public AgentDataBlock AgentData;
    public ObjectDataBlock ObjectData;
    public SurfaceInfoBlock[] SurfaceInfo;

    public ObjectDeGrabPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)119);
        header.setReliable(true);
        AgentData = new AgentDataBlock();
        ObjectData = new ObjectDataBlock();
        SurfaceInfo = new SurfaceInfoBlock[0];
    }

    public ObjectDeGrabPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        AgentData = new AgentDataBlock(bytes);
        ObjectData = new ObjectDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        SurfaceInfo = new SurfaceInfoBlock[count];
        for (int j = 0; j < count; j++)
        { SurfaceInfo[j] = new SurfaceInfoBlock(bytes); }
     }

    public ObjectDeGrabPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        AgentData = new AgentDataBlock(bytes);
        ObjectData = new ObjectDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        SurfaceInfo = new SurfaceInfoBlock[count];
        for (int j = 0; j < count; j++)
        { SurfaceInfo[j] = new SurfaceInfoBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length += AgentData.getLength();
        length += ObjectData.getLength();
        length++;
        for (int j = 0; j < SurfaceInfo.length; j++) { length += SurfaceInfo[j].getLength(); }
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
        ObjectData.ToBytes(bytes);
        bytes.put((byte)SurfaceInfo.length);
        for (int j = 0; j < SurfaceInfo.length; j++) { SurfaceInfo[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- ObjectDeGrab ---\n";
        output += AgentData.toString() + "\n";
        output += ObjectData.toString() + "\n";
        for (int j = 0; j < SurfaceInfo.length; j++)
        {
            output += SurfaceInfo[j].toString() + "\n";
        }
        return output;
    }
}
