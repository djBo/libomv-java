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

public class DataServerLogoutPacket extends Packet
{
    public class UserDataBlock
    {
        public UUID AgentID = null;
        public int ViewerIP = 0;
        public boolean Disconnect = false;
        public UUID SessionID = null;

        public int getLength(){
            return 37;
        }

        public UserDataBlock() { }
        public UserDataBlock(ByteBuffer bytes)
        {
            AgentID = new UUID(bytes);
            ViewerIP = bytes.getInt(); 
            Disconnect = (bytes.get() != 0) ? (boolean)true : (boolean)false;
            SessionID = new UUID(bytes);
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            AgentID.GetBytes(bytes);
            bytes.putInt(ViewerIP);
            bytes.put((byte)((Disconnect) ? 1 : 0));
            SessionID.GetBytes(bytes);
        }

        public String toString()
        {
            String output = "-- UserData --\n";
            try {
                output += "AgentID: " + AgentID.toString() + "\n";
                output += "ViewerIP: " + Integer.toString(ViewerIP) + "\n";
                output += "Disconnect: " + Boolean.toString(Disconnect) + "\n";
                output += "SessionID: " + SessionID.toString() + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public UserDataBlock createUserDataBlock() {
         return new UserDataBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.DataServerLogout; }
    public UserDataBlock UserData;

    public DataServerLogoutPacket()
    {
        hasVariableBlocks = false;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)251);
        header.setReliable(true);
        UserData = new UserDataBlock();
    }

    public DataServerLogoutPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        UserData = new UserDataBlock(bytes);
     }

    public DataServerLogoutPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        UserData = new UserDataBlock(bytes);
    }

    public int getLength()
    {
        int length = header.getLength();
        length += UserData.getLength();
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
        UserData.ToBytes(bytes);
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- DataServerLogout ---\n";
        output += UserData.toString() + "\n";
        return output;
    }
}
