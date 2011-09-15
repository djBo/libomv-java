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

public class SetGroupAcceptNoticesPacket extends Packet
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

    public class DataBlock
    {
        public UUID GroupID = null;
        public boolean AcceptNotices = false;

        public int getLength(){
            return 17;
        }

        public DataBlock() { }
        public DataBlock(ByteBuffer bytes)
        {
            GroupID = new UUID(bytes);
            AcceptNotices = (bytes.get() != 0) ? (boolean)true : (boolean)false;
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            GroupID.GetBytes(bytes);
            bytes.put((byte)((AcceptNotices) ? 1 : 0));
        }

        public String toString()
        {
            String output = "-- Data --\n";
            try {
                output += "GroupID: " + GroupID.toString() + "\n";
                output += "AcceptNotices: " + Boolean.toString(AcceptNotices) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public DataBlock createDataBlock() {
         return new DataBlock();
    }

    public class NewDataBlock
    {
        public boolean ListInProfile = false;

        public int getLength(){
            return 1;
        }

        public NewDataBlock() { }
        public NewDataBlock(ByteBuffer bytes)
        {
            ListInProfile = (bytes.get() != 0) ? (boolean)true : (boolean)false;
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.put((byte)((ListInProfile) ? 1 : 0));
        }

        public String toString()
        {
            String output = "-- NewData --\n";
            try {
                output += "ListInProfile: " + Boolean.toString(ListInProfile) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public NewDataBlock createNewDataBlock() {
         return new NewDataBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.SetGroupAcceptNotices; }
    public AgentDataBlock AgentData;
    public DataBlock Data;
    public NewDataBlock NewData;

    public SetGroupAcceptNoticesPacket()
    {
        hasVariableBlocks = false;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)370);
        header.setReliable(true);
        AgentData = new AgentDataBlock();
        Data = new DataBlock();
        NewData = new NewDataBlock();
    }

    public SetGroupAcceptNoticesPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        AgentData = new AgentDataBlock(bytes);
        Data = new DataBlock(bytes);
        NewData = new NewDataBlock(bytes);
     }

    public SetGroupAcceptNoticesPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        AgentData = new AgentDataBlock(bytes);
        Data = new DataBlock(bytes);
        NewData = new NewDataBlock(bytes);
    }

    public int getLength()
    {
        int length = header.getLength();
        length += AgentData.getLength();
        length += Data.getLength();
        length += NewData.getLength();
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
        Data.ToBytes(bytes);
        NewData.ToBytes(bytes);
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- SetGroupAcceptNotices ---\n";
        output += AgentData.toString() + "\n";
        output += Data.toString() + "\n";
        output += NewData.toString() + "\n";
        return output;
    }
}
