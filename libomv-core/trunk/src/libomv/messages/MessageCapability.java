package libomv.messages;

import java.util.Date;

import libomv.GridClient;
import libomv.StructuredData.OSD;
import libomv.StructuredData.OSD.OSDType;
import libomv.StructuredData.OSDArray;
import libomv.StructuredData.OSDMap;
import libomv.types.Quaternion;
import libomv.types.UUID;
import libomv.types.Vector3;
import libomv.types.Vector3d;
import libomv.types.Vector4;

public class MessageCapability extends AbstractMessage
{
	private GridClient client;
	private OSDMap message;
	
	public MessageCapability(GridClient client)
	{
		this.client = client;
	}

	@Override
	public int getNumberOfBlocks(String blockName) throws Exception
	{
		OSD osd = message.get(blockName);
		return osd.getType() == OSDType.Array ? ((OSDArray)osd).size() : 0;
	}

	private OSD getField(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = message.get(blockName);
		if (osd.getType() == OSDType.Array)
			osd = ((OSDArray)osd).get(blockNumber);
		if (osd.getType() != OSDType.Map)
		   throw new Exception("Expected a Map of fields but got instead: " + osd.getType());
		return ((OSDMap)osd).get(fieldName);
	}
	
	@Override
	public byte getMessageI8(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return (byte)osd.AsInteger();
	}

	@Override
	public short getMessageU8(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return (short)osd.AsUInteger();
	}

	@Override
	public short getMessageI16(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return (short)osd.AsInteger();
	}

	@Override
	public int getMessageU16(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsUInteger();
	}

	@Override
	public int getMessageI32(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsInteger();
	}

	@Override
	public long getMessageU32(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsULong();
	}

	@Override
	public long getMessageI64(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsLong();
	}

	@Override
	public long getMessageU64(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsULong();
	}

	@Override
	public float getMessageF32(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return (float) osd.AsReal();
	}

	@Override
	public double getMessageF64(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsReal();
	}

	@Override
	public String getMessageString(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsString();
	}

	@Override
	public UUID getMessageUUID(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsUUID();
	}

	@Override
	public Date getMessageDate(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsDate();
	}

	@Override
	public Vector3 getMessageVector3(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsVector3();
	}

	@Override
	public Vector3d getMessageVector3d(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsVector3d();
	}

	@Override
	public Vector4 getMessageVector4(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsVector4();
	}

	@Override
	public Quaternion getMessageQuaternion(String blockName, String fieldName, short blockNumber) throws Exception
	{
		OSD osd = getField(blockName, fieldName, blockNumber);	
		return osd.AsQuaternion();
	}

	@Override
	public int getNumberOfBlocks(int blockName) throws Exception
	{
		return getNumberOfBlocks(client.Protocol.keywordPosition(blockName));
	}

	@Override
	public byte getMessageI8(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageI8(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public short getMessageU8(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageU8(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public short getMessageI16(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageI16(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public int getMessageU16(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageU16(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public int getMessageI32(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageI32(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public long getMessageU32(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageU32(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public long getMessageI64(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageI64(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public long getMessageU64(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageU64(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public float getMessageF32(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageF32(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public double getMessageF64(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageF64(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public String getMessageString(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageString(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public UUID getMessageUUID(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageUUID(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public Date getMessageDate(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageDate(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public Vector3 getMessageVector3(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageVector3(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}
	@Override
	public Vector3d getMessageVector3d(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageVector3d(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public Vector4 getMessageVector4(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageVector4(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}

	@Override
	public Quaternion getMessageQuaternion(int blockName, int fieldName, short blockNumber) throws Exception
	{
		return getMessageQuaternion(client.Protocol.keywordPosition(blockName), client.Protocol.keywordPosition(fieldName), blockNumber);
	}
}
