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

public class SimStatsPacket extends Packet
{
    public class RegionBlock
    {
        public int RegionX = 0;
        public int RegionY = 0;
        public int RegionFlags = 0;
        public int ObjectCapacity = 0;

        public int getLength(){
            return 16;
        }

        public RegionBlock() { }
        public RegionBlock(ByteBuffer bytes)
        {
            RegionX = bytes.getInt(); 
            RegionY = bytes.getInt(); 
            RegionFlags = bytes.getInt(); 
            ObjectCapacity = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putInt(RegionX);
            bytes.putInt(RegionY);
            bytes.putInt(RegionFlags);
            bytes.putInt(ObjectCapacity);
        }

        public String toString()
        {
            String output = "-- Region --\n";
            try {
                output += "RegionX: " + Integer.toString(RegionX) + "\n";
                output += "RegionY: " + Integer.toString(RegionY) + "\n";
                output += "RegionFlags: " + Integer.toString(RegionFlags) + "\n";
                output += "ObjectCapacity: " + Integer.toString(ObjectCapacity) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public RegionBlock createRegionBlock() {
         return new RegionBlock();
    }

    public class StatBlock
    {
        public int StatID = 0;
        public float StatValue = 0;

        public int getLength(){
            return 8;
        }

        public StatBlock() { }
        public StatBlock(ByteBuffer bytes)
        {
            StatID = bytes.getInt(); 
            StatValue = bytes.getFloat();
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putInt(StatID);
            bytes.putFloat(StatValue);
        }

        public String toString()
        {
            String output = "-- Stat --\n";
            try {
                output += "StatID: " + Integer.toString(StatID) + "\n";
                output += "StatValue: " + Float.toString(StatValue) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public StatBlock createStatBlock() {
         return new StatBlock();
    }

    public class PidStatBlock
    {
        public int PID = 0;

        public int getLength(){
            return 4;
        }

        public PidStatBlock() { }
        public PidStatBlock(ByteBuffer bytes)
        {
            PID = bytes.getInt(); 
        }

        public void ToBytes(ByteBuffer bytes) throws Exception
        {
            bytes.putInt(PID);
        }

        public String toString()
        {
            String output = "-- PidStat --\n";
            try {
                output += "PID: " + Integer.toString(PID) + "\n";
                output = output.trim();
            }
            catch(Exception e){}
            return output;
        }
    }

    public PidStatBlock createPidStatBlock() {
         return new PidStatBlock();
    }

    private PacketHeader header;
    public PacketHeader getHeader() { return header; }
    public void setHeader(PacketHeader value) { header = value; }
    public PacketType getType() { return PacketType.SimStats; }
    public RegionBlock Region;
    public StatBlock[] Stat;
    public PidStatBlock PidStat;

    public SimStatsPacket()
    {
        hasVariableBlocks = true;
        header = new PacketHeader(PacketFrequency.Low);
        header.setID((short)140);
        header.setReliable(true);
        Region = new RegionBlock();
        Stat = new StatBlock[0];
        PidStat = new PidStatBlock();
    }

    public SimStatsPacket(ByteBuffer bytes) throws Exception
    {
        int [] a_packetEnd = new int[] { bytes.position()-1 };
        header = new PacketHeader(bytes, a_packetEnd, PacketFrequency.Low);
        Region = new RegionBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        Stat = new StatBlock[count];
        for (int j = 0; j < count; j++)
        { Stat[j] = new StatBlock(bytes); }
        PidStat = new PidStatBlock(bytes);
     }

    public SimStatsPacket(PacketHeader head, ByteBuffer bytes)
    {
        header = head;
        Region = new RegionBlock(bytes);
        int count = (int)bytes.get() & 0xFF;
        Stat = new StatBlock[count];
        for (int j = 0; j < count; j++)
        { Stat[j] = new StatBlock(bytes); }
        PidStat = new PidStatBlock(bytes);
    }

    public int getLength()
    {
        int length = header.getLength();
        length += Region.getLength();
        length += PidStat.getLength();
        length++;
        for (int j = 0; j < Stat.length; j++) { length += Stat[j].getLength(); }
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
        Region.ToBytes(bytes);
        bytes.put((byte)Stat.length);
        for (int j = 0; j < Stat.length; j++) { Stat[j].ToBytes(bytes); }
        PidStat.ToBytes(bytes);
        if (header.AckList.length > 0) {
            header.AcksToBytes(bytes);
        }
        return bytes;
    }

    public String toString()
    {
        String output = "--- SimStats ---\n";
        output += Region.toString() + "\n";
        for (int j = 0; j < Stat.length; j++)
        {
            output += Stat[j].toString() + "\n";
        }
        output += PidStat.toString() + "\n";
        return output;
    }
}
