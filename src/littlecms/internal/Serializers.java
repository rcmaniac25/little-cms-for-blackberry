//---------------------------------------------------------------------------------
//
//  Little Color Management System
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
package littlecms.internal;

import littlecms.internal.cmsio0.FILEMEM;
import littlecms.internal.cmsio0.FILENULL;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.helper.VirtualPointer.Serializer;
import littlecms.internal.lcms2.cmsIOHANDLER;
import littlecms.internal.lcms2_internal._cmsICCPROFILE;

/**
 * The serializers that are used
 */
public final class Serializers
{
	//This class is located in the "root" internal namespace instead of the helper namespace so that it can access some internal classes
	
	private static boolean init;
	
	static
	{
		init = false;
		//XXX TEMP
		VirtualPointer.setSerializer(new SerializerWrapper()
		{
			//FILENULL
			public int inSerialize(byte[] data, int offset, Object val)
			{
				//TODO
				return VirtualPointer.Serializer.STATUS_SUCCESS;
			}
			
			public int getSerializedSize(Object val)
			{
				return FILENULL.SIZE;
			}
			
			public int inDeserialize(byte[] data, int offset, Object[] val)
			{
				if(val[0] == null)
				{
					val[0] = new FILENULL();
				}
				//TODO
				return VirtualPointer.Serializer.STATUS_SUCCESS;
			}
		}, FILENULL.class);
		VirtualPointer.setSerializer(new SerializerWrapper()
		{
			//FILEMEM
			public int inSerialize(byte[] data, int offset, Object val)
			{
				//TODO
				return VirtualPointer.Serializer.STATUS_SUCCESS;
			}
			
			public int getSerializedSize(Object val)
			{
				return FILEMEM.SIZE;
			}
			
			public int inDeserialize(byte[] data, int offset, Object[] val)
			{
				if(val[0] == null)
				{
					val[0] = new FILEMEM();
				}
				//TODO
				return VirtualPointer.Serializer.STATUS_SUCCESS;
			}
		}, FILEMEM.class);
		VirtualPointer.setSerializer(new SerializerWrapper()
		{
			//cmsIOHANDLER, not exactly possible because of the callbacks.
			public int inSerialize(byte[] data, int offset, Object val)
			{
				cmsIOHANDLER io = (cmsIOHANDLER)val;
				Serializer ser = VirtualPointer.getSerializer(io.stream.getClass());
				if(ser != null)
				{
					int status;
					switch(status = ser.serialize(data, offset, io.stream))
					{
						case VirtualPointer.Serializer.STATUS_FAIL:
						case VirtualPointer.Serializer.STATUS_NEED_MORE_DATA:
							return status;
						case VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT:
							break; //Ignore stream serialization
						case VirtualPointer.Serializer.STATUS_SUCCESS:
							offset += ser.getSerializedSize(io.stream);
							break;
					}
				}
				//TODO
				return VirtualPointer.Serializer.STATUS_SUCCESS;
			}
			
			public int getSerializedSize(Object val)
			{
				if(val != null)
				{
					cmsIOHANDLER io = (cmsIOHANDLER)val;
					Serializer ser = VirtualPointer.getSerializer(io.stream.getClass());
					if(ser != null)
					{
						return ser.getSerializedSize(io.stream) + cmsIOHANDLER.SIZE;
					}
				}
				return cmsIOHANDLER.SIZE;
			}
			
			public int inDeserialize(byte[] data, int offset, Object[] val)
			{
				cmsIOHANDLER io;
				if(val[0] == null)
				{
					val[0] = io = new cmsIOHANDLER();
				}
				else
				{
					io = (cmsIOHANDLER)val[0];
				}
				Serializer streamSer = null;
				switch(data.length)
				{
					case cmsIOHANDLER.SIZE + FILENULL.SIZE:
						streamSer = VirtualPointer.getSerializer(FILENULL.class);
						break;
					case cmsIOHANDLER.SIZE + FILEMEM.SIZE:
						streamSer = VirtualPointer.getSerializer(FILEMEM.class);
						break;
					//Can't do Stream, well maybe.
				}
				if(streamSer != null)
				{
					Object[] obj = new Object[1];
					switch(streamSer.deserialize(data, offset, obj))
					{
						case VirtualPointer.Serializer.STATUS_FAIL:
						case VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT:
							return VirtualPointer.Serializer.STATUS_FAIL;
						case VirtualPointer.Serializer.STATUS_NEED_MORE_DATA:
							return VirtualPointer.Serializer.STATUS_NEED_MORE_DATA;
					}
					io.stream = obj[0];
					offset += streamSer.getSerializedSize(obj[0]);
				}
				//TODO
				return VirtualPointer.Serializer.STATUS_SUCCESS;
			}
		}, cmsIOHANDLER.class);
		VirtualPointer.setSerializer(new SerializerWrapper()
		{
			//_cmsICCPROFILE
			public int inSerialize(byte[] data, int offset, Object val)
			{
				//TODO
				return VirtualPointer.Serializer.STATUS_SUCCESS;
			}
			
			public int getSerializedSize(Object val)
			{
				return _cmsICCPROFILE.SIZE;
			}
			
			public int inDeserialize(byte[] data, int offset, Object[] val)
			{
				if(val[0] == null)
				{
					val[0] = new cmsIOHANDLER();
				}
				//TODO
				return VirtualPointer.Serializer.STATUS_SUCCESS;
			}
		}, _cmsICCPROFILE.class);
		//XXX TEMP
		
		//TODO FILENULL
		//TODO FILEMEM
		//TODO cmsIOHANDLER
		//TODO _cmsICCPROFILE
		//TODO Calendar
		//TODO cmsProfileID
		//TODO StringBuffer
		//TODO cmsICCHeader
		//TODO cmsDateTimeNumber
		//TODO cmsEncodedXYZNumber
		//TODO Stream.FileStream
		//TODO cmsTagEntry
		//TODO _cmsSubAllocator_chunk
		//TODO _cmsSubAllocator
		//TODO _cmsTagTypeLinkedList
		//TODO cmsTagTypeHandler
		
		init = true;
	}
	
	public static abstract class SerializerWrapper implements VirtualPointer.Serializer
	{
		public int serialize(byte[] data, int offset, Object val)
		{
			if(data.length < getSerializedSize(val) + offset)
			{
				return VirtualPointer.Serializer.STATUS_NEED_MORE_DATA;
			}
			if(val == null)
			{
				return VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT;
			}
			return inSerialize(data, offset, val);
		}
		
		public int deserialize(byte[] data, int offset, Object[] val)
		{
			if(val == null || val.length < 1)
			{
				return VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT;
			}
			if(data.length < getSerializedSize(val[0]) + offset)
			{
				return VirtualPointer.Serializer.STATUS_NEED_MORE_DATA;
			}
			return inDeserialize(data, offset, val);
		}
		
		public abstract int inSerialize(byte[] data, int offset, Object val);
		
		public abstract int inDeserialize(byte[] data, int offset, Object[] val);
	}
	
	public static boolean initialize()
	{
		return init;
	}
}
