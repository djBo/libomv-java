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

public class DirClassifiedReplyPacket extends Packet
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
        public UUID ClassifiedID = null;
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

        public byte ClassifiedFlags = 0;
        public int CreationDate = 0;
        public int ExpirationDate = 0;
        public int PriceForListing = 0;

        public int getLength(){
            int length = 29;
            if (getName() != null) { length += 1 + getName().length; }
            return length;
        }

        public QueryRepliesBlock() { }
        public QueryRepliesBlock(ByteBuffer bytes)
        {
            int length;
            ClassifiedID = new UUID(bytes);
            length = (int)(bytes.get()) & 0xFF;
            _name = new byte[length];
            bytes.get(_name); 
            ClassifiedFlags = bytes.get(); 
            CreationDate = bytes.getInt(); 
            ExpirationDate = bytes.getInt(); 
            PriceForListing = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            ClassifiedID.GetBytes(bytes);
            bytes.put((byte)_name.length);
            bytes.put(_name);
            bytes.put(ClassifiedFlags);
            bytes.putInt(CreationDate);
            bytes.putInt(ExpirationDate);
            bytes.putInt(PriceForListing);
        }

        public String toString()
        {
            String output = "-- QueryReplies --\n";
            try {
                output += "ClassifiedID: " + ClassifiedID.toString() + "\n";
                output += Helpers.FieldToString(_name, "Name") + "\n";
                output += "ClassifiedFlags: " + Byte.toString(ClassifiedFlags) + "\n";
                output += "CreationDate: " + Integer.toString(CreationDate) + "\n";
                output += "ExpirationDate: " + Integer.toString(ExpirationDate) + "\n";
                output += "PriceForListing: " + Integer.toString(PriceForListing) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public QueryRepliesBlock createQueryRepliesBlock() {
         return new QueryRepliesBlock();
    }

    public class StatusDataBlock
    {
        public int Status = 0;

        public int getLength(){
            return 4;
        }

        public StatusDataBlock() { }
        public StatusDataBlock(ByteBuffer bytes)
        {
            Status = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putInt(Status);
        }

        public String toString()
        {
            String output = "-- StatusData --\n";
            try {
                output += "Status: " + Integer.toString(Status) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public StatusDataBlock createStatusDataBlock() {
         return new StatusDataBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.DirClassifiedReply; }
    public AgentDataBlock AgentData;
    public QueryDataBlock QueryData;
    public QueryRepliesBlock[] QueryReplies;
    public StatusDataBlock[] StatusData;

    public DirClassifiedReplyPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)41);
        header.setReliable(true);
        AgentData = new AgentDataBlock();
        QueryData = new QueryDataBlock();
        QueryReplies = new QueryRepliesBlock[0];
        StatusData = new StatusDataBlock[0];
    }

    public DirClassifiedReplyPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        AgentData = new AgentDataBlock(bytes);
        QueryData = new QueryDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        QueryReplies = new QueryRepliesBlock[count];
        for (int j = 0; j < count; j++)
        { QueryReplies[j] = new QueryRepliesBlock(bytes); }
        count = (int)bytes.get() & 0xFF;
        StatusData = new StatusDataBlock[count];
        for (int j = 0; j < count; j++)
        { StatusData[j] = new StatusDataBlock(bytes); }
     }

    public DirClassifiedReplyPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        AgentData = new AgentDataBlock(bytes);
        QueryData = new QueryDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        QueryReplies = new QueryRepliesBlock[count];
        for (int j = 0; j < count; j++)
        { QueryReplies[j] = new QueryRepliesBlock(bytes); }
        count = (int)bytes.get() & 0xFF;
        StatusData = new StatusDataBlock[count];
        for (int j = 0; j < count; j++)
        { StatusData[j] = new StatusDataBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length += AgentData.getLength();
        length += QueryData.getLength();
        length++;
        for (int j = 0; j < QueryReplies.length; j++) { length += QueryReplies[j].getLength(); }
        length++;
        for (int j = 0; j < StatusData.length; j++) { length += StatusData[j].getLength(); }
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
        bytes.put((byte)StatusData.length);
        for (int j = 0; j < StatusData.length; j++) { StatusData[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- DirClassifiedReply ---\n";
        output += AgentData.toString() + "\n";
        output += QueryData.toString() + "\n";
        for (int j = 0; j < QueryReplies.length; j++)
        {
            output += QueryReplies[j].toString() + "\n";
        }
        for (int j = 0; j < StatusData.length; j++)
        {
            output += StatusData[j].toString() + "\n";
        }
        return output;
    }
}
