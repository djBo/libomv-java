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

public class DataHomeLocationReplyPacket extends Packet
{
    public class InfoBlock
    {
        public UUID AgentID = null;
        public long RegionHandle = 0;
        public Vector3 Position = null;
        public Vector3 LookAt = null;

        public int getLength(){
            return 48;
        }

        public InfoBlock() { }
        public InfoBlock(ByteBuffer bytes)
        {
            AgentID = new UUID(bytes);
            RegionHandle = bytes.getLong(); 
            Position = new Vector3(bytes); 
            LookAt = new Vector3(bytes); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            AgentID.GetBytes(bytes);
            bytes.putLong(RegionHandle);
            Position.GetBytes(bytes);
            LookAt.GetBytes(bytes);
        }

        public String toString()
        {
            String output = "-- Info --\n";
            try {
                output += "AgentID: " + AgentID.toString() + "\n";
                output += "RegionHandle: " + Long.toString(RegionHandle) + "\n";
                output += "Position: " + Position.toString() + "\n";
                output += "LookAt: " + LookAt.toString() + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public InfoBlock createInfoBlock() {
         return new InfoBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.DataHomeLocationReply; }
    public InfoBlock Info;

    public DataHomeLocationReplyPacket()
    {
        hasVariableBlocks = false;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)68);
        header.setReliable(true);
        Info = new InfoBlock();
    }

    public DataHomeLocationReplyPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        Info = new InfoBlock(bytes);
     }

    public DataHomeLocationReplyPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        Info = new InfoBlock(bytes);
    }

    public int getLength()
    {
        int length = header.getLength();
        length += Info.getLength();
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
        Info.ToBytes(bytes);
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- DataHomeLocationReply ---\n";
        output += Info.toString() + "\n";
        return output;
    }
}
