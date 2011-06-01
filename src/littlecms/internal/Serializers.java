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
import littlecms.internal.helper.VirtualPointer.Serializer;
import littlecms.internal.lcms2.cmsCIELCh;
import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsCIEXYZTRIPLE;
import littlecms.internal.lcms2.cmsCIExyY;
import littlecms.internal.lcms2.cmsCIExyYTRIPLE;
import littlecms.internal.lcms2.cmsDateTimeNumber;
import littlecms.internal.lcms2.cmsEncodedXYZNumber;
import littlecms.internal.lcms2.cmsICCData;
import littlecms.internal.lcms2.cmsICCHeader;
import littlecms.internal.lcms2.cmsICCMeasurementConditions;
import littlecms.internal.lcms2.cmsICCViewingConditions;
import littlecms.internal.lcms2.cmsIOHANDLER;
import littlecms.internal.lcms2.cmsJCh;
import littlecms.internal.lcms2.cmsProfileID;
import littlecms.internal.lcms2.cmsScreening;
import littlecms.internal.lcms2.cmsScreeningChannel;
import littlecms.internal.lcms2.cmsTagEntry;
import littlecms.internal.lcms2_internal._cmsICCPROFILE;
import littlecms.internal.lcms2_internal._cmsSubAllocator;
import littlecms.internal.lcms2_internal._cmsSubAllocator_chunk;
import littlecms.internal.lcms2_plugin.cmsMAT3;
import littlecms.internal.lcms2_plugin.cmsTagTypeHandler;
import littlecms.internal.lcms2_plugin.cmsVEC3;

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
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, Calendar.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					Calendar cal = (Calendar)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(cal.get(Calendar.SECOND), true);
					proc.write(cal.get(Calendar.MINUTE), true);
					proc.write(cal.get(Calendar.HOUR_OF_DAY), true);
					proc.write(cal.get(Calendar.DAY_OF_MONTH), true);
					proc.write(cal.get(Calendar.MONTH), true);
					proc.write(cal.get(Calendar.YEAR), true);
					return true;
				}
				
				public Object createObject()
				{
					return Calendar.getInstance();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new Calendar[size];
				}
				
				public int getItemSize()
				{
					return 4 * 6;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					Calendar cal = (Calendar)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					cal.set(Calendar.SECOND, proc.readInt32(true));
					cal.set(Calendar.MINUTE, proc.readInt32(true));
					cal.set(Calendar.HOUR_OF_DAY, proc.readInt32(true));
					cal.set(Calendar.DAY_OF_MONTH, proc.readInt32(true));
					cal.set(Calendar.MONTH, proc.readInt32(true));
					cal.set(Calendar.YEAR, proc.readInt32(true));
					return true;
				}
			}, Calendar.class);
			VirtualPointer.setSerializer(VirtualPointer.getSerializer(Calendar.class), Calendar.getInstance().getClass()); //Since getInstance doesn't return a java.util.Calendar
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, StringBuffer.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(val.toString(), true);
					return true;
				}
				
				public Object createObject()
				{
					return new StringBuffer();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new StringBuffer[size];
				}
				
				public int getItemSize()
				{
					//Not used
					return 0;
				}
				
				public int getSerializedSize(Object val)
				{
					if(val != null)
					{
						if(val.getClass().isArray())
						{
							return -1;
						}
						return Utility.strlen(val.toString());
					}
					return 0;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					String str = vp.getProcessor().readString(true);
					Utility.strncpy((StringBuffer)val[index], str, Utility.strlen(str));
					return true;
				}
			}, StringBuffer.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, Byte.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((Byte)val).byteValue(), true);
					return true;
				}
				
				public Object createObject()
				{
					return null;
				}
				
				public Object[] createObjectArray(int size)
				{
					return new Byte[size];
				}
				
				public int getItemSize()
				{
					return 1;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					val[index] = new Byte(vp.getProcessor().readInt8(true));
					return true;
				}
			}, Byte.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, Short.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((Short)val).shortValue(), true);
					return true;
				}
				
				public Object createObject()
				{
					return null;
				}
				
				public Object[] createObjectArray(int size)
				{
					return new Short[size];
				}
				
				public int getItemSize()
				{
					return 2;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					val[index] = new Short(vp.getProcessor().readInt16(true));
					return true;
				}
			}, Short.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, Character.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((Character)val).charValue(), true);
					return true;
				}
				
				public Object createObject()
				{
					return null;
				}
				
				public Object[] createObjectArray(int size)
				{
					return new Character[size];
				}
				
				public int getItemSize()
				{
					return 2;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					val[index] = new Character(vp.getProcessor().readChar(true));
					return true;
				}
			}, Character.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, Integer.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((Integer)val).intValue(), true);
					return true;
				}
				
				public Object createObject()
				{
					return null;
				}
				
				public Object[] createObjectArray(int size)
				{
					return new Integer[size];
				}
				
				public int getItemSize()
				{
					return 4;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					val[index] = new Integer(vp.getProcessor().readInt32(true));
					return true;
				}
			}, Integer.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, Long.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((Long)val).longValue(), true);
					return true;
				}
				
				public Object createObject()
				{
					return null;
				}
				
				public Object[] createObjectArray(int size)
				{
					return new Long[size];
				}
				
				public int getItemSize()
				{
					return 8;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					val[index] = new Long(vp.getProcessor().readInt64(true));
					return true;
				}
			}, Long.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, Float.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((Float)val).floatValue(), true);
					return true;
				}
				
				public Object createObject()
				{
					return null;
				}
				
				public Object[] createObjectArray(int size)
				{
					return new Float[size];
				}
				
				public int getItemSize()
				{
					return 4;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					val[index] = new Float(vp.getProcessor().readFloat(true));
					return true;
				}
			}, Float.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, Double.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((Double)val).doubleValue(), true);
					return true;
				}
				
				public Object createObject()
				{
					return null;
				}
				
				public Object[] createObjectArray(int size)
				{
					return new Double[size];
				}
				
				public int getItemSize()
				{
					return 8;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					val[index] = new Double(vp.getProcessor().readDouble(true));
					return true;
				}
			}, Double.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, Boolean.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((Boolean)val).booleanValue(), true);
					return true;
				}
				
				public Object createObject()
				{
					return null;
				}
				
				public Object[] createObjectArray(int size)
				{
					return new Boolean[size];
				}
				
				public int getItemSize()
				{
					return 1;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					val[index] = new Boolean(vp.getProcessor().readBool(true));
					return true;
				}
			}, Boolean.class);
			
			//LCMS
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsio0.FILENULL.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((cmsio0.FILENULL)val).Pointer, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsio0.FILENULL();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsio0.FILENULL[size];
				}
				
				public int getItemSize()
				{
					return cmsio0.FILENULL.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					((cmsio0.FILENULL)val[index]).Pointer = vp.getProcessor().readInt32(true);
					return true;
				}
			}, cmsio0.FILENULL.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsio0.FILEMEM.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsio0.FILEMEM mem = (cmsio0.FILEMEM)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(mem.Block, true);
					proc.write(mem.Size, true);
					proc.write(mem.Pointer, true);
					proc.write(mem.FreeBlockOnClose, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsio0.FILEMEM();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsio0.FILEMEM[size];
				}
				
				public int getItemSize()
				{
					return cmsio0.FILEMEM.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsio0.FILEMEM mem = (cmsio0.FILEMEM)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					mem.Block = proc.readVirtualPointer(true);
					mem.Size = proc.readInt32(true);
					mem.Pointer = proc.readInt32(true);
					mem.FreeBlockOnClose = proc.readBool(true);
					return true;
				}
			}, cmsio0.FILEMEM.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsIOHANDLER.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsIOHANDLER io = (cmsIOHANDLER)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(io.UsedSpace, true);
					proc.write(io.PhysicalFile, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsIOHANDLER();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsIOHANDLER[size];
				}
				
				public int getItemSize()
				{
					return cmsIOHANDLER.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsIOHANDLER io = (cmsIOHANDLER)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					io.UsedSpace = proc.readInt32(true);
					proc.readObject(StringBuffer.class, true, io.PhysicalFile);
					return true;
				}
			}, cmsIOHANDLER.class);
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
					
					type.readObject(cmsProfileID.class, true, profile.ProfileID);
					
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
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsCIEXYZTRIPLE.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					cmsCIEXYZTRIPLE number = (cmsCIEXYZTRIPLE)val;
					Serializer ser = VirtualPointer.getSerializer(cmsCIEXYZ.class);
					proc.write(number.Red, true, ser);
					proc.write(number.Green, true, ser);
					proc.write(number.Blue, true, ser);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsCIEXYZTRIPLE();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsCIEXYZTRIPLE[size];
				}
				
				public int getItemSize()
				{
					return cmsCIEXYZTRIPLE.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					VirtualPointer.TypeProcessor type = vp.getProcessor();
					cmsCIEXYZTRIPLE number = (cmsCIEXYZTRIPLE)val[index];
					Serializer ser = VirtualPointer.getSerializer(cmsCIEXYZ.class);
					type.readObject(ser, true, number.Red);
					type.readObject(ser, true, number.Green);
					type.readObject(ser, true, number.Blue);
					return true;
				}
			}, cmsCIEXYZTRIPLE.class);
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
					number.X = type.readInt32(true);
					number.Y = type.readInt32(true);
					number.Z = type.readInt32(true);
					return true;
				}
			}, cmsEncodedXYZNumber.class);
			//TODO-Maybe? Stream.FileStream
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsTagEntry.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					cmsTagEntry entry = (cmsTagEntry)val;
					proc.write(entry.sig, true);
					proc.write(entry.offset, true);
					proc.write(entry.size, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsTagEntry();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsTagEntry[size];
				}
				
				public int getItemSize()
				{
					return cmsTagEntry.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					cmsTagEntry entry = (cmsTagEntry)val[index];
					entry.sig = proc.readInt32(true);
					entry.offset = proc.readInt32(true);
					entry.size = proc.readInt32(true);
					return true;
				}
			}, cmsTagEntry.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, _cmsSubAllocator_chunk.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					_cmsSubAllocator_chunk alloc = (_cmsSubAllocator_chunk)val;
					proc.write(alloc.Block, true);
					proc.write(alloc.BlockSize, true);
					proc.write(alloc.Used, true);
					//There is no such thing as null so we pretend, "next" is a pointer so if it is null we put an invalid pointer position, else we write the pointer position and the object directly after it
					if(alloc.next == null)
					{
						proc.write(-1, true);
					}
					else
					{
						proc.write(vp.getPosition() + 4, true);
						proc.write(alloc.next);
					}
					return true;
				}
				
				public Object createObject()
				{
					return new _cmsSubAllocator_chunk();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new _cmsSubAllocator_chunk[size];
				}
				
				public int getItemSize()
				{
					return _cmsSubAllocator_chunk.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					_cmsSubAllocator_chunk alloc = (_cmsSubAllocator_chunk)val[index];
					alloc.Block = proc.readVirtualPointer(true);
					alloc.BlockSize = proc.readInt32(true);
					alloc.Used = proc.readInt32(true);
					//When we read in we make sure the pointer is valid, then the object should be directly after it
					if(proc.readInt32(true) >= 0)
					{
						alloc.next = (_cmsSubAllocator_chunk)proc.readObject(_cmsSubAllocator_chunk.class);
					}
					return true;
				}
			}, _cmsSubAllocator_chunk.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, _cmsSubAllocator.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((_cmsSubAllocator)val).h, true);
					return true;
				}
				
				public Object createObject()
				{
					return new _cmsSubAllocator();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new _cmsSubAllocator[size];
				}
				
				public int getItemSize()
				{
					return _cmsSubAllocator.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					((_cmsSubAllocator)val[index]).h = (_cmsSubAllocator_chunk)vp.getProcessor().readObject(_cmsSubAllocator_chunk.class, true);
					return true;
				}
			}, _cmsSubAllocator.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmstypes._cmsTagTypeLinkedList.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					cmstypes._cmsTagTypeLinkedList list = (cmstypes._cmsTagTypeLinkedList)val;
					proc.write(list.Handler, true);
					//There is no such thing as null so we pretend, "next" is a pointer so if it is null we put an invalid pointer position, else we write the pointer position and the object directly after it
					if(list.Next == null)
					{
						proc.write(-1, true);
					}
					else
					{
						proc.write(vp.getPosition() + 4, true);
						proc.write(list.Next);
					}
					return true;
				}
				
				public Object createObject()
				{
					return new cmstypes._cmsTagTypeLinkedList();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmstypes._cmsTagTypeLinkedList[size];
				}
				
				public int getItemSize()
				{
					return cmstypes._cmsTagTypeLinkedList.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					cmstypes._cmsTagTypeLinkedList list = (cmstypes._cmsTagTypeLinkedList)val[index];
					list.Handler = (cmsTagTypeHandler)proc.readObject(cmsTagTypeHandler.class, true);
					//When we read in we make sure the pointer is valid, then the object should be directly after it
					if(proc.readInt32(true) >= 0)
					{
						list.Next = (cmstypes._cmsTagTypeLinkedList)proc.readObject(cmstypes._cmsTagTypeLinkedList.class);
					}
					return true;
				}
			}, cmstypes._cmsTagTypeLinkedList.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsTagTypeHandler.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((cmsTagTypeHandler)val).Signature, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsTagTypeHandler(0, null, null, null, null);
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsTagTypeHandler[size];
				}
				
				public int getItemSize()
				{
					return cmsTagTypeHandler.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					((cmsTagTypeHandler)val[index]).Signature = vp.getProcessor().readInt32(true);
					return true;
				}
			}, cmsTagTypeHandler.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsCIEXYZ.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsCIEXYZ obj = (cmsCIEXYZ)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(obj.X, true);
					proc.write(obj.Y, true);
					proc.write(obj.Z, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsCIEXYZ();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsCIEXYZ[size];
				}
				
				public int getItemSize()
				{
					return cmsCIEXYZ.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsCIEXYZ obj = (cmsCIEXYZ)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					obj.X = proc.readDouble(true);
					obj.Y = proc.readDouble(true);
					obj.Z = proc.readDouble(true);
					return true;
				}
			}, cmsCIEXYZ.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsCIExyY.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsCIExyY obj = (cmsCIExyY)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(obj.x, true);
					proc.write(obj.y, true);
					proc.write(obj.Y, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsCIExyY();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsCIExyY[size];
				}
				
				public int getItemSize()
				{
					return cmsCIExyY.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsCIExyY obj = (cmsCIExyY)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					obj.x = proc.readDouble(true);
					obj.y = proc.readDouble(true);
					obj.Y = proc.readDouble(true);
					return true;
				}
			}, cmsCIExyY.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsCIExyYTRIPLE.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsCIExyYTRIPLE obj = (cmsCIExyYTRIPLE)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					Serializer ser = VirtualPointer.getSerializer(cmsCIExyY.class);
					proc.write(obj.Red, true, ser);
					proc.write(obj.Green, true, ser);
					proc.write(obj.Blue, true, ser);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsCIExyYTRIPLE();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsCIExyYTRIPLE[size];
				}
				
				public int getItemSize()
				{
					return cmsCIExyYTRIPLE.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsCIExyYTRIPLE obj = (cmsCIExyYTRIPLE)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					Serializer ser = VirtualPointer.getSerializer(cmsCIExyY.class);
					proc.readObject(ser, true, obj.Red);
					proc.readObject(ser, true, obj.Green);
					proc.readObject(ser, true, obj.Blue);
					return true;
				}
			}, cmsCIExyYTRIPLE.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsICCData.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsICCData obj = (cmsICCData)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(obj.len, true);
					proc.write(obj.flag, true);
					proc.write(obj.data, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsICCData();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsICCData[size];
				}
				
				public int getItemSize()
				{
					return cmsICCData.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsICCData obj = (cmsICCData)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					obj.len = proc.readInt32(true);
					obj.flag = proc.readInt32(true);
					obj.data = proc.readVirtualPointer(true);
					return true;
				}
			}, cmsICCData.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsICCMeasurementConditions.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsICCMeasurementConditions obj = (cmsICCMeasurementConditions)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(obj.Observer, true);
					proc.write(obj.Backing, true);
					proc.write(obj.Geometry, true);
					proc.write(obj.Flare, true);
					proc.write(obj.IlluminantType, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsICCMeasurementConditions();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsICCMeasurementConditions[size];
				}
				
				public int getItemSize()
				{
					return cmsICCMeasurementConditions.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsICCMeasurementConditions obj = (cmsICCMeasurementConditions)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					obj.Observer = proc.readInt32(true);
					proc.readObject(cmsCIEXYZ.class, true, obj.Backing);
					obj.Geometry = proc.readInt32(true);
					obj.Flare = proc.readDouble(true);
					obj.IlluminantType = proc.readInt32(true);
					return true;
				}
			}, cmsICCMeasurementConditions.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsMAT3.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((cmsMAT3)val).v, true, 3);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsMAT3();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsMAT3[size];
				}
				
				public int getItemSize()
				{
					return cmsMAT3.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					vp.getProcessor().readArray(cmsVEC3.class, true, 3, ((cmsMAT3)val[index]).v);
					return true;
				}
			}, cmsMAT3.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsVEC3.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					vp.getProcessor().write(((cmsVEC3)val).n, true, 3);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsVEC3();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsVEC3[size];
				}
				
				public int getItemSize()
				{
					return cmsVEC3.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					vp.getProcessor().readArray(true, "double", 3, ((cmsVEC3)val[index]).n);
					return true;
				}
			}, cmsVEC3.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsScreening.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsScreening obj = (cmsScreening)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(obj.Flag, true);
					proc.write(obj.nChannels, true);
					proc.write(obj.Channels, true, lcms2.cmsMAXCHANNELS);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsScreening();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsScreening[size];
				}
				
				public int getItemSize()
				{
					return cmsScreening.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsScreening obj = (cmsScreening)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					obj.Flag = proc.readInt32(true);
					obj.nChannels = proc.readInt32(true);
					proc.readArray(cmsScreeningChannel.class, true, lcms2.cmsMAXCHANNELS, obj.Channels);
					return true;
				}
			}, cmsScreening.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsScreeningChannel.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsScreeningChannel obj = (cmsScreeningChannel)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(obj.Frequency, true);
					proc.write(obj.ScreenAngle, true);
					proc.write(obj.SpotShape, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsScreeningChannel();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsScreeningChannel[size];
				}
				
				public int getItemSize()
				{
					return cmsScreeningChannel.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsScreeningChannel obj = (cmsScreeningChannel)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					obj.Frequency = proc.readDouble(true);
					obj.ScreenAngle = proc.readDouble(true);
					obj.SpotShape = proc.readInt32(true);
					return true;
				}
			}, cmsScreeningChannel.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsICCViewingConditions.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsICCViewingConditions obj = (cmsICCViewingConditions)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					Serializer ser = VirtualPointer.getSerializer(cmsCIEXYZ.class);
					proc.write(obj.IlluminantXYZ, true, ser);
					proc.write(obj.SurroundXYZ, true, ser);
					proc.write(obj.IlluminantType, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsICCViewingConditions();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsICCViewingConditions[size];
				}
				
				public int getItemSize()
				{
					return cmsICCViewingConditions.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsICCViewingConditions obj = (cmsICCViewingConditions)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					Serializer ser = VirtualPointer.getSerializer(cmsCIEXYZ.class);
					proc.readObject(ser, true, obj.IlluminantXYZ);
					proc.readObject(ser, true, obj.SurroundXYZ);
					obj.IlluminantType = proc.readInt32(true);
					return true;
				}
			}, cmsICCViewingConditions.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsCIELab.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsCIELab obj = (cmsCIELab)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(obj.L, true);
					proc.write(obj.a, true);
					proc.write(obj.b, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsCIELab();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsCIELab[size];
				}
				
				public int getItemSize()
				{
					return cmsCIELab.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsCIELab obj = (cmsCIELab)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					obj.L = proc.readDouble(true);
					obj.a = proc.readDouble(true);
					obj.b = proc.readDouble(true);
					return true;
				}
			}, cmsCIELab.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsCIELCh.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsCIELCh obj = (cmsCIELCh)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(obj.L, true);
					proc.write(obj.C, true);
					proc.write(obj.h, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsCIELCh();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsCIELCh[size];
				}
				
				public int getItemSize()
				{
					return cmsCIELCh.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsCIELCh obj = (cmsCIELCh)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					obj.L = proc.readDouble(true);
					obj.C = proc.readDouble(true);
					obj.h = proc.readDouble(true);
					return true;
				}
			}, cmsCIELCh.class);
			VirtualPointer.setSerializer(new EvenSimplierSerializer(false, cmsJCh.class)
			{
				public boolean actualWrite(VirtualPointer vp, Object val)
				{
					cmsJCh obj = (cmsJCh)val;
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					proc.write(obj.J, true);
					proc.write(obj.C, true);
					proc.write(obj.h, true);
					return true;
				}
				
				public Object createObject()
				{
					return new cmsJCh();
				}
				
				public Object[] createObjectArray(int size)
				{
					return new cmsJCh[size];
				}
				
				public int getItemSize()
				{
					return cmsJCh.SIZE;
				}
				
				public boolean readData(VirtualPointer vp, Object[] val, int index)
				{
					cmsJCh obj = (cmsJCh)val[index];
					VirtualPointer.TypeProcessor proc = vp.getProcessor();
					obj.J = proc.readDouble(true);
					obj.C = proc.readDouble(true);
					obj.h = proc.readDouble(true);
					return true;
				}
			}, cmsJCh.class);
			
			Utility.singletonStorageSet(SERIALIZER_INITIALIZED_UID, new Boolean(true));
		}
	}
	
	public static boolean initialize()
	{
		return Utility.singletonStorageGet(SERIALIZER_INITIALIZED_UID) != null; //This is simply to cause the static constructor to get invoked if not already invoked
	}
	
	public static abstract class EvenSimplierSerializer extends SerializerWrapper
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
					vals = (Object[])val[2];
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
