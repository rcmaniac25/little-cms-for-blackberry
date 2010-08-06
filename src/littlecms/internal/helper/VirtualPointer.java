//---------------------------------------------------------------------------------
//
// Permission is hereby granted, free of charge, to any person obtaining 
// a copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the Software 
// is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in 
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION 
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
//---------------------------------------------------------------------------------
//
package littlecms.internal.helper;

import littlecms.internal.Serializers;
import net.rim.device.api.util.Arrays;

/**
 * Virtual pointer system for data types
 */
public class VirtualPointer
{
	public static interface Serializer
	{
		public static final int STATUS_FAIL = 0;
		public static final int STATUS_SUCCESS = 1;
		public static final int STATUS_NEED_MORE_DATA = 2;
		public static final int STATUS_REQUIRES_OBJECT = 3;
		
		public int getSerializedSize(Object val);
		
		public int serialize(byte[] data, int offset, Object val);
		
		public int deserialize(byte[] data, int offset, Object[] val);
	}
	
	public static final int SIZE = 4; //Format: high-bit set to symbolize inner type, rest of int is index to inner stored inner type
	
	//XXX TEMP
	//Used for general purposes, it's possible to read outside these bounds
	private int[] id;
	private int[] itemPos;
	private int[] len;
	
	private int pos;
	private boolean resizeable;
	private byte[] data;
	private Serializer serEr;
	//XXX TEMP
	
	public VirtualPointer(int size)
	{
		if(!Serializers.initialize())
		{
			throw new IllegalStateException("Default serializers are not setup.");
		}
		
		if(size < 0)
		{
			throw new IndexOutOfBoundsException("size < 0");
		}
		pos = 0;
		data = new byte[size];
		
		id = new int[10];
		itemPos = new int[10];
		len = new int[10];
		
		resizeable = true;
	}
	
	public VirtualPointer(byte[] data)
	{
		this(data, false);
	}
	
	public VirtualPointer(byte[] data, boolean direct)
	{
		this(0); //Do this because if data is direct then we don't want any precreated data
		if(direct) //Direct means that if pointer is written to it will set the "data" that it was set to.
		{
			this.data = data;
			resizeable = false;
		}
		else
		{
			this.data = new byte[data.length];
			System.arraycopy(data, 0, this.data, 0, data.length);
		}
	}
	
	public VirtualPointer(VirtualPointer ptr)
	{
		this(ptr, 0);
	}
	
	public VirtualPointer(VirtualPointer ptr, int offset)
	{
		this(ptr.data, true);
		this.pos = ptr.pos + offset;
		
		//XXX item type, resizeable, serializer are not copied because they could be replaced
	}
	
	public VirtualPointer(Object obj)
	{
		this(obj, false);
	}
	
	public VirtualPointer(Object obj, boolean direct)
	{
		this(0); //Simply to get everything setup, this can be resized later
		
		//TODO: If object is not direct then it should be serialized to the data, if it is direct then it needs to have it's field positions marked. Whenever the values at
		//those positions are changed then the fields should be changed as well.
	}
	
	//TODO: Object constructor needs Serializer added arg too
	
	public void free()
	{
		this.data = null;
	}
	
	public void resize(int newSize)
	{
		//resizeable is ignored because a specific call to resize needs to be done
		if(newSize < 0 || newSize == this.data.length)
		{
			return;
		}
		byte[] temp = new byte[newSize];
		if(newSize < this.data.length)
		{
			System.arraycopy(this.data, 0, temp, 0, newSize);
		}
		else
		{
			System.arraycopy(this.data, 0, temp, 0, this.data.length);
		}
		this.data = temp;
	}
	
	//XXX TEMP
	private int getIndexFromPos()
	{
		int p = 0;
		int size = id.length;
		int i;
		for(i = 0; i < size; i++)
		{
			if(id[i] == 0 || p == pos)
			{
				break;
			}
			p += len[i];
		}
		return i;
	}
	
	private int getLastIndex()
	{
		int size = id.length;
		for(int i = 0; i < size; i++)
		{
			if(id[i] == 0)
			{
				return i;
			}
		}
		return -1;
	}
	
	private void expandIndex()
	{
		int tSize = id.length;
		int size = tSize + 10;
		int[] temp = new int[size];
		System.arraycopy(id, 0, temp, 0, tSize);
		id = Arrays.copy(temp);
		System.arraycopy(itemPos, 0, temp, 0, tSize);
		itemPos = Arrays.copy(temp);
		System.arraycopy(len, 0, temp, 0, tSize);
		len = Arrays.copy(temp);
	}
	//XXX TEMP
	
	public void readRaw(byte[] buffer, int offset, int len)
	{
		readRaw(buffer, offset, len, -1);
	}
	
	public void readRaw(byte[] buffer, int offset, int len, int directPos)
	{
		if(directPos < 0)
		{
			directPos = this.pos;
		}
		System.arraycopy(this.data, directPos, buffer, offset, len);
	}
	
	public void writeRaw(byte[] buffer, int offset, int len)
	{
		readRaw(buffer, offset, len, -1);
	}
	
	public void writeRaw(byte[] buffer, int offset, int len, int directPos)
	{
		if(directPos < 0)
		{
			directPos = this.pos;
		}
		if(directPos + len > this.data.length && resizeable)
		{
			byte[] nData = new byte[directPos + len];
			System.arraycopy(this.data, 0, nData, 0, this.data.length);
			this.data = nData;
		}
		else if(!resizeable)
		{
			throw new ArrayStoreException("Destination array not big enough");
		}
		System.arraycopy(buffer, offset, this.data, directPos, len);
	}
	
	public Object getDeserializedType()
	{
		return deserialize((Serializer)null, false);
	}
	
	public Object getDeserializedType(boolean increment)
	{
		return deserialize((Serializer)null, increment);
	}
	
	public Object getDeserializedTypeStructured()
	{
		return getDeserializedTypeStructured(false);
	}
	
	public Object getDeserializedTypeStructured(boolean increment)
	{
		return null; //TODO: Basically if data was written "int, int, long", the pos is 0 and this is called, it would read and return a int
	}
	
	public Object getDeserializedType(Class type)
	{
		return deserialize(VirtualPointer.getSerializer(type), false);
	}
	
	public Object getDeserializedType(Class type, boolean increment)
	{
		return deserialize(VirtualPointer.getSerializer(type), increment);
	}
	
	public Object getDeserializedType(Serializer serializer)
	{
		return deserialize(serializer, false);
	}
	
	public Object deserialize(Serializer serializer, boolean increment)
	{
		if(serializer == null)
		{
			if(this.serEr == null)
			{
				return null;
			}
			serializer = this.serEr;
		}
		int status;
		Object[] val = null;
		while(serializer != null && ((status = serializer.deserialize(this.data, this.pos, val)) != Serializer.STATUS_SUCCESS))
		{
			switch(status)
			{
				case Serializer.STATUS_FAIL: //Darn
				case Serializer.STATUS_NEED_MORE_DATA: //If not enough data exists then can't get the object
					return null;
				case Serializer.STATUS_REQUIRES_OBJECT: //Ok so the serializer is not the object to set, needs to create a secondary object.
					val = new Object[1];
					break;
			}
		}
		Object obj = val == null ? serializer : val[0];
		if(increment)
		{
			this.pos += serializer.getSerializedSize(obj);
		}
		return obj;
	}
	
	public VirtualPointer serialize(Object obj, Serializer serializer, boolean increment)
	{
		if(obj == null)
		{
			return null;
		}
		if(serializer == null)
		{
			serializer = VirtualPointer.getSerializer(obj.getClass());
			if(serializer == null)
			{
				return null; //No serializer
			}
		}
		int index = VirtualPointer.getSerializerIndex(serializer);
		int status;
		int size = this.data.length;
		while(serializer != null && ((status = serializer.serialize(this.data, this.pos, obj)) != Serializer.STATUS_SUCCESS))
		{
			switch(status)
			{
				case Serializer.STATUS_FAIL: //Darn
				case Serializer.STATUS_REQUIRES_OBJECT: //Object already passed in
					return null;
				case Serializer.STATUS_NEED_MORE_DATA: //Shouldn't get this otherwise getSerializedSize is not setup properly
					if(resizeable)
					{
						this.data = null;
						this.data = new byte[size += 100];
					}
					else
					{
						return null;
					}
					break;
			}
		}
		//TODO: Figure out how to setup id/itemPos/len. Pos can be changed so items might need to be moved around, items can get overridden, etc.
		
		if(increment)
		{
			this.pos += serializer.getSerializedSize(obj);
		}
		return this;
	}
	
	public int getPos()
	{
		return this.pos;
	}
	
	public void setPos(int pos)
	{
		this.pos = pos; //Checks would be nice but this is a virtual pointer... so you can go outside the array (if this happens a standard error will occur when processing).
	}
	
	//XXX TEMP
	public Serializer getDefaultSerializer()
	{
		return this.serEr;
	}
	
	public void setDefaultSerializer(Serializer ser)
	{
		this.serEr = ser;
	}
	
	public byte[] getBacking()
	{
		return this.data;
	}
	//XXX TEMP
	
	//------------Static----------------
	
	public static Serializer getSerializer(Class type)
	{
		return null; //TODO
	}
	
	public static Serializer setSerializer(Serializer ser, Class type)
	{
		return null; //TODO
	}
	
	private static int getSerializerIndex(Serializer ser)
	{
		return -1; //TODO
	}
	
	//XXX TEMP, Replace with constructor versions
	public static VirtualPointer wrap(Object obj)
	{
		return VirtualPointer.wrap(obj, VirtualPointer.getSerializer(obj.getClass()));
	}
	
	public static VirtualPointer wrap(Object obj, Class type)
	{
		return VirtualPointer.wrap(obj, VirtualPointer.getSerializer(type));
	}
	
	public static VirtualPointer wrap(Object obj, Serializer serializer)
	{
		if(obj == null)
		{
			return null;
		}
		if(serializer == null)
		{
			serializer = VirtualPointer.getSerializer(obj.getClass());
			if(serializer == null)
			{
				return null; //No serializer
			}
		}
		int size = serializer.getSerializedSize(obj);
		if(size < 0)
		{
			return null;
		}
		byte[] data = new byte[size];
		return new VirtualPointer(data).serialize(obj, serializer, false);
	}
	//XXX TEMP
	
	/*
	 * Ideas:
	 * -Have a default Serializer (but allow explicit Serializer).
	 * -Get/set raw byte array
	 * -Linking (so that a VirtualPointer can point to other pointers, that point to data or other pointers)
	 * -Continuation (so that a VirtualPointer can contain continues data even if they are separate elements)
	 */
}
