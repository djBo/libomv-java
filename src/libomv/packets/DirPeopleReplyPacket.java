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
import libomv.types.OverflowException;

public class DirPeopleReplyPacket extends Packet
{
    public class AgentDataBlock
    {
        public UUID AgentID = null;

        public int getLength(){
            return 16;
        }

        public AgentDataBlock() { }
        public AgentDataBlock(ByteBuffer bytes)
        {
            AgentID = new UUID(bytes);
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            AgentID.GetBytes(bytes);
        }

        public String toString()
        {
            String output = "-- AgentData --\n";
            try {
                output += "AgentID: " + AgentID.toString() + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public AgentDataBlock createAgentDataBlock() {
         return new AgentDataBlock();
    }

    public class QueryDataBlock
    {
        public UUID QueryID = null;

        public int getLength(){
            return 16;
        }

        public QueryDataBlock() { }
        public QueryDataBlock(ByteBuffer bytes)
        {
            QueryID = new UUID(bytes);
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            QueryID.GetBytes(bytes);
        }

        public String toString()
        {
            String output = "-- QueryData --\n";
            try {
                output += "QueryID: " + QueryID.toString() + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public QueryDataBlock createQueryDataBlock() {
         return new QueryDataBlock();
    }

    public class QueryRepliesBlock
    {
        public UUID AgentID = null;
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

        private byte[] _group;
        public byte[] getGroup() {
            return _group;
        }

        public void setGroup(byte[] value) throws Exception {
            if (value == null) {
                _group = null;
            }
            if (value.length > 255) {
                throw new OverflowException("Value exceeds 255 characters");
            }
            else {
                _group = new byte[value.length];
                System.arraycopy(value, 0, _group, 0, value.length);
            }
        }

        public boolean Online = false;
        public int Reputation = 0;

        public int getLength(){
            int length = 21;
            if (getFirstName() != null) { length += 1 + getFirstName().length; }
            if (getLastName() != null) { length += 1 + getLastName().length; }
            if (getGroup() != null) { length += 1 + getGroup().length; }
            return length;
        }

        public QueryRepliesBlock() { }
        public QueryRepliesBlock(ByteBuffer bytes)
        {
            int length;
            AgentID = new UUID(bytes);
            length = (int)(bytes.get()) & 0xFF;
            _firstname = new byte[length];
            bytes.get(_firstname); 
            length = (int)(bytes.get()) & 0xFF;
            _lastname = new byte[length];
            bytes.get(_lastname); 
            length = (int)(bytes.get()) & 0xFF;
            _group = new byte[length];
            bytes.get(_group); 
            Online = (bytes.get() != 0) ? (boolean)true : (boolean)false;
            Reputation = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            AgentID.GetBytes(bytes);
            bytes.put((byte)_firstname.length);
            bytes.put(_firstname);
            bytes.put((byte)_lastname.length);
            bytes.put(_lastname);
            bytes.put((byte)_group.length);
            bytes.put(_group);
            bytes.put((byte)((Online) ? 1 : 0));
            bytes.putInt(Reputation);
        }

        public String toString()
        {
            String output = "-- QueryReplies --\n";
            try {
                output += "AgentID: " + AgentID.toString() + "\n";
                output += Helpers.FieldToString(_firstname, "FirstName") + "\n";
                output += Helpers.FieldToString(_lastname, "LastName") + "\n";
                output += Helpers.FieldToString(_group, "Group") + "\n";
                output += "Online: " + Boolean.toString(Online) + "\n";
                output += "Reputation: " + Integer.toString(Reputation) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public QueryRepliesBlock createQueryRepliesBlock() {
         return new QueryRepliesBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.DirPeopleReply; }
    public AgentDataBlock AgentData;
    public QueryDataBlock QueryData;
    public QueryRepliesBlock[] QueryReplies;

    public DirPeopleReplyPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)36);
        header.setReliable(true);
        AgentData = new AgentDataBlock();
        QueryData = new QueryDataBlock();
        QueryReplies = new QueryRepliesBlock[0];
    }

    public DirPeopleReplyPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        AgentData = new AgentDataBlock(bytes);
        QueryData = new QueryDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        QueryReplies = new QueryRepliesBlock[count];
        for (int j = 0; j < count; j++)
        { QueryReplies[j] = new QueryRepliesBlock(bytes); }
     }

    public DirPeopleReplyPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        AgentData = new AgentDataBlock(bytes);
        QueryData = new QueryDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        QueryReplies = new QueryRepliesBlock[count];
        for (int j = 0; j < count; j++)
        { QueryReplies[j] = new QueryRepliesBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length += AgentData.getLength();
        length += QueryData.getLength();
        length++;
        for (int j = 0; j < QueryReplies.length; j++) { length += QueryReplies[j].getLength(); }
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
        QueryData.ToBytes(bytes);
        bytes.put((byte)QueryReplies.length);
        for (int j = 0; j < QueryReplies.length; j++) { QueryReplies[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- DirPeopleReply ---\n";
        output += AgentData.toString() + "\n";
        output += QueryData.toString() + "\n";
        for (int j = 0; j < QueryReplies.length; j++)
        {
            output += QueryReplies[j].toString() + "\n";
        }
        return output;
    }
}
