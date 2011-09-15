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

public class GroupAccountDetailsReplyPacket extends Packet
{
    public class AgentDataBlock
    {
        public UUID AgentID = null;
        public UUID GroupID = null;

        public int getLength(){
            return 32;
        }

        public AgentDataBlock() { }
        public AgentDataBlock(ByteBuffer bytes)
        {
            AgentID = new UUID(bytes);
            GroupID = new UUID(bytes);
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            AgentID.GetBytes(bytes);
            GroupID.GetBytes(bytes);
        }

        public String toString()
        {
            String output = "-- AgentData --\n";
            try {
                output += "AgentID: " + AgentID.toString() + "\n";
                output += "GroupID: " + GroupID.toString() + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public AgentDataBlock createAgentDataBlock() {
         return new AgentDataBlock();
    }

    public class MoneyDataBlock
    {
        public UUID RequestID = null;
        public int IntervalDays = 0;
        public int CurrentInterval = 0;
        private byte[] _startdate;
        public byte[] getStartDate() {
            return _startdate;
        }

        public void setStartDate(byte[] value) throws Exception {
            if (value == null) {
                _startdate = null;
            }
            if (value.length > 255) {
                throw new OverflowException("Value exceeds 255 characters");
            }
            else {
                _startdate = new byte[value.length];
                System.arraycopy(value, 0, _startdate, 0, value.length);
            }
        }


        public int getLength(){
            int length = 24;
            if (getStartDate() != null) { length += 1 + getStartDate().length; }
            return length;
        }

        public MoneyDataBlock() { }
        public MoneyDataBlock(ByteBuffer bytes)
        {
            int length;
            RequestID = new UUID(bytes);
            IntervalDays = bytes.getInt(); 
            CurrentInterval = bytes.getInt(); 
            length = (int)(bytes.get()) & 0xFF;
            _startdate = new byte[length];
            bytes.get(_startdate); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            RequestID.GetBytes(bytes);
            bytes.putInt(IntervalDays);
            bytes.putInt(CurrentInterval);
            bytes.put((byte)_startdate.length);
            bytes.put(_startdate);
        }

        public String toString()
        {
            String output = "-- MoneyData --\n";
            try {
                output += "RequestID: " + RequestID.toString() + "\n";
                output += "IntervalDays: " + Integer.toString(IntervalDays) + "\n";
                output += "CurrentInterval: " + Integer.toString(CurrentInterval) + "\n";
                output += Helpers.FieldToString(_startdate, "StartDate") + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public MoneyDataBlock createMoneyDataBlock() {
         return new MoneyDataBlock();
    }

    public class HistoryDataBlock
    {
        private byte[] _description;
        public byte[] getDescription() {
            return _description;
        }

        public void setDescription(byte[] value) throws Exception {
            if (value == null) {
                _description = null;
            }
            if (value.length > 255) {
                throw new OverflowException("Value exceeds 255 characters");
            }
            else {
                _description = new byte[value.length];
                System.arraycopy(value, 0, _description, 0, value.length);
            }
        }

        public int Amount = 0;

        public int getLength(){
            int length = 4;
            if (getDescription() != null) { length += 1 + getDescription().length; }
            return length;
        }

        public HistoryDataBlock() { }
        public HistoryDataBlock(ByteBuffer bytes)
        {
            int length;
            length = (int)(bytes.get()) & 0xFF;
            _description = new byte[length];
            bytes.get(_description); 
            Amount = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.put((byte)_description.length);
            bytes.put(_description);
            bytes.putInt(Amount);
        }

        public String toString()
        {
            String output = "-- HistoryData --\n";
            try {
                output += Helpers.FieldToString(_description, "Description") + "\n";
                output += "Amount: " + Integer.toString(Amount) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public HistoryDataBlock createHistoryDataBlock() {
         return new HistoryDataBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.GroupAccountDetailsReply; }
    public AgentDataBlock AgentData;
    public MoneyDataBlock MoneyData;
    public HistoryDataBlock[] HistoryData;

    public GroupAccountDetailsReplyPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)356);
        header.setReliable(true);
        AgentData = new AgentDataBlock();
        MoneyData = new MoneyDataBlock();
        HistoryData = new HistoryDataBlock[0];
    }

    public GroupAccountDetailsReplyPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        AgentData = new AgentDataBlock(bytes);
        MoneyData = new MoneyDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        HistoryData = new HistoryDataBlock[count];
        for (int j = 0; j < count; j++)
        { HistoryData[j] = new HistoryDataBlock(bytes); }
     }

    public GroupAccountDetailsReplyPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        AgentData = new AgentDataBlock(bytes);
        MoneyData = new MoneyDataBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        HistoryData = new HistoryDataBlock[count];
        for (int j = 0; j < count; j++)
        { HistoryData[j] = new HistoryDataBlock(bytes); }
    }

    public int getLength()
    {
        int length = header.getLength();
        length += AgentData.getLength();
        length += MoneyData.getLength();
        length++;
        for (int j = 0; j < HistoryData.length; j++) { length += HistoryData[j].getLength(); }
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
        MoneyData.ToBytes(bytes);
        bytes.put((byte)HistoryData.length);
        for (int j = 0; j < HistoryData.length; j++) { HistoryData[j].ToBytes(bytes); }
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- GroupAccountDetailsReply ---\n";
        output += AgentData.toString() + "\n";
        output += MoneyData.toString() + "\n";
        for (int j = 0; j < HistoryData.length; j++)
        {
            output += HistoryData[j].toString() + "\n";
        }
        return output;
    }
}
