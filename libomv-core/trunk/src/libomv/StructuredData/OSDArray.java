/**
 * Copyright (c) 2006-2014, openmetaverse.org
 * Copyright (c) 2009-2017, Frederick Martian
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * - Neither the name of the openmetaverse.org or libomv-java project nor the
 *   names of its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
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
package libomv.StructuredData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import libomv.types.Color4;
import libomv.types.Quaternion;
import libomv.types.Vector2;
import libomv.types.Vector3;
import libomv.types.Vector3d;
import libomv.types.Vector4;

public class OSDArray extends OSD implements List<OSD>
{
	private ArrayList<OSD> value;

	@Override
	public OSDType getType()
	{
		return OSDType.Array;
	}

	public OSDArray()
	{
		value = new ArrayList<OSD>();
	}

	public OSDArray(int capacity)
	{
		value = new ArrayList<OSD>(capacity);
	}

	public OSDArray(ArrayList<OSD> value)
	{
		this.value = new ArrayList<OSD>(value);
	}

	@Override
	public boolean add(OSD osd)
	{
		return value.add(osd);
	}

	@Override
	public void add(int index, OSD osd)
	{
		value.add(index, osd);
	}

	@Override
	public boolean addAll(Collection<? extends OSD> coll)
	{
		return value.addAll(coll);
	}

	@Override
	public boolean addAll(int index, Collection<? extends OSD> coll)
	{
		return value.addAll(index, coll);
	}

	@Override
	public final void clear()
	{
		value.clear();
	}

	@Override
	public boolean contains(Object obj)
	{
		return value.contains(obj);
	}

	public final boolean contains(String element)
	{
		for (int i = 0; i < value.size(); i++)
		{
			if (value.get(i).getType() == OSDType.String && value.get(i).AsString() == element)
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean containsAll(Collection<?> objs)
	{
		return value.containsAll(objs);
	}

	@Override
	public OSD get(int index)
	{
		return value.get(index);
	}

	@Override
	public int indexOf(Object obj)
	{
		return value.indexOf(obj);
	}

	@Override
	public boolean isEmpty()
	{
		return size() == 0;
	}

	@Override
	public Iterator<OSD> iterator()
	{
		return value.iterator();
	}

	@Override
	public int lastIndexOf(Object obj)
	{
		return value.lastIndexOf(obj);
	}

	@Override
	public ListIterator<OSD> listIterator()
	{
		return value.listIterator();
	}

	@Override
	public ListIterator<OSD> listIterator(int index)
	{
		return value.listIterator(index);
	}

	@Override
	public boolean remove(Object key)
	{
		return value.remove(key);
	}

	public final boolean remove(OSD osd)
	{
		return value.remove(osd);
	}

	@Override
	public OSD remove(int key)
	{
		return value.remove(key);
	}

	@Override
	public boolean removeAll(Collection<?> values)
	{
		return value.removeAll(values);
	}

	@Override
	public boolean retainAll(Collection<?> values)
	{
		return value.retainAll(values);
	}

	@Override
	public OSD set(int index, OSD osd)
	{
		return value.set(index, osd);
	}

	@Override
	public int size()
	{
		return value.size();
	}

	@Override
	public List<OSD> subList(int from, int to)
	{
		return value.subList(from, to);
	}

	@Override
	public Object[] toArray()
	{
		return value.toArray();
	}

	@Override
	public <T> T[] toArray(T[] arg)
	{
		return value.toArray(arg);
	}

	@Override
	public byte[] AsBinary()
	{
		byte[] binary = new byte[value.size()];

		for (int i = 0; i < value.size(); i++)
		{
			binary[i] = (byte) value.get(i).AsInteger();
		}
		return binary;
	}

	@Override
	public long AsLong()
	{
		OSDBinary binary = new OSDBinary(AsBinary());
		return binary.AsLong();
	}

	@Override
	public long AsULong()
	{
		OSDBinary binary = new OSDBinary(AsBinary());
		return binary.AsULong();
	}

	@Override
	public int AsUInteger()
	{
		OSDBinary binary = new OSDBinary(AsBinary());
		return binary.AsUInteger();
	}

	@Override
	public Vector2 AsVector2()
	{
		Vector2 vector = new Vector2(Vector2.Zero);

		if (this.size() == 2)
		{
			vector.X = (float) this.get(0).AsReal();
			vector.Y = (float) this.get(1).AsReal();
		}

		return vector;
	}

	@Override
	public Vector3 AsVector3()
	{
		Vector3 vector = new Vector3(Vector3.Zero);

		if (this.size() == 3)
		{
			vector.X = (float) this.get(0).AsReal();
			vector.Y = (float) this.get(1).AsReal();
			vector.Z = (float) this.get(2).AsReal();
		}

		return vector;
	}

	@Override
	public Vector3d AsVector3d()
	{
		Vector3d vector = new Vector3d(Vector3d.Zero);

		if (this.size() == 3)
		{
			vector.X = this.get(0).AsReal();
			vector.Y = this.get(1).AsReal();
			vector.Z = this.get(2).AsReal();
		}

		return vector;
	}

	@Override
	public Vector4 AsVector4()
	{
		Vector4 vector = new Vector4(Vector4.Zero);

		if (this.size() == 4)
		{
			vector.X = (float) this.get(0).AsReal();
			vector.Y = (float) this.get(1).AsReal();
			vector.Z = (float) this.get(2).AsReal();
			vector.S = (float) this.get(3).AsReal();
		}
		return vector;
	}

	@Override
	public Quaternion AsQuaternion()
	{
		Quaternion quaternion = new Quaternion(Quaternion.Identity);

		if (this.size() == 4)
		{
			quaternion.X = (float) this.get(0).AsReal();
			quaternion.Y = (float) this.get(1).AsReal();
			quaternion.Z = (float) this.get(2).AsReal();
			quaternion.W = (float) this.get(3).AsReal();
		}
		return quaternion;
	}

	@Override
	public Color4 AsColor4()
	{
		Color4 color = new Color4(Color4.Black);

		if (this.size() == 4)
		{
			color.R = (float) this.get(0).AsReal();
			color.G = (float) this.get(1).AsReal();
			color.B = (float) this.get(2).AsReal();
			color.A = (float) this.get(3).AsReal();
		}
		return color;
	}

	@Override
	public boolean AsBoolean()
	{
		return value.size() > 0;
	}

	@Override
	public int hashCode()
	{
		return value.hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return obj != null && obj instanceof OSD && equals((OSD)obj);
	}

	public boolean equals(OSD osd)
	{
		return osd != null && osd.getType() == OSDType.Array && ((OSDArray)osd).value.equals(value);
	}

	public OSD clone()
	{
		OSDArray osd = (OSDArray)super.clone();
		osd.value = new ArrayList<OSD>(this.value);
		return osd;
	}

	@Override
	public String toString()
	{
		try
		{
			return OSDParser.serializeToString(this, OSDFormat.Notation);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
}
