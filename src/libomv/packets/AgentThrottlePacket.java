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

public class AgentThrottlePacket extends Packet
{
    public class AgentDataBlock
    {
        public UUID AgentID = null;
        public UUID SessionID = null;
        public int CircuitCode = 0;

        public int getLength(){
            return 36;
        }

        public AgentDataBlock() { }
        public AgentDataBlock(ByteBuffer bytes)
        {
            AgentID = new UUID(bytes);
            SessionID = new UUID(bytes);
            CircuitCode = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            AgentID.GetBytes(bytes);
            SessionID.GetBytes(bytes);
            bytes.putInt(CircuitCode);
        }

        public String toString()
        {
            String output = "-- AgentData --\n";
            try {
                output += "AgentID: " + AgentID.toString() + "\n";
                output += "SessionID: " + SessionID.toString() + "\n";
                output += "CircuitCode: " + Integer.toString(CircuitCode) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public AgentDataBlock createAgentDataBlock() {
         return new AgentDataBlock();
    }

    public class ThrottleBlock
    {
        public int GenCounter = 0;
        private byte[] _throttles;
        public byte[] getThrottles() {
            return _throttles;
        }

        public void setThrottles(byte[] value) throws Exception {
            if (value == null) {
                _throttles = null;
            }
            if (value.length > 255) {
                throw new OverflowException("Value exceeds 255 characters");
            }
            else {
                _throttles = new byte[value.length];
                System.arraycopy(value, 0, _throttles, 0, value.length);
            }
        }


        public int getLength(){
            int length = 4;
            if (getThrottles() != null) { length += 1 + getThrottles().length; }
            return length;
        }

        public ThrottleBlock() { }
        public ThrottleBlock(ByteBuffer bytes)
        {
            int length;
            GenCounter = bytes.getInt(); 
            length = (int)(bytes.get()) & 0xFF;
            _throttles = new byte[length];
            bytes.get(_throttles); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putInt(GenCounter);
            bytes.put((byte)_throttles.length);
            bytes.put(_throttles);
        }

        public String toString()
        {
            String output = "-- Throttle --\n";
            try {
                output += "GenCounter: " + Integer.toString(GenCounter) + "\n";
                output += Helpers.FieldToString(_throttles, "Throttles") + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public ThrottleBlock createThrottleBlock() {
         return new ThrottleBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.AgentThrottle; }
    public AgentDataBlock AgentData;
    public ThrottleBlock Throttle;

    public AgentThrottlePacket()
    {
        hasVariableBlocks = false;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)81);
        header.setReliable(true);
        AgentData = new AgentDataBlock();
        Throttle = new ThrottleBlock();
    }

    public AgentThrottlePacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        AgentData = new AgentDataBlock(bytes);
        Throttle = new ThrottleBlock(bytes);
     }

    public AgentThrottlePacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        AgentData = new AgentDataBlock(bytes);
        Throttle = new ThrottleBlock(bytes);
    }

    public int getLength()
    {
        int length = header.getLength();
        length += AgentData.getLength();
        length += Throttle.getLength();
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
        Throttle.ToBytes(bytes);
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- AgentThrottle ---\n";
        output += AgentData.toString() + "\n";
        output += Throttle.toString() + "\n";
        return output;
    }
}
