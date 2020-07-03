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
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import littlecms.internal.LCMSResource;
import littlecms.internal.Serializers;

import net.rim.device.api.collection.util.SparseList;
import net.rim.device.api.util.Arrays;

/**
 * Virtual pointer system for data types
 * @Author Vinnie Simonetti
 */
public class VirtualPointer
{
	public static final class TypeProcessor
    {
		private static String UTF_ENCODING = "UTF-16BE";
		private static String ASCII_ENCODING = "ISO-8859-1";
		
        private VirtualPointer vp;
        private int stat;
        
        TypeProcessor(VirtualPointer vp)
        {
            this.vp = vp;
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        //Is this type processor valid?
        public boolean isValid()
        {
        	return this.vp.data != null;
        }
        
        //Was the last operation successful?
        public boolean lastOpSuccess()
        {
        	return this.stat == VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public int getStatus()
        {
        	return this.stat;
        }
        
        public int getPosition()
        {
        	return this.vp.getPosition();
        }
        
        public void setPosition(int pos)
        {
        	this.vp.setPosition(pos);
        }
        
        public void movePosition(int offset)
        {
        	this.vp.movePosition(offset);
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
                inc, false);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public byte readInt8()
        {
            return readInt8(false);
        }
        
        public byte readInt8(boolean inc)
        {
            byte[] buffer = new byte[1];
            vp.readByteArray(buffer, 0, 1, inc, false);
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
                inc, false);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public boolean readBool()
        {
            return readBool(false);
        }
        
        public boolean readBool(boolean inc)
        {
            byte[] buffer = new byte[1];
            vp.readByteArray(buffer, 0, 1, inc, false);
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
                inc, true);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public short readInt16()
        {
            return readInt16(false);
        }
        
        public short readInt16(boolean inc)
        {
            byte[] buffer = new byte[2];
            vp.readByteArray(buffer, 0, 2, inc, true);
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
                inc, true);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public int readInt32()
        {
            return readInt32(false);
        }
        
        public int readInt32(boolean inc)
        {
            byte[] buffer = new byte[4];
            vp.readByteArray(buffer, 0, 4, inc, true);
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
                inc, true);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public long readInt64()
        {
            return readInt64(false);
        }
        
        public long readInt64(boolean inc)
        {
            byte[] buffer = new byte[8];
            vp.readByteArray(buffer, 0, 8, inc, true);
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
                inc, false);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public char readChar()
        {
            return readChar(false);
        }
        
        public char readChar(boolean inc)
        {
            byte[] buffer = new byte[2];
            vp.readByteArray(buffer, 0, 2, inc, false);
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
                inc, true);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public float readFloat()
        {
            return readFloat(false);
        }
        
        public float readFloat(boolean inc)
        {
            byte[] buffer = new byte[4];
            vp.readByteArray(buffer, 0, 4, inc, true);
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
                inc, true);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }

        public double readDouble()
        {
            return readDouble(false);
        }

        public double readDouble(boolean inc)
        {
            byte[] buffer = new byte[8];
            vp.readByteArray(buffer, 0, 8, inc, true);
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
            String enc = unicode ? UTF_ENCODING : ASCII_ENCODING;
            StringBuffer tvalue = new StringBuffer(value);
            tvalue.append('\0');
            byte[] data = null;
			try
			{
				data = tvalue.toString().getBytes(enc);
				int len = Utility.strlen(data);
				if(len != data.length)
				{
					data = Arrays.copy(data, 0, len);
				}
			}
			catch (UnsupportedEncodingException e)
			{
				//What? How?
				this.stat = VirtualPointer.Serializer.STATUS_UNSUPPORTED;
				return;
			}
            vp.writeIn(data,
//#ifdef DEBUG
                unicode ? VirtualPointer.StringUnicode : VirtualPointer.StringAscii,
//#endif
                inc, false);
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
        }
        
        public String readString()
        {
            return readString(false);
        }
        
        public String readString(boolean inc)
        {
//#ifdef DEBUG
        	//If we are in debug mode then we can see if we can take a shortcut to determining the String type
            switch(vp.types[vp.dataPos])
            {
                case VirtualPointer.StringAscii:
                    return readString(inc, false);
                case VirtualPointer.StringUnicode:
                    return readString(inc, true);
            }
//#endif
            //Need to figure out String type
            int len = vp.data.length;
            for (int i = vp.dataPos; i < len; i++)
            {
                if ((vp.data[i] & 0xFF) > 0x7F)
                {
                	//Byte has the high-bit set, this means UTF-8
                    return readString(inc, true);
                }
            }
            //ASCII
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
                            if (BitConverter.toInt16(vp.data, i) == 0) //This isn't normal (null byte is, well, a byte) but it adds extra safety and confidence
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
                	//Reached the end of array without finding null byte, must be the entire portion of the array
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
            vp.readByteArray(data, 0, actLen, inc, false);
            String en = unicode ? UTF_ENCODING : ASCII_ENCODING;
            this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            try
            {
            	return new String(data, 0, len, en);
            }
            catch(UnsupportedEncodingException e)
            {
            	//What? How?
            	this.stat = VirtualPointer.Serializer.STATUS_UNSUPPORTED;
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
                inc, false);
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
                	if(!this.vp.cleanChildren())
                	{
                		VirtualPointer.clearRefPointers();
                	}
                	if(value.ref == null)
                	{
                		value.ref = new WeakReference(value);
                	}
                	index = VirtualPointer.pointerRef.getKey(value.ref);
                	if(index < 0)
                    {
                		index = VirtualPointer.pointerRef.addAndGetIndex(value.ref);
                    }
                	index++;
                }
            }
            vp.writeIn(BitConverter.getBytes(index),
//#ifdef DEBUG
                VirtualPointer.Pointer,
//#endif
                inc, false);
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
            	this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
            	if(!this.vp.cleanChildren())
            	{
            		VirtualPointer.clearRefPointers();
            	}
                synchronized (VirtualPointer.pointerRef)
                {
                	WeakReference ref = (WeakReference)VirtualPointer.pointerRef.get(index);
                	VirtualPointer vp = (VirtualPointer)ref.get();
                	if(vp != null)
                	{
                		return vp;
                	}
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
        
        public void write(Object value, int size)
        {
        	if (value == null)
            {
                return;
            }
            write(value, false, size);
        }
        
        public void write(Object value, boolean inc)
        {
            if (value == null)
            {
                return;
            }
            write(value, inc, value.getClass());
        }
        
        public void write(Object value, boolean inc, int size)
        {
        	if (value == null)
            {
                return;
            }
            write(value, inc, value.getClass(), size);
        }
        
        public void write(Object value, boolean inc, Class clazz)
        {
            write(value, inc, VirtualPointer.getSerializer(clazz));
        }
        
        public void write(Object value, boolean inc, Class clazz, int size)
        {
        	write(value, inc, VirtualPointer.getSerializer(clazz), size);
        }
        
        public void write(Object value, boolean inc, Serializer ser)
        {
        	write(value, inc, ser, -1);
        }
        
        public void write(Object value, boolean inc, Serializer ser, int size)
        {
            if (value == null)
            {
                return; //Value is null, nothing to do
            }
            boolean array = value.getClass().isArray();
            int len = -1;
            Object[] objArray = null;
            if (ser == null)
            {
                ser = VirtualPointer.getSerializer(value.getClass());
                if (ser == null)
                {
                	if(array)
                	{
                		if(Utility.isPrimitive(value))
                		{
                			//Primitive array
                			writePrimitiveArray(value, inc, size);
                			this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
                			return;
                		}
                		objArray = (Object[])value;
                		//Only exceptions to the cast rule is if the elements are String or VirtualPointer
                		if(value instanceof String[] || value instanceof VirtualPointer[])
                		{
                			//Primitive array
                			writePrimitiveArray(value, inc, size);
                			this.stat = VirtualPointer.Serializer.STATUS_SUCCESS;
                			return;
                		}
                		len = objArray.length;
                		Class clz = null;
                		//Make sure there are no null elements because there is no serialized value for null
                		for(int i = 0; i < len; i++)
                		{
                			if(objArray[i] == null)
                			{
                				this.stat = VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT;
                				return; //Can't handle null elements
                			}
                			else if(!objArray[i].getClass().equals(clz))
                			{
                				if(i == 0)
                				{
                					clz = objArray[i].getClass();
                				}
                				else
                				{
                					this.stat = VirtualPointer.Serializer.STATUS_UNSUPPORTED;
                					return; //Multi-type arrays are not supported
                				}
                			}
                		}
                		ser = VirtualPointer.getSerializer(clz);
                		if(ser == null)
                		{
                			this.stat = VirtualPointer.Serializer.STATUS_FAIL;
                			return; //Couldn't find serializer
                		}
                	}
                	else
                	{
                		this.stat = VirtualPointer.Serializer.STATUS_FAIL;
                		return; //Couldn't find serializer
                	}
                }
            }
            if(objArray == null)
            {
            	if(array)
            	{
            		if(Utility.isPrimitive(value, true))
            		{
            			//If it got this far then a serializer exists, continue processing
            			len = -1;
            		}
            		else
            		{
            			objArray = (Object[])value;
            			len = objArray.length;
                		//Make sure there are no null elements because there is no serialized value for null
                		for(int i = 0; i < len; i++)
                		{
                			if(objArray[i] == null)
                			{
                				this.stat = VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT;
                				return; //Can't handle null elements
                			}
                		}
            		}
            	}
            	else
            	{
            		len = 1;
            		objArray = new Object[]{value}; //We know this is a Object and not a primitive so we can just do this
            	}
            }
            
            boolean arraySupport;
            if (ser.canProcess(objArray))
            {
            	arraySupport = true;
                //Serializer can process data. Does the serializers have this?
                Class clazz = value.getClass();
                Class clazz2 = objArray[0].getClass();
                synchronized (VirtualPointer.serializers)
                {
                    if (!VirtualPointer.serializers.containsKey(clazz.toString()))
                    {
                        VirtualPointer.setSerializer(ser, clazz); //Apparently not, add it so that future use can be done explicitly
                    }
                    if (!VirtualPointer.serializers.containsKey(clazz2.toString()))
                    {
                        VirtualPointer.setSerializer(ser, clazz2); //Apparently not, add it so that future use can be done explicitly
                    }
                }
            }
            else
            {
            	arraySupport = false;
            	for(int i = 0; i < len; i++)
            	{
            		if(!ser.canProcess(objArray[i]))
            		{
            			this.stat = VirtualPointer.Serializer.STATUS_UNSUPPORTED;
            			return; //Serializer may be for that type but doesn't support serializing that object
            		}
            	}
            }
            int pos = vp.dataPos;
            if(arraySupport)
            {
            	this.stat = ser.serialize(this.vp, value);
            }
            else
            {
            	//Write each item, if an error occurs then stop processing
            	this.stat = Serializer.STATUS_SUCCESS; //Just to prevent problems
            	for(int i = 0; i < len && this.stat == Serializer.STATUS_SUCCESS; i++)
            	{
            		this.stat = ser.serialize(this.vp, objArray[i]);
            	}
            }
            if (!inc)
            {
                vp.dataPos = pos;
            }
            else if(size >= 0 && vp.dataPos - pos != size)
            {
            	//Too many/Not enough bytes written, make sure size matches
            	vp.dataPos = pos + size;
            }
        }
        
        private void writePrimitiveArray(Object value, boolean inc, int size)
        {
        	int pos = vp.dataPos;
        	int blen = 0;
        	if(value instanceof byte[])
        	{
        		blen = 1;
        		byte[] val = (byte[])value;
        		this.write(val, true);
        	}
        	else if(value instanceof short[])
        	{
        		blen = 2;
        		short[] val = (short[])value;
        		int len = val.length;
        		for(int i = 0; i < len; i++)
        		{
        			this.write(val[i], true);
        		}
        	}
        	else if(value instanceof char[])
        	{
        		blen = 2;
        		char[] val = (char[])value;
        		int len = val.length;
        		for(int i = 0; i < len; i++)
        		{
        			this.write(val[i], true);
        		}
        	}
        	else if(value instanceof int[])
        	{
        		blen = 4;
        		int[] val = (int[])value;
        		int len = val.length;
        		for(int i = 0; i < len; i++)
        		{
        			this.write(val[i], true);
        		}
        	}
        	else if(value instanceof long[])
        	{
        		blen = 8;
        		long[] val = (long[])value;
        		int len = val.length;
        		for(int i = 0; i < len; i++)
        		{
        			this.write(val[i], true);
        		}
        	}
        	else if(value instanceof float[])
        	{
        		blen = 4;
        		float[] val = (float[])value;
        		int len = val.length;
        		for(int i = 0; i < len; i++)
        		{
        			this.write(val[i], true);
        		}
        	}
        	else if(value instanceof double[])
        	{
        		blen = 8;
        		double[] val = (double[])value;
        		int len = val.length;
        		for(int i = 0; i < len; i++)
        		{
        			this.write(val[i], true);
        		}
        	}
        	else if(value instanceof boolean[])
        	{
        		blen = 1;
        		boolean[] val = (boolean[])value;
        		int len = val.length;
        		for(int i = 0; i < len; i++)
        		{
        			this.write(val[i], true);
        		}
        	}
        	else if(value instanceof String[])
        	{
        		blen = 1; //Interpret size as byte count
        		String[] val = (String[])value;
        		int len = val.length;
        		int tlen = 0; //Keep a count on how many bytes are written (but only for the number of Strings that are expected to be written), this will be used as a limit
        		for(int i = 0, tcount = 0; i < len; i++, tcount++)
        		{
        			int tpos = this.vp.dataPos;
        			this.write(val[i], true);
        			if(tcount < size)
        			{
        				tlen += this.vp.dataPos - tpos;
        			}
        		}
        		size = tlen; //Slight defeat of the purpose of "number of bytes written" since size in interpreted as item count instead of byte count
        	}
        	else if(value instanceof VirtualPointer[])
        	{
        		blen = VirtualPointer.SIZE;
        		VirtualPointer[] val = (VirtualPointer[])value;
        		int len = val.length;
        		for(int i = 0; i < len; i++)
        		{
        			this.write(val[i], true);
        		}
        	}
        	else
        	{
        		//If it gets here without running then something is up
        		this.stat = VirtualPointer.Serializer.STATUS_FAIL;
        		return;
        	}
        	if (!inc)
            {
                vp.dataPos = pos;
            }
            else if(size >= 0 && blen != -1 && vp.dataPos - pos != (size * blen))
            {
            	//Too many/Not enough bytes written, make sure size matches
            	vp.dataPos = pos + (size * blen);
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
            return readObject(VirtualPointer.getSerializer(clazz), inc, null);
        }
        
        public Object readObject(Serializer ser, boolean inc)
        {
            return readObject(ser, inc, null);
        }
        
        public Object readObject(Class clazz, boolean inc, Object org)
        {
            return readObject(VirtualPointer.getSerializer(clazz), inc, org);
        }
        
        public Object readObject(Serializer ser, boolean inc, Object org)
        {
            Object[] obj = org == null ? null : new Object[]{org};
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
            if (obj != null && obj[0] != null)
            {
                Class clazz = obj[0].getClass();
                synchronized (VirtualPointer.serializers)
                {
                    if (!VirtualPointer.serializers.containsKey(clazz.toString()))
                    {
                        VirtualPointer.setSerializer(ser, clazz);
                    }
                }
            }
            return obj != null ? obj[0] : null;
        }
        
        //Primitive array
        
        public Object readArray(final String primitiveType, int len)
        {
        	return readArray(false, primitiveType, len);
        }
        
        public Object readArray(boolean inc, final String primitiveType, int len)
        {
        	return readArray(inc, primitiveType, len, false, null);
        }
        
        public Object readArray(boolean inc, final String primitiveType, int len, Object org)
        {
        	return readArray(inc, primitiveType, len, false, org);
        }
        
        public Object readArray(boolean inc, final String primitiveType, int len, boolean lenIsBytes, Object org)
        {
        	return readArray((Serializer)null, inc, true, primitiveType, len, lenIsBytes, org);
        }
        
        //Object array
        
        public Object readArray(Serializer ser, int len)
        {
        	return readArray(ser, false, len);
        }
        
        public Object readArray(Class clazz, int len)
        {
        	return readArray(VirtualPointer.getSerializer(clazz), false, len);
        }
        
        public Object readArray(Serializer ser, boolean inc, int len)
        {
        	return readArray(ser, inc, len, false, null);
        }
        
        public Object readArray(Class clazz, boolean inc, int len)
        {
        	return readArray(VirtualPointer.getSerializer(clazz), inc, len, false, null);
        }
        
        public Object readArray(Serializer ser, boolean inc, int len, Object org)
        {
        	return readArray(ser, inc, len, false, org);
        }
        
        public Object readArray(Class clazz, boolean inc, int len, Object org)
        {
        	return readArray(VirtualPointer.getSerializer(clazz), inc, len, false, org);
        }
        
        public Object readArray(Serializer ser, boolean inc, int len, boolean lenIsBytes, Object org)
        {
        	return readArray(ser, inc, false, null, len, lenIsBytes, org);
        }
        
        public Object readArray(Class clazz, boolean inc, int len, boolean lenIsBytes, Object org)
        {
        	return readArray(VirtualPointer.getSerializer(clazz), inc, false, null, len, lenIsBytes, org);
        }
        
        public Object readArray(Class clazz, boolean inc, boolean primitive, final String primitiveType, int len, boolean lenIsBytes, Object org)
        {
        	return readArray(VirtualPointer.getSerializer(clazz), inc, primitive, primitiveType, len, lenIsBytes, org);
        }
        
        /**
         * <strong>NOTE:</strong> "String" and "VirtualPointer" are primitive types even though they are objects. This is because they can be read and
         * written directly from TypeProcessor.
         */
        public Object readArray(Serializer ser, boolean inc, boolean primitive, final String primitiveType, int len, boolean lenIsBytes, Object org)
        {
        	int pos = vp.dataPos;
        	int[] count = new int[1];
        	Object[] result = new Object[3];
        	result[1] = count;
        	if(org != null)
    		{
        		//If an original value already exists then add it to the results so that it can be used instead of making a new array
    			result[2] = org;
    		}
        	if(primitive)
        	{
        		readPrimitiveArray(primitiveType, len, lenIsBytes, result);
        	}
        	else
        	{
        		result[0] = new Integer(lenIsBytes ? -len : len);
        		this.stat = ser.deserialize(this.vp, result); //Serializer's are required to support arrays
        	}
        	if (!inc)
            {
                vp.dataPos = pos;
            }
        	else
        	{
        		if(!lenIsBytes)
        		{
        			len = count[0];
        		}
        		if(len >= 0 && vp.dataPos - pos != len)
                {
                	//Too many/Not enough bytes written, make sure size matches
                	vp.dataPos = pos + len;
                }
        	}
        	return result[2];
        }
        
        private void readPrimitiveArray(final String primitiveType, int len, boolean lenIsBytes, Object[] result)
        {
        	int count = 0;
        	int elementCount = 0;
        	if(primitiveType.equals("byte"))
        	{
        		byte[] arr;
        		elementCount = count = len;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new byte[elementCount]);
        		}
        		else
        		{
        			arr = (byte[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		this.vp.readByteArray(arr, 0, elementCount, true, false);
        	}
        	else if(primitiveType.equals("short"))
        	{
        		short[] arr;
        		if(lenIsBytes)
        		{
        			len >>= 1;
        		}
        		count = (elementCount = len) * 2;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new short[elementCount]);
        		}
        		else
        		{
        			arr = (short[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		for(int i = 0; i < elementCount; i++)
        		{
        			arr[i] = readInt16(true);
        		}
        	}
        	else if(primitiveType.equals("char"))
        	{
        		char[] arr;
        		if(lenIsBytes)
        		{
        			len >>= 1;
        		}
        		count = (elementCount = len) * 2;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new char[elementCount]);
        		}
        		else
        		{
        			arr = (char[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		for(int i = 0; i < elementCount; i++)
        		{
        			arr[i] = readChar(true);
        		}
        	}
        	else if(primitiveType.equals("int"))
        	{
        		int[] arr;
        		if(lenIsBytes)
        		{
        			len >>= 2;
        		}
        		count = (elementCount = len) * 4;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new int[elementCount]);
        		}
        		else
        		{
        			arr = (int[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		for(int i = 0; i < elementCount; i++)
        		{
        			arr[i] = readInt32(true);
        		}
        	}
        	else if(primitiveType.equals("long"))
        	{
        		long[] arr;
        		if(lenIsBytes)
        		{
        			len >>= 3;
        		}
        		count = (elementCount = len) * 8;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new long[elementCount]);
        		}
        		else
        		{
        			arr = (long[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		for(int i = 0; i < elementCount; i++)
        		{
        			arr[i] = readInt64(true);
        		}
        	}
        	else if(primitiveType.equals("float"))
        	{
        		float[] arr;
        		if(lenIsBytes)
        		{
        			len >>= 2;
        		}
        		count = (elementCount = len) * 4;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new float[elementCount]);
        		}
        		else
        		{
        			arr = (float[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		for(int i = 0; i < elementCount; i++)
        		{
        			arr[i] = readFloat(true);
        		}
        	}
        	else if(primitiveType.equals("double"))
        	{
        		double[] arr;
        		if(lenIsBytes)
        		{
        			len >>= 3;
        		}
        		count = (elementCount = len) * 8;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new double[elementCount]);
        		}
        		else
        		{
        			arr = (double[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		for(int i = 0; i < elementCount; i++)
        		{
        			arr[i] = readDouble(true);
        		}
        	}
        	else if(primitiveType.equals("boolean"))
        	{
        		boolean[] arr;
        		elementCount = count = len;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new boolean[elementCount]);
        		}
        		else
        		{
        			arr = (boolean[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		for(int i = 0; i < elementCount; i++)
        		{
        			arr[i] = readBool(true);
        		}
        	}
        	else if(primitiveType.equals("String"))
        	{
        		String[] arr;
        		elementCount = len;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new String[elementCount]);
        		}
        		else
        		{
        			arr = (String[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		int or;
        		for(int i = 0; i < elementCount; i++)
        		{
        			or = this.vp.dataPos;
        			arr[i] = readString(true);
        			count += this.vp.dataPos - or;
        		}
        	}
        	else if(primitiveType.equals("VirtualPointer"))
        	{
        		VirtualPointer[] arr;
        		if(lenIsBytes)
        		{
        			len /= SIZE;
        		}
        		count = (elementCount = len) * SIZE;
        		if(result[2] == null)
        		{
        			result[2] = (arr = new VirtualPointer[elementCount]);
        		}
        		else
        		{
        			arr = (VirtualPointer[])result[2];
        			elementCount = Math.min(elementCount, arr.length);
        		}
        		for(int i = 0; i < elementCount; i++)
        		{
        			arr[i] = readVirtualPointer(true);
        		}
        	}
        	((int[])result[1])[0] = count;
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
		
		/** Serialize <i>val</i>. Note: Pointers need to be incremented after writing the data to them. Array support is not necessary.*/
		public int serialize(VirtualPointer vp, Object val);
		
		/**
		 * Deserialize a value into <i>val</i>. Note: Pointers need to be incremented after reading the data from them. If <i>val</i>'s length is greater then 1 then
		 * the Serializer should deserialize an array of items. Array support is necessary (specified below):
		 * <p>
		 * The first element is a Integer that specifies how many elements are in the array, if this value is
		 * negative then it is the number of bytes that should be read (simply negate the number). If the element has a fixed size then the size can simply be
		 * divided to get the number of elements, else the Serializer should read until the byte count equals or exceeds the specified size, VirtualPointer will take
		 * care of the rest.
		 * <p>
		 * The second element is the total read in byte count, it is a int[] of one element and should be set before returning out of the function.
		 * <p>
		 * Finally the last element is the data itself, if this is not null then it should be used to set the data. Do not take the first element in the <i>val</i>
		 * array as fact, if this element in the array is smaller then what the first element says then take this element's length instead of the first element.
		 * If it is null then take the first element in <i>val</i> and use that as the length the array should be. Note that if the first parameter does not
		 * end on the proper bounds for an element the the rest of the element should be made up so as to not mix up data (as if the element went outside of it's
		 * allocated bounds where it is undefined).
		 */
		public int deserialize(VirtualPointer vp, Object[] val);
		
		public boolean canProcess(Object val);
		
		public boolean canWrap(Object val);
	}
	
	/*
     * TODO: Determine if resizing invalidates any pointers based off the data
     * TODO: Create basic type serializers (Integer, Long, etc.)
     * TODO: Figure out how to handle "other" resources. Like ones that can't be serialized. This a cmsPipeline, can't serialize the lerp functions.
     * TODO: Figure out (de)serialization system so that if an object is added it will be 'registered" with the pointer. So if the pointer is changed, the object changes too. Don't think there is a way to allow the object to change to change the pointer. Also if the pointer is "lost" but the object is "wrapped", the original pointer should be returned. If an object is read more then once then the original object should be returned.
     */
	
	public static final int SIZE = 4; //Format: high-bit set to symbolize inner type, rest of int is index to inner stored inner type
	
	private static final long POINTER_REF_UID = 0xAE7A0376ECFF1112L;
	private static final long SERIALIZERS_UID = 0x507EA7834E510030L;
	
	private static SparseList pointerRef;
	private static Hashtable serializers;
	
	static
	{
		Object obj = Utility.singletonStorageGet(POINTER_REF_UID);
		if(obj == null)
		{
			pointerRef = new SparseList(); //Use a SparseList so that indexes don't change
			serializers = new Hashtable();
			Utility.singletonStorageSet(POINTER_REF_UID, pointerRef);
			Utility.singletonStorageSet(SERIALIZERS_UID, serializers);
			if(!Serializers.initialize())
			{
				throw new IllegalStateException();
			}
		}
		else
		{
			pointerRef = (SparseList)obj;
			serializers = (Hashtable)Utility.singletonStorageGet(SERIALIZERS_UID);
		}
	}
	
	private byte[] data;
    private int dataPos;
    private TypeProcessor processor;
    private boolean resizeable;
    private Vector children;
    private WeakReference ref;
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
    	int len = data == null ? 0 : data.length;
	    if (direct) //Direct means that if pointer is written to it will set the "data" that it was set to.
	    {
	        this.data = data;
//#ifdef DEBUG
	        this.types = new byte[len];
	        this.len = new int[len];
//#endif
	        this.resizeable = false;
	    }
	    else
	    {
	        resize(len);
	        if(data != null)
	        {
	        	System.arraycopy(data, 0, this.data, 0, len);
	        }
	    }
	}
	
    public VirtualPointer(VirtualPointer parent)
	{
    	this(parent, 0);
	}
	
    public VirtualPointer(VirtualPointer parent, int pos)
	{
    	this(0);
	    if (parent == null || parent.data == null)
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
	    if (parent != null && parent.children == null)
	    {
	        parent.children = new Vector();
	    }
	    if(parent != null)
	    {
		    synchronized (parent.children)
		    {
		    	if(this.ref == null)
            	{
            		this.ref = new WeakReference(this);
            	}
		        parent.children.addElement(this.ref);
		    }
		    parent.cleanChildren();
	    }
	    this.parent = parent;
	    this.dataPos = (parent == null ? 0 : parent.dataPos) + pos;
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
	    //An inefficient process using functions already defined
	    VirtualPointer.TypeProcessor proc = this.getProcessor();
	    //Write the object and tell it to move the pointer
    	proc.write(obj, true, ser);
    	switch(proc.stat)
    	{
	    	case VirtualPointer.Serializer.STATUS_SUCCESS:
	    		break;
	    	case VirtualPointer.Serializer.STATUS_NEED_MORE_DATA:
	    		//Since it needs more data then I expect that it knows how much data is needed, so it has a serializer
	    		if(ser == null)
	    		{
	    			Class clazz = obj.getClass();
	    			if(clazz.isArray())
	    			{
	    				clazz = ((Object[])obj)[0].getClass();
	    			}
	    			ser = VirtualPointer.getSerializer(clazz);
	    		}
	    		this.data = null;
		    	this.resize(ser.getSerializedSize(obj));
		    	proc.write(obj, true, ser);
	    		if(proc.stat != VirtualPointer.Serializer.STATUS_SUCCESS)
	    		{
	    			throw new UnsupportedOperationException(Utility.LCMS_Resources.getString(LCMSResource.VP_NO_SERIALIZER));
	    		}
	    		break;
    		default:
    			throw new UnsupportedOperationException(Utility.LCMS_Resources.getString(LCMSResource.VP_NO_SERIALIZER));
    	}
    	//Now we have the position (AKA the length required to write the object), resize the pointer
    	if(this.data.length == 0)
    	{
	    	this.data = null;
	    	this.resize(this.dataPos);
    	}
	    //Now write out the actual object
	    if(this.dataPos == 0)
	    {
		    proc.write(obj, false, ser);
	    }
	    else
	    {
	    	this.dataPos = 0;
	    }
	}
    
    public VirtualPointer set(int value, int size)
    {
    	synchronized (this.data)
    	{
	    	if(size <= 0 || this.dataPos > this.data.length)
	    	{
	    		return this;
	    	}
	    	size = Math.min(this.data.length, size);
	    	//Set available data
	    	byte val = (byte)value;
	    	for(int i = this.dataPos; i < size; i++)
	    	{
	    		this.data[i] = val;
	    	}
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
            //Equivalent of "realloc(ptr, 0)"
            this.free();
            return null;
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
            //It is not expected that there are any children
            return this;
        }
        else if (newsize == data.length)
        {
            return this;
        }
        //Equivalent of "realloc(ptr, 'size > 0')"
        byte[] ndata = new byte[newsize];
        //Technically, if a pointer gets reallocated to a new pointer (so the original pointer no longer exists), the old pointer is freed. Since we know all our children, we can simply update them ourselves.
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
        if (children != null)
        {
            synchronized (children)
            {
            	cleanChildren();
            	int count = children.size();
                for (int i = 0; i < count; i++)
                {
                    ((VirtualPointer)((WeakReference)children.elementAt(i)).get()).inResize(this);
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
    
    public boolean isFree()
    {
    	return data == null;
    }
    
    private boolean cleanChildren()
    {
    	if(this.children != null)
    	{
    		boolean checkRef = false;
    		//First see if any "children" are inaccessable
    		for(int i = this.children.size() - 1; i >= 0; i--)
    		{
    			WeakReference ref = (WeakReference)this.children.elementAt(i);
    			VirtualPointer vp = (VirtualPointer)ref.get();
    			if(vp == null)
    			{
    				//Found one
    				ref.clear(); //Just in case
    				checkRef = true;
    				this.children.removeElementAt(i);
    			}
    			else
    			{
    				vp.cleanChildren();
    			}
    		}
    		if(this.children.size() == 0)
    		{
    			this.children = null;
    		}
    		if(checkRef)
        	{
    			//If an inaccessable VirtualPointer is found then check pointerRef because it means that one might be invalid there
    			VirtualPointer.clearRefPointers();
    			return true;
        	}
    	}
    	return false;
    }
    
    private static void clearRefPointers()
    {
    	synchronized (VirtualPointer.pointerRef)
    	{
    		//Find null sources
    		Enumeration en = VirtualPointer.pointerRef.elements();
    		Vector v = null;
    		while(en.hasMoreElements())
    		{
    			WeakReference ref = (WeakReference)en.nextElement();
    			if(ref != null)
    			{
    				if(ref.get() == null)
    				{
    					ref.clear(); //Just in case
    					if(v == null)
    					{
    						v = new Vector();
    					}
    					v.addElement(ref);
    				}
    			}
    		}
    		//Remove null sources
    		if(v != null)
    		{
    			for(int i = v.size() - 1; i >= 0; i--)
    			{
    				VirtualPointer.pointerRef.remove(v.elementAt(i));
    			}
    		}
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
            	if(this.ref == null)
            	{
            		this.ref = new WeakReference(this);
            	}
                parent.children.removeElement(this.ref);
            }
            parent.free(); //This is because, since the event was removed, this won't get called again by the parent
        }
        if (children != null)
        {
        	synchronized (children)
            {
        		int count = children.size();
        		WeakReference[] tE = new WeakReference[count];
        		children.copyInto(tE); //Free function will remove itself from "events", so copy the list
                for (int i = 0; i < count; i++)
                {
                	VirtualPointer vp = (VirtualPointer)tE[i].get();
                	if(vp != null)
                	{
                		vp.inFree(this);
                	}
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
    //In a debug build, the lenth of the data is stored so it is possible to read the data without specifying a length
    public byte[] readStructuredByteArray(boolean inc, boolean reverse)
    {
        if (dataPos < data.length)
        {
            if (types[dataPos] == Invalid)
            {
                return new byte[0];
            }
            byte[] dat = new byte[this.len[dataPos]];
            readByteArray(dat, 0, dat.length, inc, reverse);
            return dat;
        }
        return null;
    }
//#endif
    
    public int readByteArray(byte[] buffer, int offset, int len, boolean inc, boolean reverse)
    {
        if (buffer == null)
        {
        	/*
        	 * Little insider ability. By default if data read is past the bounds of the allocated memory it returns what it can and fills the rest with random data
        	 * because nobody knows what is outside the allocated data. BUT by passing in a null buffer it will return how much data can actually be read out.
        	 */
            if (data == null || dataPos >= data.length || len < 0)
            {
                return 0;
            }
            else if (len + dataPos > data.length)
            {
                //return Math.min(data.length - dataPos, len); //Wait, why? len is greater so it will just return the other option.
            	return data.length - dataPos;
            }
            return len;
        }
        if (data == null)
        {
        	//Reading from a null pointer? In most modern compilers this would throw an exception. Here we simply return jiberish.
        	readRandom(new Random(), buffer, offset, len);
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
                int count;
                byte[] randData = new byte[(count = len >= 0 ? dif : dif + len)];
                readRandom(new Random(), randData, 0, count);
                if(reverse)
                {
                	//Reverse the desired data, so if data was big-endian then it will be returned little endian and vis-versa
                	int tOff = len >= 0 ? len + offset : offset;
        			for(int i = randData.length - 1, k = 0; i >= 0; i--, k++)
        			{
        				buffer[k + tOff] = randData[i];
        			}
                }
                else
                {
                	System.arraycopy(randData, 0, buffer, len >= 0 ? len + offset : offset, randData.length);
                }
                len += dif;
            }
            else
            {
            	synchronized (data)
                {
            		if(reverse)
            		{
            			for(int i = (dataPos + len) - 1, k = 0; i >= dataPos; i--, k++)
            			{
            				buffer[k + offset] = data[i];
            			}
            		}
            		else
            		{
            			System.arraycopy(data, dataPos, buffer, offset, len);
            		}
                }
            }
            if (inc)
            {
                dataPos += len;
            }
            return len;
        }
    }
    
    //Simple helper function to deal with random data creation.
    private static void readRandom(Random rand, byte[] data, int off, int len)
    {
    	len += off;
    	for(int i = off; i < len; i++)
        {
    		data[i] = (byte)rand.nextInt(256);
        }
    }
    
    private void writeIn(byte[] value,
//#ifdef DEBUG
                byte type,
//#endif
                boolean inc, boolean reverse)
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
        		if(reverse)
        		{
        			int tLen = dataPos + len;
        			for(int i = dataPos, k = len - 1; i < tLen; i++, k--)
        			{
        				data[i] = value[k];
        			}
        		}
        		else
        		{
        			System.arraycopy(value, 0, data, dataPos, len);
        		}
            }
        }
        if (inc)
        {
            dataPos += value.length;
        }
    }
    
    //Read and write directly to the pointer with no safety attached.
    public int readRaw()
    {
    	byte[] buf = new byte[1];
    	readRaw(buf, 0, 1);
    	return buf[0] & 0xFF;
    }
    
    public int readRaw(int directPos)
    {
    	byte[] buf = new byte[1];
    	readRaw(buf, 0, 1, directPos);
    	return buf[0] & 0xFF;
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
    
    public void readRaw(VirtualPointer buffer, int offset, int len)
    {
        readRaw(buffer, offset, len, -1);
    }
    
    public void readRaw(VirtualPointer buffer, int offset, int len, int directPos)
    {
        if (directPos < 0)
        {
            directPos = this.dataPos;
        }
        synchronized (this.data)
        {
        	synchronized (buffer.data)
        	{
        		System.arraycopy(this.data, directPos, buffer.data, offset + buffer.dataPos, len);
        	}
        }
    }
    
    public void writeRaw(int val)
    {
    	writeRaw(new byte[]{(byte)val}, 0, 1);
    }
    
    public void writeRaw(int val, int directPos)
    {
    	writeRaw(new byte[]{(byte)val}, 0, 1, directPos);
    }
    
    public void writeRaw(byte[] buffer, int offset, int len)
    {
    	writeRaw(buffer, offset, len, -1);
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
    
    public void writeRaw(VirtualPointer buffer, int offset, int len)
    {
    	writeRaw(buffer, offset, len, -1);
    }
    
    public void writeRaw(VirtualPointer buffer, int offset, int len, int directPos)
    {
        if (directPos < 0)
        {
            directPos = this.dataPos;
        }
        synchronized (this.data)
        {
        	synchronized (buffer.data)
        	{
        		System.arraycopy(buffer.data, offset + buffer.dataPos, this.data, directPos, len);
        	}
        }
    }
    
    //Cheap memmove within the same virtual pointer, for memmove to/from another buffer then use read/writeRaw.
    public void memmove(int dst, int src, int len)
    {
    	/*
    	byte[] temp = new byte[len];
    	readRaw(temp, src, len);
    	writeRaw(temp, dst, len);
    	*/
    	synchronized (this.data)
        {
        	System.arraycopy(this.data, src + this.dataPos, this.data, dst + this.dataPos, len);
        }
    }
    
    public int getPosition()
    {
        return dataPos;
    }
    
    public void setPosition(int pos)
    {
    	if(pos < 0)
    	{
    		pos = Integer.MAX_VALUE + pos;
    	}
        dataPos = pos;
    }
    
    public void movePosition(int offset)
    {
    	dataPos += offset;
    	if(dataPos < 0)
    	{
    		dataPos = Integer.MAX_VALUE + dataPos;
    	}
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
        	return (Serializer)VirtualPointer.serializers.get(clazz.toString());
        }
    }
    
    public static Serializer setSerializer(Serializer ser, Class clazz)
    {
    	synchronized (VirtualPointer.serializers)
        {
    		if(ser == null)
    		{
    			return (Serializer)VirtualPointer.serializers.remove(clazz.toString());
    		}
    		else
    		{
    			return (Serializer)VirtualPointer.serializers.put(clazz.toString(), ser);
    		}
        }
    }
    
    public String toString()
    {
    	StringBuffer str = new StringBuffer();
    	Utility.sprintf(str, "%p", new Object[]{this});
    	str.deleteCharAt(str.length() - 1);
    	return str.toString();
    }
    
    public int hashCode()
    {
    	int hash = dataPos + (resizeable ? 1 : 0) + (data != null ? ((Object)data).hashCode() : 0);
    	if(children != null)
    	{
    		hash += children.hashCode();
    	}
    	if(parent != null)
    	{
    		hash += parent.hashCode();
    	}
//#ifdef DEBUG
    	hash += ((Object)types).hashCode();
    	hash += ((Object)len).hashCode();
//#endif
    	return hash;
    }
    
    public boolean equals(Object obj)
    {
    	if(obj instanceof VirtualPointer)
    	{
    		if(obj == this)
    		{
    			return true;
    		}
    		VirtualPointer vp = (VirtualPointer)obj;
    		if(vp.dataPos == this.dataPos)
    		{
    			if(vp.resizeable == this.resizeable)
    			{
    				if(Arrays.equals(vp.data, this.data))
    				{
    					/* Could add other types but isn't needed because if data is the same then this is probably a child of the original, which means that the
    					 * types and len will be the same, the children only exist in the parent, and the parent would have to be the same. A child pointer will often
    					 * have a different data position too so it doesn't pay to check parent and children as well as len and types. If that makes sense.
    					 */
    					return true;
    				}
    			}
    		}
    	}
    	return false;
    }
}
