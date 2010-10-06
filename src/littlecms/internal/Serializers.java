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

import java.util.Calendar;

import littlecms.internal.helper.SerializerWrapper;
import littlecms.internal.helper.Utility;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsDateTimeNumber;
import littlecms.internal.lcms2.cmsEncodedXYZNumber;
import littlecms.internal.lcms2.cmsICCHeader;
import littlecms.internal.lcms2.cmsProfileID;
import littlecms.internal.lcms2_internal._cmsICCPROFILE;

/**
 * The serializers that are used
 */
public final class Serializers
{
	//This class is located in the "root" internal namespace instead of the helper namespace so that it can access some internal classes
	
	private static final long SERIALIZER_INITIALIZED_UID = 0x5AED8B01B1866C60L;
	
	static
	{
		if(Utility.singletonStorageGet(SERIALIZER_INITIALIZED_UID) == null)
		{
			//Native
			//TODO Calendar
			//TODO StringBuffer
			
			//LCMS
			//TODO FILENULL
			//TODO FILEMEM
			//TODO cmsIOHANDLER
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, _cmsICCPROFILE.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					_cmsICCPROFILE profile = (_cmsICCPROFILE)val;
					proc.write(profile.Created, true);
					
					proc.write(profile.Version, true);
					proc.write(profile.DeviceClass, true);
					proc.write(profile.ColorSpace, true);
					proc.write(profile.PCS, true);
					proc.write(profile.RenderingIntent, true);
					proc.write(profile.flags, true);
					proc.write(profile.manufacturer, true);
					proc.write(profile.model, true);
					proc.write(profile.attributes, true);
					
					proc.write(profile.ProfileID, true);
					
					proc.write(profile.TagCount, true);
					proc.write(profile.TagNames, true, lcms2_internal.MAX_TABLE_TAG);
					proc.write(profile.TagLinked, true, lcms2_internal.MAX_TABLE_TAG);
					proc.write(profile.TagSizes, true, lcms2_internal.MAX_TABLE_TAG);
					proc.write(profile.TagOffsets, true, lcms2_internal.MAX_TABLE_TAG);
					proc.write(profile.TagSaveAsRaw, true, lcms2_internal.MAX_TABLE_TAG);
					
					proc.write(profile.IsWrite, true);
					return true;
				}
				
				public Object createObject()
				{
					return new _cmsICCPROFILE();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new _cmsICCPROFILE[size];
				}
				
				public int getItemSize()
				{
					return _cmsICCPROFILE.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					VirtualPointer.TypeProcessor type = vp.getProcessor();
					_cmsICCPROFILE profile = (_cmsICCPROFILE)val[index];
					profile.Created = (Calendar)type.readObject(Calendar.class, true, profile.Created);
					
					profile.Version = type.readInt32(true);
					profile.DeviceClass = type.readInt32(true);
					profile.ColorSpace = type.readInt32(true);
					profile.PCS = type.readInt32(true);
					profile.RenderingIntent = type.readInt32(true);
					profile.flags = type.readInt32(true);
					profile.manufacturer = type.readInt32(true);
					profile.model = type.readInt32(true);
					profile.attributes = type.readInt64(true);
					
					profile.ProfileID = (cmsProfileID)type.readObject(cmsProfileID.class, true, profile.ProfileID);
					
					profile.TagCount = type.readInt32(true);
					type.readArray(true, "int", lcms2_internal.MAX_TABLE_TAG, profile.TagNames);
					type.readArray(true, "int", lcms2_internal.MAX_TABLE_TAG, profile.TagLinked);
					type.readArray(true, "int", lcms2_internal.MAX_TABLE_TAG, profile.TagSizes);
					type.readArray(true, "int", lcms2_internal.MAX_TABLE_TAG, profile.TagOffsets);
					type.readArray(true, "boolean", lcms2_internal.MAX_TABLE_TAG, profile.TagSaveAsRaw);
					
					profile.IsWrite = type.readBool(true);
					return true;
				}
			}, _cmsICCPROFILE.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsProfileID.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((cmsProfileID)val).data, true, cmsProfileID.SIZE);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsProfileID();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsProfileID[size];
				}
				
				public int getItemSize()
				{
					return cmsProfileID.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val,int index)
				{
					vp.getProcessor().readArray(true, "byte", cmsProfileID.SIZE, ((cmsProfileID)val[index]).data);
					return true;
				}
			}, cmsProfileID.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsICCHeader.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					cmsICCHeader profile = (cmsICCHeader)val;
					proc.write(profile.size, true);
					proc.write(profile.cmmId, true);
					proc.write(profile.version, true);
					proc.write(profile.deviceClass, true);
					proc.write(profile.colorSpace, true);
					proc.write(profile.pcs, true);
					proc.write(profile.date, true);
					proc.write(profile.magic, true);
					proc.write(profile.platform, true);
					proc.write(profile.flags, true);
					proc.write(profile.manufacturer, true);
					proc.write(profile.model, true);
					proc.write(profile.attributes, true);
					proc.write(profile.renderingIntent, true);
					proc.write(profile.illuminant, true);
					proc.write(profile.creator, true);
					proc.write(profile.profileID, true);
					proc.write(profile.reserved, true, 28);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsICCHeader();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsICCHeader[size];
				}
				
				public int getItemSize()
				{
					return cmsICCHeader.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					VirtualPointer.TypeProcessor type = vp.getProcessor();
					cmsICCHeader profile = (cmsICCHeader)val[index];
					profile.size = type.readInt32(true);
					profile.cmmId = type.readInt32(true);
					profile.version = type.readInt32(true);
					profile.deviceClass = type.readInt32(true);
					profile.colorSpace = type.readInt32(true);
					profile.pcs = type.readInt32(true);
					type.readObject(cmsDateTimeNumber.class, true, profile.date);
					profile.magic = type.readInt32(true);
					profile.platform = type.readInt32(true);
					profile.flags = type.readInt32(true);
					profile.manufacturer = type.readInt32(true);
					profile.model = type.readInt32(true);
					profile.attributes = type.readInt64(true);
					profile.renderingIntent = type.readInt32(true);
					type.readObject(cmsEncodedXYZNumber.class, true, profile.illuminant);
					profile.creator = type.readInt32(true);
					type.readObject(cmsProfileID.class, true, profile.profileID);
					type.readArray(true, "byte", 28, profile.reserved);
					return true;
				}
			}, cmsICCHeader.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsDateTimeNumber.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					cmsDateTimeNumber dateTime = (cmsDateTimeNumber)val;
					proc.write(dateTime.year, true);
					proc.write(dateTime.month, true);
					proc.write(dateTime.day, true);
					proc.write(dateTime.hours, true);
					proc.write(dateTime.minutes, true);
					proc.write(dateTime.seconds, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsDateTimeNumber();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsDateTimeNumber[size];
				}
				
				public int getItemSize()
				{
					return cmsDateTimeNumber.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					VirtualPointer.TypeProcessor type = vp.getProcessor();
					cmsDateTimeNumber dateTime = (cmsDateTimeNumber)val[index];
					dateTime.year = type.readInt16(true);
					dateTime.month = type.readInt16(true);
					dateTime.day = type.readInt16(true);
					dateTime.hours = type.readInt16(true);
					dateTime.minutes = type.readInt16(true);
					dateTime.seconds = type.readInt16(true);
					return true;
				}
			}, cmsDateTimeNumber.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsEncodedXYZNumber.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					cmsEncodedXYZNumber number = (cmsEncodedXYZNumber)val;
					proc.write(number.X, true);
					proc.write(number.Y, true);
					proc.write(number.Z, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsEncodedXYZNumber();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsEncodedXYZNumber[size];
				}
				
				public int getItemSize()
				{
					return cmsEncodedXYZNumber.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					VirtualPointer.TypeProcessor type = vp.getProcessor();
					cmsEncodedXYZNumber number = (cmsEncodedXYZNumber)val[index];
					number.X = type.readInt16(true);
					number.Y = type.readInt16(true);
					number.Z = type.readInt16(true);
					return true;
				}
			}, cmsEncodedXYZNumber.class);
			//TODO Stream.FileStream
			//TODO cmsTagEntry
			//TODO _cmsSubAllocator_chunk
			//TODO _cmsSubAllocator
			//TODO _cmsTagTypeLinkedList
			//TODO cmsTagTypeHandler
			//TODO cmsCIEXYZ
			//TODO cmsCIExyYTRIPLE
			//TODO cmsICCData
			//TODO cmsICCMeasurementConditions
			//TODO cmsMAT3
			//TODO cmsVEC3
			//TODO cmsScreening
			//TODO cmsICCViewingConditions
			//TODO cmsCIELab
			
			Utility.singletonStorageSet(SERIALIZER_INITIALIZED_UID, new Boolean(true));
		}
	}
	
	public static boolean initialize()
	{
		return Utility.singletonStorageGet(SERIALIZER_INITIALIZED_UID) != null; //This is simply to cause the static constructor to get invoked if not already invoked
	}
	
	private static abstract class EvenSimplierSerializer extends SerializerWrapper
	{
		protected EvenSimplierSerializer(boolean canWrap, Class class2match)
		{
			super(canWrap, class2match);
		}
		
		protected EvenSimplierSerializer(boolean canWrap, boolean arraySupport, Class class2match)
		{
			super(canWrap, arraySupport, class2match);
		}
		
		public int getSerializedSize(Object val)
		{
			if(val != null && val.getClass().isArray())
			{
				return -1;
			}
			return getItemSize();
		}
		
		protected abstract int getItemSize();

		public int inDeserialize(VirtualPointer vp, Object[] val)
		{
			if(val.length > 1)
			{
				int elementCount = ((Integer)val[0]).intValue();
				int itemSize = getItemSize();
				if(elementCount < 0)
				{
					elementCount = Math.abs(elementCount) / itemSize;
				}
				((int[])val[1])[0] = elementCount * itemSize;
				Object[] vals;
				if(val[2] == null)
				{
					val[2] = (vals = createObjectArray(elementCount));
				}
				else
				{
					vals = (cmsDateTimeNumber[])val[2];
					elementCount = Math.min(elementCount, vals.length);
				}
				for(int i = 0; i < elementCount; i++)
				{
					if(!readData(vp, vals, i))
					{
						return SerializerWrapper.STATUS_FAIL;
					}
				}
			}
			else
			{
				if(val[0] == null)
				{
					val[0] = createObject();
				}
				if(!readData(vp, val, 0))
				{
					return SerializerWrapper.STATUS_FAIL;
				}
			}
			return SerializerWrapper.STATUS_SUCCESS;
		}
		
		protected abstract Object createObject();
		
		protected abstract Object[] createObjectArray(int size);
		
		protected abstract boolean readData(VirtualPointer vp, Object[] val, int index);
		
		public int inSerialize(VirtualPointer vp, Object val)
		{
			if(val.getClass().isArray())
			{
				//Why no array support? Why is it needed?
				return SerializerWrapper.STATUS_FAIL;
			}
			else
			{
				if(!actualWrite(vp, val))
				{
					return SerializerWrapper.STATUS_FAIL;
				}
			}
			return SerializerWrapper.STATUS_SUCCESS;
		}
		
		protected abstract boolean actualWrite(VirtualPointer vp, Object val);
	}
}
