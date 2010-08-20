//#preprocessor

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

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import net.rim.device.api.collection.util.SparseList;

/**
 * Virtual pointer system for data types
 */
public class VirtualPointer
{
	public static final class TypeProcessor
    {
        private VirtualPointer vp;
        private int stat;
        
        TypeProcessor(VirtualPointer vp)
        {
            this.vp = vp;
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public boolean isValid()
        {
        	return this.vp.data != null;
        }
        
        public boolean lastOpSuccess()
        {
        	return this.stat == VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public int getStatus()
        {
        	return this.stat;
        }
        
        public void write(byte value)
        {
            write(value, false);
        }
        
        public void write(byte value, boolean inc)
        {
            vp.writeIn(new byte[] { value },
//#ifdef DEBUG
                VirtualPointer.Int8,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public byte readInt8()
        {
            return readInt8(false);
        }
        
        public byte readInt8(boolean inc)
        {
            byte[] buffer = new byte[1];
            vp.readByteArray(buffer, 0, 1, inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            return buffer[0];
        }
        
        public void write(boolean value)
        {
            write(value, false);
        }
        
        public void write(boolean value, boolean inc)
        {
            vp.writeIn(new byte[] { (byte)(value ? 1 : 0) },
//#ifdef DEBUG
                VirtualPointer.Bool,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public boolean readBool()
        {
            return readBool(false);
        }
        
        public boolean readBool(boolean inc)
        {
            byte[] buffer = new byte[1];
            vp.readByteArray(buffer, 0, 1, inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            return buffer[0] != 0;
        }
        
        public void write(short value)
        {
            write(value, false);
        }
        
        public void write(short value, boolean inc)
        {
            vp.writeIn(BitConverter.getBytes(value),
//#ifdef DEBUG
                VirtualPointer.Int16,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public short readInt16()
        {
            return readInt16(false);
        }
        
        public short readInt16(boolean inc)
        {
            byte[] buffer = new byte[2];
            vp.readByteArray(buffer, 0, 2, inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            return BitConverter.toInt16(buffer, 0);
        }
        
        public void write(int value)
        {
            write(value, false);
        }
        
        public void write(int value, boolean inc)
        {
            vp.writeIn(BitConverter.getBytes(value),
//#ifdef DEBUG
                VirtualPointer.Int32,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public int readInt32()
        {
            return readInt32(false);
        }
        
        public int readInt32(boolean inc)
        {
            byte[] buffer = new byte[4];
            vp.readByteArray(buffer, 0, 4, inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            return BitConverter.toInt32(buffer, 0);
        }
        
        public void write(long value)
        {
            write(value, false);
        }
        
        public void write(long value, boolean inc)
        {
            vp.writeIn(BitConverter.getBytes(value),
//#ifdef DEBUG
                VirtualPointer.Int64,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public long readInt64()
        {
            return readInt64(false);
        }
        
        public long readInt64(boolean inc)
        {
            byte[] buffer = new byte[8];
            vp.readByteArray(buffer, 0, 8, inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            return BitConverter.toInt64(buffer, 0);
        }
        
        public void write(char value)
        {
            write(value, false);
        }
        
        public void write(char value, boolean inc)
        {
            vp.writeIn(BitConverter.getBytes(value),
//#ifdef DEBUG
                VirtualPointer.Char,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public char readChar()
        {
            return readChar(false);
        }
        
        public char readChar(boolean inc)
        {
            byte[] buffer = new byte[2];
            vp.readByteArray(buffer, 0, 2, inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            return BitConverter.toChar(buffer, 0);
        }
        
        public void write(float value)
        {
            write(value, false);
        }
        
        public void write(float value, boolean inc)
        {
            vp.writeIn(BitConverter.getBytes(value),
//#ifdef DEBUG
                VirtualPointer.Float,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public float readFloat()
        {
            return readFloat(false);
        }
        
        public float readFloat(boolean inc)
        {
            byte[] buffer = new byte[4];
            vp.readByteArray(buffer, 0, 4, inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            return BitConverter.toFloat(buffer, 0);
        }
        
        public void write(double value)
        {
            write(value, false);
        }
        
        public void write(double value, boolean inc)
        {
            vp.writeIn(BitConverter.getBytes(value),
//#ifdef DEBUG
                VirtualPointer.Double,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }

        public double readDouble()
        {
            return readDouble(false);
        }

        public double readDouble(boolean inc)
        {
            byte[] buffer = new byte[8];
            vp.readByteArray(buffer, 0, 8, inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            return BitConverter.toDouble(buffer, 0);
        }
        
        public void write(String value)
        {
            write(value, false);
        }
        
        public void write(String value, boolean inc)
        {
            write(value, inc, true, false);
        }
        
        public void write(String value, boolean inc, boolean dynamicSel, boolean unicode)
        {
            if (dynamicSel)
            {
                //Save the most space, that is the goal
                char[] chars = value.toCharArray();
                int len = chars.length;
                for (int i = 0; i < len; i++)
                {
                    if (chars[i] > 0x7F)
                    {
                        unicode = true; //Ok so can't use ascii
                        break;
                    }
                }
            }
            String enc = unicode ? "UTF-16BE" : "ISO-8859-1";
            StringBuffer tvalue = new StringBuffer(value);
            tvalue.append('\0');
            byte[] data = null;
			try
			{
				data = tvalue.toString().getBytes(enc);
			}
			catch (UnsupportedEncodingException e)
			{
				//What? How?
			}
            vp.writeIn(data,
//#ifdef DEBUG
                unicode ? VirtualPointer.StringUnicode : VirtualPointer.StringAscii, 
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public String readString()
        {
            return readString(false);
        }
        
        public String readString(boolean inc)
        {
//#ifdef DEBUG
            switch(vp.types[vp.dataPos])
            {
                case VirtualPointer.StringAscii:
                    return readString(inc, false);
                case VirtualPointer.StringUnicode:
                    return readString(inc, true);
            }
//#endif
            int len = vp.data.length;
            for (int i = vp.dataPos; i < len; i++)
            {
                if (vp.data[i] > 0x7F)
                {
                    return readString(inc, true);
                }
            }
            return readString(inc, false);
        }
        
        public String readString(boolean inc, boolean unicode)
        {
            int actLen = 0;
//#ifdef DEBUG
            switch (vp.types[vp.dataPos])
            {
                case VirtualPointer.StringAscii:
                case VirtualPointer.StringUnicode:
                    actLen = vp.len[vp.dataPos]; //If valid data point exists then len exists too
                    break;
            }
            if (actLen == 0)
            {
//#endif
                int len = vp.data.length;
                if (unicode)
                {
                    for (int i = vp.dataPos; i < len; i += 2)
                    {
                        if (i < (len - 2))
                        {
                            if (BitConverter.toInt16(vp.data, i) == 0)
                            {
                                actLen = (i - vp.dataPos) + 2;
                                break;
                            }
                        }
                    }
                }
                else
                {
                    for (int i = vp.dataPos; i < len; i++)
                    {
                        if (vp.data[i] == 0)
                        {
                            actLen = (i - vp.dataPos) + 1;
                            break;
                        }
                    }
                }
                if (actLen == 0)
                {
                    actLen = unicode ? 2 : 1;
                    if (vp.dataPos < len)
                    {
                        actLen += len - vp.dataPos;
                    }
                }
//#ifdef DEBUG
            }
//#endif
            return readString(inc, unicode, actLen);
        }
        
        private String readString(boolean inc, boolean unicode, int actLen)
        {
            int len = actLen - (unicode ? 2 : 1);
            byte[] data = new byte[actLen];
            vp.readByteArray(data, 0, actLen, inc);
            String en = unicode ? "UTF-16BE" : "ISO-8859-1";
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            try
            {
            	return new String(data, 0, len, en);
            }
            catch(UnsupportedEncodingException e)
            {
            	//What? How?
            	return null;
            }
        }
        
        public void write(byte[] value)
        {
            write(value, false);
        }
        
        public void write(byte[] value, boolean inc)
        {
            vp.writeIn(value,
//#ifdef DEBUG
                VirtualPointer.ByteArray,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        //In C/C++ you don't get a byte array, you read one byte at a time until the size (stored separately) is met. At least that is the proper way.
        
        public void write(VirtualPointer value)
        {
            write(value, false);
        }

        public void write(VirtualPointer value, boolean inc)
        {
            int index = 0;
            if (value != null)
            {
                synchronized (VirtualPointer.pointerRef)
                {
                	index = VirtualPointer.pointerRef.getKey(value);
                	if(index < 0)
                    {
                		index = VirtualPointer.pointerRef.addAndGetIndex(value);
                    }
                	index++;
                }
            }
            vp.writeIn(BitConverter.getBytes(index),
//#ifdef DEBUG
                VirtualPointer.Pointer,
//#endif
                inc);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }

        public VirtualPointer readVirtualPointer()
        {
            return readVirtualPointer(false);
        }

        public VirtualPointer readVirtualPointer(boolean inc)
        {
            int index = readInt32(inc) - 1;
            if (index >= 0)
            {
                try
                {
                    this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
                    synchronized (VirtualPointer.pointerRef)
                    {
                    	return (VirtualPointer)VirtualPointer.pointerRef.get(index);
                    }
                }
                catch(Exception e)
                {
                    //Not a valid index
                }
            }
            this.stat = VirtualPointer.Serializer.STATUS_FAIL;
            return null;
        }
        
        public void write(Object value)
        {
            if (value == null)
            {
                return;
            }
            write(value, false);
        }
        
        public void write(Object value, boolean inc)
        {
            if (value == null)
            {
                return;
            }
            write(value, inc, value.getClass());
        }
        
        public void write(Object value, boolean inc, Class clazz)
        {
            write(value, inc, VirtualPointer.getSerializer(clazz));
        }
        
        public void write(Object value, boolean inc, Serializer ser)
        {
            if (value == null)
            {
                return; //Value is null, nothing to do
            }
            else if (ser == null)
            {
                ser = VirtualPointer.getSerializer(value.getClass());
                if (ser == null)
                {
                    return; //Couldn't find serializer
                }
            }
            if (ser.canProcess(value))
            {
                //Serializer can process data. Does the serializers have this?
                Class clazz = value.getClass();
                synchronized (VirtualPointer.serializers)
                {
                    if (!VirtualPointer.serializers.containsKey(clazz))
                    {
                        VirtualPointer.setSerializer(ser, clazz); //Apparently not, add it so that future use can be done explicitly
                    }
                }
            }
            else
            {
                return; //Serializer may be for that type but doesn't support serializing that object
            }
            int pos = vp.dataPos;
            this.stat = ser.serialize(this.vp, value);
            if (!inc)
            {
                vp.dataPos = pos;
            }
        }
        
        public Object readObject(Class clazz)
        {
            return readObject(VirtualPointer.getSerializer(clazz), false);
        }
        
        public Object readObject(Serializer ser)
        {
            return readObject(ser, false);
        }
        
        public Object readObject(Class clazz, boolean inc)
        {
            return readObject(VirtualPointer.getSerializer(clazz), inc);
        }
        
        public Object readObject(Serializer ser, boolean inc)
        {
            Object[] obj = null;
            int pos = vp.dataPos;
            this.stat = ser.deserialize(this.vp, obj);
            if (this.stat == VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT)
            {
                obj = new Object[1];
                vp.dataPos = pos;
                this.stat = ser.deserialize(this.vp, obj);
            }
            if (!inc)
            {
                vp.dataPos = pos;
            }
            if (obj != null)
            {
                Class clazz = obj[0].getClass();
                synchronized (VirtualPointer.serializers)
                {
                    if (!VirtualPointer.serializers.containsKey(clazz))
                    {
                        VirtualPointer.setSerializer(ser, clazz);
                    }
                }
            }
            return obj[0];
        }
    }
	
	public static interface Serializer
	{
		public static final int STATUS_FAIL = 0;
		public static final int STATUS_SUCCESS = 1;
		public static final int STATUS_NEED_MORE_DATA = 2;
		public static final int STATUS_REQUIRES_OBJECT = 3;
		public static final int STATUS_UNSUPPORTED = 4;
		
		public int getSerializedSize(Object val);
		
		/** Serialize <i>val</i>. Note: Pointers need to be incramented after writing the data to them.*/
		public int serialize(VirtualPointer vp, Object val);
		
		/** Deserialize a value into <i>val</i>. Note: Pointers need to be incramented after reading the data from them.*/
		public int deserialize(VirtualPointer vp, Object[] val);

		public boolean canProcess(Object val);

		public boolean canWrap(Object val);
	}
	
	/*
     * TODO: Determine if resizing invalidates any pointers based off the data
     * TODO: Figure out (de)serialization system so that if an object is added it will be 'registered" with the pointer. So if the pointer is changed, the object changes too. Don't think there is a way to allow the object to change to change the pointer. Also if the pointer is "lost" but the object is "wrapped", the original pointer should be returned. If an object is read more then once then the original object should be returned.
     */
	
	public static final int SIZE = 4; //Format: high-bit set to symbolize inner type, rest of int is index to inner stored inner type
	
	//TODO: Make these singletons
	private static SparseList pointerRef = new SparseList(); //Use a SparseList so that indexes don't change
	private static Hashtable serializers = new Hashtable();
	
	private byte[] data;
    private int dataPos;
    private TypeProcessor processor;
    private boolean resizeable;
    private Vector children;
    protected VirtualPointer parent;
//#ifdef DEBUG
    private byte[] types;
    private int[] len;
    
    private static final byte Invalid = 0;
    private static final byte ByteArray = Invalid + 1;
    private static final byte Bool = ByteArray + 1;
    private static final byte Int8 = Bool + 1;
    private static final byte Int16 = Int8 + 1;
    private static final byte Int32 = Int16 + 1;
    private static final byte Int64 = Int32 + 1;
    private static final byte Char = Int64 + 1;
    private static final byte Float = Char + 1;
    private static final byte Double = Float + 1;
    private static final byte StringAscii = Double + 1;
    private static final byte StringUnicode = StringAscii + 1;
    private static final byte Pointer = StringUnicode + 1;
//#endif
	
    public VirtualPointer(int size)
    {
        //PARENT CONSTRUCTOR
    	
        data = new byte[size];
        dataPos = 0;
        processor = null;
        resizeable = true;
//#ifdef DEBUG
        types = new byte[size];
        len = new int[size];
//#endif
    }
	
    public VirtualPointer(byte[] data)
    {
    	this(data, false);
    }
	
    public VirtualPointer(byte[] data, boolean direct)
	{
    	this(0); //Do this because if data is direct then we don't want any precreated data
    	int len = data.length;
	    if (direct) //Direct means that if pointer is written to it will set the "data" that it was set to.
	    {
	        this.data = data;
//#ifdef DEBUG
	        this.types = new byte[len];
	        this.len = new int[len];
//#endif
	        resizeable = false;
	    }
	    else
	    {
	        resize(len);
	        System.arraycopy(data, 0, this.data, 0, len);
	    }
	}
	
    public VirtualPointer(VirtualPointer parent)
	{
    	this(parent, 0);
	}
	
    public VirtualPointer(VirtualPointer parent, int pos)
	{
    	this(0);
	    if (parent.data == null)
	    {
	        this.data = null;
//#ifdef DEBUG
	        this.len = null;
	        this.types = null;
//#endif
	    }
	    else
	    {
	    	synchronized (parent.data)
	        {
	            this.data = parent.data;
	        }
//#ifdef DEBUG
	    	synchronized (parent.len)
	        {
	            this.len = parent.len;
	        }
	    	synchronized (parent.types)
	        {
	            this.types = parent.types;
	        }
//#endif
	    }
	    if (parent.children == null)
	    {
	        parent.children = new Vector();
	    }
	    synchronized (parent.children)
	    {
	        parent.children.addElement(this);
	    }
	    this.parent = parent;
	    this.dataPos = parent.dataPos + pos;
	}
	
    public VirtualPointer(Object obj)
	{
    	this(obj, null);
	}
	
    public VirtualPointer(Object obj, Serializer ser)
	{
    	this(0);
	    if (obj == null)
	    {
	        throw new NullPointerException("obj");
	    }
	    if (ser == null)
	    {
	        ser = VirtualPointer.getSerializer(obj.getClass());
	        if (ser == null)
	        {
	            throw new UnsupportedOperationException("obj needs Serializer in order to be converted to pointer");
	        }
	    }
	    resize(ser.getSerializedSize(obj));
	    this.getProcessor().write(obj, false, ser);
	}
    
    public VirtualPointer set(int value, int size)
    {
    	if(size <= 0 || dataPos > data.length)
    	{
    		return this;
    	}
    	size = Math.min(data.length, size);
    	//Set available data
    	byte val = (byte)value;
    	for(int i = dataPos; i < size; i++)
    	{
    		data[i] = val;
    	}
    	return this;
    }
	
    public VirtualPointer resize(int newsize)
    {
        if (!resizeable || newsize < 0)
        {
            return null;
        }
        else if (newsize == 0)
        {
            //equivilant of "realloc(ptr, 0)"
            this.free();
            return null;
        }
        else if (newsize == data.length)
        {
            return this;
        }
        else if (data == null)
        {
            //equivilant of "realloc(null, 'size > 0')"
            this.data = new byte[newsize];
            this.dataPos = 0;
//#ifdef DEBUG
            this.types = new byte[newsize];
            this.len = new int[newsize];
//#endif
        }
        //equivilant of "realloc(ptr, 'size > 0')"
        try
        {
            byte[] ndata = new byte[newsize];
            synchronized (data)
            {
                System.arraycopy(data, 0, ndata, 0, Math.min(newsize, data.length));
            }
            data = ndata;
//#ifdef DEBUG
            int[] nlen = new int[newsize];
            synchronized (len)
            {
            	System.arraycopy(len, 0, nlen, 0, Math.min(newsize, len.length));
            }
            len = nlen;
            byte[] ntypes = new byte[newsize];
            synchronized (types)
            {
            	System.arraycopy(types, 0, ntypes, 0, Math.min(newsize, types.length));
            }
            types = ntypes;
//#endif
        }
        catch(Exception e)
        {
            return null;
        }
        if (children != null)
        {
            synchronized (children)
            {
            	int count = children.size();
                for (int i = 0; i < count; i++)
                {
                    ((VirtualPointer)children.elementAt(i)).inResize(this);
                }
            }
        }
        return this;
    }
    
    private void inResize(VirtualPointer parent)
    {
        if (this.parent == parent) //Simple precaution
        {
        	synchronized (parent.data)
            {
                this.data = parent.data;
            }
//#ifdef DEBUG
			synchronized (parent.len)
            {
                this.len = parent.len;
            }
			synchronized (parent.types)
            {
                this.types = parent.types;
            }
//#endif
        }
    }
    
    public void free()
    {
        if (data == null)
        {
            return;
        }
        data = null;
        processor = null;
//#ifdef DEBUG
        len = null;
        types = null;
//#endif
        if (parent != null)
        {
            synchronized (parent.children)
            {
                parent.children.removeElement(this);
            }
            parent.free(); //This is because, since the event was removed, this won't get called again by the parent
        }
        if (children != null)
        {
        	synchronized (children)
            {
        		int count = children.size();
        		VirtualPointer[] tE = new VirtualPointer[count];
        		children.copyInto(tE); //Free function will remove itself from "events", so copy the list
                for (int i = 0; i < count; i++)
                {
                    tE[i].inFree(this);
                }
            }
        }
        if (parent != null)
        {
            if (parent.children != null && parent.children.size() == 0)
            {
                parent.children = null;
            }
            parent = null;
        }
        synchronized (VirtualPointer.pointerRef)
        {
            if (VirtualPointer.pointerRef.getKey(this) >= 0)
            {
                VirtualPointer.pointerRef.remove(this);
            }
        }
    }
    
    private void inFree(VirtualPointer parent)
    {
        if (this.parent == parent) //Simple precaution
        {
            free();
        }
    }
    
//#ifdef DEBUG
    public byte[] readStructuredByteArray(boolean inc)
    {
        if (dataPos < data.length)
        {
            if (types[dataPos] == Invalid)
            {
                return new byte[0];
            }
            byte[] dat = new byte[this.len[dataPos]];
            readByteArray(dat, 0, dat.length, inc);
            return dat;
        }
        return null;
    }
//#endif
    
    public int readByteArray(byte[] buffer, int offset, int len, boolean inc)
    {
        if (buffer == null)
        {
            if (data == null || dataPos >= data.length || len <= 0)
            {
                return 0;
            }
            else if (len + dataPos > data.length)
            {
                return Math.min(data.length - dataPos, len);
            }
            return len;
        }
        if (data == null)
        {
            Random rand = new Random();
            for(int i = offset; i < offset + len; i++)
            {
            	buffer[i] = (byte)rand.nextInt(255);
            }
            return 0;
        }
        else
        {
            if (len + dataPos > data.length)
            {
                int dif = (len - (data.length - dataPos));
                len -= dif;
                if (len > 0)
                {
                    synchronized (data)
                    {
                        System.arraycopy(data, dataPos, buffer, offset, len);
                    }
                }
                Random rand = new Random();
                int count;
                byte[] randData = new byte[(count = len >= 0 ? dif : dif + len)];
                for(int i = 0; i < count; i++)
                {
                	randData[i] = (byte)rand.nextInt(255);
                }
                System.arraycopy(randData, 0, buffer, len >= 0 ? len + offset : offset, randData.length);
                if (inc)
                {
                    dataPos += (len + dif);
                }
                return len + dif;
            }
            else
            {
            	synchronized (data)
                {
            		System.arraycopy(data, dataPos, buffer, offset, len);
                }
                if (inc)
                {
                    dataPos += len;
                }
                return len;
            }
        }
    }
    
    private void writeIn(byte[] value, 
//#ifdef DEBUG
                byte type, 
//#endif
                boolean inc)
    {
        if (data == null)
        {
            return;
        }
        int len = value.length;
//#ifdef DEBUG
        if (dataPos < data.length)
        {
            synchronized (this.types)
            {
                this.types[dataPos] = type;
            }
            synchronized (this.len)
            {
                this.len[dataPos] = len;
            }
        }
//#endif
        if (len + dataPos >= data.length)
        {
            len = data.length - dataPos;
        }
        if (len > 0)
        {
        	synchronized (this.data)
            {
                System.arraycopy(value, 0, data, dataPos, len);
            }
        }
        if (inc)
        {
            dataPos += value.length;
        }
    }
    
    public void readRaw(byte[] buffer, int offset, int len)
    {
        readRaw(buffer, offset, len, -1);
    }
    
    public void readRaw(byte[] buffer, int offset, int len, int directPos)
    {
        if (directPos < 0)
        {
            directPos = this.dataPos;
        }
        synchronized (this.data)
        {
            System.arraycopy(this.data, directPos, buffer, offset, len);
        }
    }
    
    public void writeRaw(byte[] buffer, int offset, int len)
    {
        readRaw(buffer, offset, len, -1);
    }
    
    public void writeRaw(byte[] buffer, int offset, int len, int directPos)
    {
        if (directPos < 0)
        {
            directPos = this.dataPos;
        }
        synchronized (this.data)
        {
        	System.arraycopy(buffer, offset, this.data, directPos, len);
        }
    }
    
    public int getPosition()
    {
        return dataPos;
    }
    
    public void setPosition(int pos)
    {
        dataPos = pos;
    }
    
    int getAllocLen()
    {
        if (data == null)
        {
            return -1;
        }
        return data.length;
    }
    
    public TypeProcessor getProcessor()
    {
        if (this.data == null)
        {
            return null;
        }
        if (this.processor == null)
        {
            this.processor = new VirtualPointer.TypeProcessor(this);
        }
        return this.processor;
    }
    
    public static Serializer getSerializer(Class clazz)
    {
        synchronized (VirtualPointer.serializers)
        {
        	return (Serializer)VirtualPointer.serializers.get(clazz);
        }
    }
    
    public static Serializer setSerializer(Serializer ser, Class clazz)
    {
    	synchronized (VirtualPointer.serializers)
        {
    		if(ser == null)
    		{
    			return (Serializer)VirtualPointer.serializers.remove(clazz);
    		}
    		else
    		{
    			return (Serializer)VirtualPointer.serializers.put(clazz, ser);
    		}
        }
    }
}
