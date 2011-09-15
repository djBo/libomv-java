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

public class CreateGroupRequestPacket extends Packet
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

    public class GroupDataBlock
    {
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

        private byte[] _charter;
        public byte[] getCharter() {
            return _charter;
        }

        public void setCharter(byte[] value) throws Exception {
            if (value == null) {
                _charter = null;
            }
            if (value.length > 1024) {
                throw new OverflowException("Value exceeds 1024 characters");
            }
            else {
                _charter = new byte[value.length];
                System.arraycopy(value, 0, _charter, 0, value.length);
            }
        }

        public boolean ShowInList = false;
        public UUID InsigniaID = null;
        public int MembershipFee = 0;
        public boolean OpenEnrollment = false;
        public boolean AllowPublish = false;
        public boolean MaturePublish = false;

        public int getLength(){
            int length = 24;
            if (getName() != null) { length += 1 + getName().length; }
            if (getCharter() != null) { length += 2 + getCharter().length; }
            return length;
        }

        public GroupDataBlock() { }
        public GroupDataBlock(ByteBuffer bytes)
        {
            int length;
            length = (int)(bytes.get()) & 0xFF;
            _name = new byte[length];
            bytes.get(_name); 
            length = (int)(bytes.getShort()) & 0xFFFF;
            _charter = new byte[length];
            bytes.get(_charter); 
            ShowInList = (bytes.get() != 0) ? (boolean)true : (boolean)false;
            InsigniaID = new UUID(bytes);
            MembershipFee = bytes.getInt(); 
            OpenEnrollment = (bytes.get() != 0) ? (boolean)true : (boolean)false;
            AllowPublish = (bytes.get() != 0) ? (boolean)true : (boolean)false;
            MaturePublish = (bytes.get() != 0) ? (boolean)true : (boolean)false;
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.put((byte)_name.length);
            bytes.put(_name);
            bytes.putShort((short)_charter.length);
            bytes.put(_charter);
            bytes.put((byte)((ShowInList) ? 1 : 0));
            InsigniaID.GetBytes(bytes);
            bytes.putInt(MembershipFee);
            bytes.put((byte)((OpenEnrollment) ? 1 : 0));
            bytes.put((byte)((AllowPublish) ? 1 : 0));
            bytes.put((byte)((MaturePublish) ? 1 : 0));
        }

        public String toString()
        {
            String output = "-- GroupData --\n";
            try {
                output += Helpers.FieldToString(_name, "Name") + "\n";
                output += Helpers.FieldToString(_charter, "Charter") + "\n";
                output += "ShowInList: " + Boolean.toString(ShowInList) + "\n";
                output += "InsigniaID: " + InsigniaID.toString() + "\n";
                output += "MembershipFee: " + Integer.toString(MembershipFee) + "\n";
                output += "OpenEnrollment: " + Boolean.toString(OpenEnrollment) + "\n";
                output += "AllowPublish: " + Boolean.toString(AllowPublish) + "\n";
                output += "MaturePublish: " + Boolean.toString(MaturePublish) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public GroupDataBlock createGroupDataBlock() {
         return new GroupDataBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.CreateGroupRequest; }
    public AgentDataBlock AgentData;
    public GroupDataBlock GroupData;

    public CreateGroupRequestPacket()
    {
        hasVariableBlocks = false;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)339);
        header.setReliable(true);
        AgentData = new AgentDataBlock();
        GroupData = new GroupDataBlock();
    }

    public CreateGroupRequestPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        AgentData = new AgentDataBlock(bytes);
        GroupData = new GroupDataBlock(bytes);
     }

    public CreateGroupRequestPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        AgentData = new AgentDataBlock(bytes);
        GroupData = new GroupDataBlock(bytes);
    }

    public int getLength()
    {
        int length = header.getLength();
        length += AgentData.getLength();
        length += GroupData.getLength();
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
        GroupData.ToBytes(bytes);
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- CreateGroupRequest ---\n";
        output += AgentData.toString() + "\n";
        output += GroupData.toString() + "\n";
        return output;
    }
}
