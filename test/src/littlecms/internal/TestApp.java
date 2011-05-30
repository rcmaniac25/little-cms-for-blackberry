//#preprocessor

/* 
 * TestApp.java
 * 
 * © Rebuild, 2004-2010
 * Confidential and proprietary
 */
package littlecms.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import littlecms.internal.helper.Stream;
import littlecms.internal.helper.Utility;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsCIELCh;
import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsCIEXYZTRIPLE;
import littlecms.internal.lcms2.cmsCIExyY;
import littlecms.internal.lcms2.cmsCIExyYTRIPLE;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHANDLE;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsHTRANSFORM;
import littlecms.internal.lcms2.cmsICCData;
import littlecms.internal.lcms2.cmsICCMeasurementConditions;
import littlecms.internal.lcms2.cmsICCViewingConditions;
import littlecms.internal.lcms2.cmsLogErrorHandlerFunction;
import littlecms.internal.lcms2.cmsMLU;
import littlecms.internal.lcms2.cmsNAMEDCOLORLIST;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsSAMPLER16;
import littlecms.internal.lcms2.cmsSEQ;
import littlecms.internal.lcms2.cmsScreening;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2.cmsUcrBg;
import littlecms.internal.lcms2_internal._cmsTRANSFORM;
import littlecms.internal.lcms2_plugin.cmsFormatter;
import littlecms.internal.lcms2_plugin.cmsInterpParams;
import littlecms.internal.lcms2_plugin.cmsPluginMemHandler;
import littlecms.internal.lcms2_plugin.cmsPluginMemHandler.pluginFreePtr;
import littlecms.internal.lcms2_plugin.cmsPluginMemHandler.pluginMallocPtr;
import littlecms.internal.lcms2_plugin.cmsPluginMemHandler.pluginReallocPtr;

//#ifndef BlackBerrySDK4.5.0
import net.rim.device.api.util.MathUtilities;
//#endif
//#ifndef BlackBerrySDK4.6.1 | BlackBerrySDK4.6.0 | BlackBerrySDK4.5.0 | BlackBerrySDK4.2.1 | BlackBerrySDK4.2.0
import net.rim.device.api.ui.TouchEvent;
//#endif
import net.rim.device.api.util.Arrays;
import net.rim.device.api.system.Characters;
import net.rim.device.api.i18n.DateFormat;
import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.ObjectListField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

public final class TestApp extends UiApplication
{
	private static final String FILE_PREFIX = "file:///store/appdata/";
	//private static final String FILE_PREFIX = "file:///SDCard/BlackBerry/documents/";
	
	private static final String RES_PREFIX = "/littlecms/internal/res/";
	
	public static void main(String[] args)
	{
		TestApp.app = new TestApp();
		TestApp.app.runScreenStuff();
		TestApp.app.enterEventDispatcher();
	}
	
	private void runScreenStuff()
	{
		TestApp.screen = new TestScreen();
		this.pushScreen(TestApp.screen);
	}
	
	private static class ListOut extends OutputStream
	{
		//private StringBuffer buf;
		private int len;
		private ObjectListField list;
		
		public ListOut(ObjectListField list)
		{
			this.list = list;
			this.len = 0;
			//this.buf = new StringBuffer();
		}
		
		public void write(int b) throws IOException
		{
			write(new byte[]{(byte)b});
		}
		
		/*
		public void write(byte[] b, int off, int len) throws IOException
		{
			for(int i = off; i < len; i++)
			{
				if(b[i] != '\n')
				{
					buf.append((char)b[i]);
				}
				else
				{
					final ObjectListField listF = this.list;
					if(buf.length() > 1)
					{
						final String bufF = buf.toString();
						TestApp.app.invokeLater(new Runnable()
						{
							public void run()
							{
								listF.insert(0, bufF);
							}
						});
						buf.setLength(0);
					}
					else
					{
						TestApp.app.invokeLater(new Runnable()
						{
							public void run()
							{
								listF.insert(0);
							}
						});
					}
				}
			}
		}
		*/
		
		public void write(byte[] b, int off, int len) throws IOException
		{
			final ObjectListField listF = this.list;
			StringBuffer buf = new StringBuffer();
			int tLen = 0;
			for(int i = off; i < len; i++)
			{
				switch(b[i])
				{
					case '\n':
						if(buf.length() > 0)
						{
							final String bufF = buf.toString();
							this.len += tLen;
							buf.setLength(0);
							if(this.len != tLen)
							{
								TestApp.app.invokeAndWait(new Runnable()
								{
									public void run()
									{
										listF.delete(0);
										listF.insert(0, bufF);
									}
								});
							}
							else
							{
								TestApp.app.invokeAndWait(new Runnable()
								{
									public void run()
									{
										listF.insert(0, bufF);
									}
								});
							}
						}
						if(this.len > 0)
						{
							this.len = 0;
							tLen = 0;
						}
						else
						{
							TestApp.app.invokeAndWait(new Runnable()
							{
								public void run()
								{
									listF.insert(0);
								}
							});
						}
						break;
					case '\r':
						if(this.len > 0)
						{
							/*
							TestApp.app.invokeAndWait(new Runnable()
							{
								public void run()
								{
									listF.delete(0);
								}
							});
							*/
							//TODO
						}
						break;
					default:
						if(this.len > 0)
						{
							buf.append(listF.get(listF, 0));
						}
						buf.append((char)b[i]);
						tLen++;
						break;
				}
			}
			if(buf.length() > 0)
			{
				final String bufF = buf.toString();
				this.len += tLen;
				if(this.len != tLen)
				{
					TestApp.app.invokeAndWait(new Runnable()
					{
						public void run()
						{
							listF.delete(0);
							listF.insert(0, bufF);
						}
					});
				}
				else
				{
					TestApp.app.invokeAndWait(new Runnable()
					{
						public void run()
						{
							listF.insert(0, bufF);
						}
					});
				}
			}
		}
		
		public void close() throws IOException
		{
			//this.buf = null;
			super.close();
		}
	}
	
	//Now for the ported code
	
	// A single check. Returns 1 if success, 0 if failed
	private static interface TestFn
	{
		public int run();
	}
	
	// A parametric Tone curve test function
	private static interface dblfnptr
	{
		public float run(float x, final double[] Params);
	}
	
	// Some globals to keep track of error
	private static final int TEXT_ERROR_BUFFER_SIZE = 4096;
	
	private static char[] ReasonToFailBuffer = new char[TEXT_ERROR_BUFFER_SIZE];
	private static char[] SubTestBuffer = new char[TEXT_ERROR_BUFFER_SIZE];
	private static int TotalTests = 0, TotalFail = 0;
	private static boolean TrappedError;
	private static int SimultaneousErrors;
	private static PrintStream print;
	private static TestScreen screen;
	private static TestApp app;
	
	// Die, a fatal unexpected error is detected!
	private static void Die(final String Reason)
	{
		Utility.fprintf(print, "\n\nArrrgggg!!: %s!\n\n", new Object[]{Reason});
		print.flush();
	    //System.exit(1);
		TestApp.app.invokeLater(new Runnable()
		{
			public void run()
			{
				screen.close(); //Do this instead of System.exit(1);
			}
		});
	}
	
	// Memory management replacement -----------------------------------------------------------------------------
	
	// This is just a simple plug-in for malloc, free and realloc to keep track of memory allocated,
	// maximum requested as a single block and maximum allocated at a given time. Results are printed at the end
	private static int SingleHit, MaxAllocated=0, TotalMemory=0;
	
	// I'm hidding the size before the block. This is a well-known technique and probably the blocks coming from
	// malloc are built in a way similar to that, but I do on my own to be portable.
	private static class _cmsMemoryBlock
	{
		public static int SIZE_OF_MEM_HEADER = (4 * 2) + 4;
		
		//Not really used but here anyway
		public int KeepSize;
		public int Align8;
		public cmsContext WhoAllocated; // Some systems do need pointers aligned to 8-byte boundaries.
	}
	
	// This is a fake thread descriptor used to check thread integrity. 
	// Basically it returns a different threadID each time it is called.
	// Then the memory management replacement functions does check if each
	// free() is being called with same ContextID used on malloc()
	private static int n = 1;
	
	private static class cmsTestContext implements cmsContext
	{
		public int value;
		
		public cmsTestContext(int value)
		{
			this.value = value;
		}
		
		public String toString()
		{
			StringBuffer buf = new StringBuffer();
			Utility.sprintf(buf, "%#.8x", new Object[]{new Integer(this.value)});
			buf.deleteCharAt(buf.length() - 1);
			return buf.toString();
		}
	}
	
	private static cmsContext DbgThread()
	{
		return new cmsTestContext(n++);
	}
	
	// The allocate routine
	private static final pluginMallocPtr DebugMalloc = new pluginMallocPtr()
	{
		public VirtualPointer run(cmsContext ContextID, int size)
		{
			if (size <= 0)
			{
		       Die("malloc requested with zero bytes");
		    }
			
		    TotalMemory += size;
		    
		    if (TotalMemory > MaxAllocated)
		    {
		    	MaxAllocated = TotalMemory;
		    }
		    
		    if (size > SingleHit)
		    {
		    	SingleHit = size;
		    }
		    
		    VirtualPointer blk = new VirtualPointer(size + _cmsMemoryBlock.SIZE_OF_MEM_HEADER);
		    VirtualPointer.TypeProcessor proc = blk.getProcessor();
		    
		    proc.write(size, true);
		    proc.write(0, true);
		    if(ContextID == null)
		    {
		    	ContextID = new cmsTestContext(0);
		    }
		    proc.write(((cmsTestContext)ContextID).value, true);
		    
		    return blk;
		}
	};
	
	// The free routine
	private static final pluginFreePtr DebugFree = new pluginFreePtr()
	{
		public void run(cmsContext ContextID, VirtualPointer Ptr)
		{
			VirtualPointer blk;
		    
		    if (Ptr == null)
		    {
		        Die("NULL free (which is a no-op in C, but may be an clue of something going wrong)");
		    }
		    
		    blk = new VirtualPointer(Ptr);
		    blk.setPosition(0);
		    VirtualPointer.TypeProcessor proc = blk.getProcessor();
		    TotalMemory -= proc.readInt32(true);
		    
		    proc.readInt32(true);
		    
		    if(ContextID == null)
		    {
		    	ContextID = new cmsTestContext(0);
		    }
		    if (proc.readInt32(true) != ((cmsTestContext)ContextID).value)
		    {
		        Die("Trying to free memory allocated by a different thread");
		    }
		    
		    blk.free();
		}
	};
	
	// Reallocate, just a malloc, a copy and a free in this case.
	private static final pluginReallocPtr DebugRealloc = new pluginReallocPtr()
	{
		public VirtualPointer run(cmsContext ContextID, VirtualPointer Ptr, int NewSize)
		{
			VirtualPointer blk;
			VirtualPointer NewPtr;
		    int max_sz;
		    
		    if(ContextID == null)
		    {
		    	ContextID = new cmsTestContext(0);
		    }
		    NewPtr = DebugMalloc.run(ContextID, NewSize);
		    if (Ptr == null)
		    {
		    	return NewPtr;
		    }
		    
		    blk = new VirtualPointer(Ptr);
		    blk.movePosition(-_cmsMemoryBlock.SIZE_OF_MEM_HEADER);
		    VirtualPointer.TypeProcessor proc = blk.getProcessor();
		    max_sz = proc.readInt32() > NewSize ? NewSize : proc.readInt32();
		    NewPtr.writeRaw(Ptr, 0, max_sz);
		    DebugFree.run(ContextID, Ptr);
		    
		    return NewPtr;
		}
	};
	
	// Let's know the totals
	private static void DebugMemPrintTotals()
	{
	    Utility.fprintf(print, "[Memory statistics]\n", null);
	    Utility.fprintf(print, "Allocated = %d MaxAlloc = %d Single block hit = %d\n", new Object[]{new Integer(TotalMemory), new Integer(MaxAllocated), new Integer(SingleHit)});
	}
	
	// Here we go with the plug-in declaration
	private static final cmsPluginMemHandler DebugMemHandler;
	
	// Utils  -------------------------------------------------------------------------------------
	
	private static final cmsLogErrorHandlerFunction FatalErrorQuit = new cmsLogErrorHandlerFunction()
	{
		public void run(cmsContext ContextID, int ErrorCode, String Text)
		{
			Die(Text);
		}
	};
	
	// Print a dot for gauging
	private static void Dot()
	{
		Utility.fprintf(System.out, ".", null);
		print.flush();
	}
	
	// Keep track of the reason to fail
	private static void Fail(final String frm, Object[] args)
	{
	    Utility.vsprintf(ReasonToFailBuffer, frm, args);
	}
	
	// Keep track of subtest
	private static void SubTest(final String frm, Object[] args)
	{
	    Dot();
	    Utility.vsprintf(SubTestBuffer, frm, args);
	}
	
	// Memory string
	private static String MemStr(int size)
	{
		StringBuffer Buffer = new StringBuffer(1024);
		
	    if (size > 1024*1024)
	    {
	        Utility.sprintf(Buffer, "%g Mb", new Object[]{new Double(size / (1024.0*1024.0))});
	    }
	    else
	    {
	        if (size > 1024)
	        {
	        	Utility.sprintf(Buffer, "%g Kb", new Object[]{new Double(size / 1024.0)});
	        }
	        else
	        {
	        	Utility.sprintf(Buffer, "%g bytes", new Object[]{new Double(size)});
	        }
	    }
	    return Buffer.toString();
	}
	
	// The check framework
	private static void Check(final String Title, TestFn Fn)
	{
	    Utility.fprintf(print, "Checking %s ...", new Object[]{Title});
	    print.flush();
	    
	    ReasonToFailBuffer[0] = 0;
	    SubTestBuffer[0] = 0;
	    TrappedError = false;
	    SimultaneousErrors = 0;
	    TotalTests++;
	    
	    if (Fn.run() != 0 && !TrappedError)
	    {
	        // It is a good place to check memory
	        if (TotalMemory > 0)
	        {
	        	Utility.fprintf(print, "Ok, but %s are left!\n", new Object[]{MemStr(TotalMemory)});
	        }
	        else
	        {
	        	Utility.fprintf(print, "Ok.\n", null);
	        }
	    }
	    else
	    {
	    	Utility.fprintf(print, "FAIL!\n", null);
	        
	        if (SubTestBuffer[0] != 0)
	        {
	        	Utility.fprintf(print, "%s: [%s]\n\t%s\n", new Object[]{Title, SubTestBuffer, ReasonToFailBuffer});
	        }
	        else
	        {
	        	Utility.fprintf(print, "%s:\n\t%s\n", new Object[]{Title, ReasonToFailBuffer});
	        }
	        
	        if (SimultaneousErrors > 1)
	        {
	        	Utility.fprintf(print, "\tMore than one (%d) errors were reported\n", new Object[]{new Integer(SimultaneousErrors)}); 
	        }
	        
	        TotalFail++;
	    }   
	    print.flush();
	}
	
	// Dump a tone curve, for easy diagnostic
	private static void DumpToneCurve(cmsToneCurve gamma, final String FileName)
	{
	    cmsHANDLE hIT8;
	    int i;
	    
	    hIT8 = lcms2.cmsIT8Alloc(gamma.InterpParams.ContextID);
	    
	    lcms2.cmsIT8SetPropertyDbl(hIT8, "NUMBER_OF_FIELDS", 2);
	    lcms2.cmsIT8SetPropertyDbl(hIT8, "NUMBER_OF_SETS", gamma.nEntries);
	    
	    lcms2.cmsIT8SetDataFormat(hIT8, 0, "SAMPLE_ID");
	    lcms2.cmsIT8SetDataFormat(hIT8, 1, "VALUE");
	    
	    for (i=0; i < gamma.nEntries; i++)
	    {
	    	StringBuffer Val = new StringBuffer(30);
	        
	        Utility.sprintf(Val, "%d", new Object[]{new Integer(i)});
	        lcms2.cmsIT8SetDataRowCol(hIT8, i, 0, Val.toString());
	        Val.setLength(0);
	        Utility.sprintf(Val, "0x%x", new Object[]{new Short(gamma.Table16[i])});
	        lcms2.cmsIT8SetDataRowCol(hIT8, i, 1, Val.toString());
	    }
	    
	    lcms2.cmsIT8SaveToFile(hIT8, FileName);
	    lcms2.cmsIT8Free(hIT8);
	}
	
	// -------------------------------------------------------------------------------------------------


	// Used to perform several checks. 
	// The space used is a clone of a well-known commercial 
	// color space which I will name "Above RGB"
	private static cmsHPROFILE Create_AboveRGB()
	{
	    cmsToneCurve[] Curve = new cmsToneCurve[3];
	    cmsHPROFILE hProfile;
	    cmsCIExyY D65 = new cmsCIExyY();
	    cmsCIExyYTRIPLE Primaries = new cmsCIExyYTRIPLE(new double[]{
	    		0.64, 0.33, 1,
	    		0.21, 0.71, 1,
	    		0.15, 0.06, 1
	    });
	    
	    Curve[0] = Curve[1] = Curve[2] = lcms2.cmsBuildGamma(DbgThread(), 2.19921875);
	    
	    lcms2.cmsWhitePointFromTemp(D65, 6504);
	    hProfile = lcms2.cmsCreateRGBProfileTHR(DbgThread(), D65, Primaries, Curve);
	    lcms2.cmsFreeToneCurve(Curve[0]);
	    
	    return hProfile;
	}
	
	// A gamma-2.2 gray space
	private static cmsHPROFILE Create_Gray22()
	{
	    cmsHPROFILE hProfile;
	    cmsToneCurve Curve = lcms2.cmsBuildGamma(DbgThread(), 2.2);
	    if (Curve == null)
	    {
	    	return null;
	    }
	    
	    hProfile = lcms2.cmsCreateGrayProfileTHR(DbgThread(), lcms2.cmsD50_xyY, Curve);
	    lcms2.cmsFreeToneCurve(Curve);
	    
	    return hProfile;
	}
	
	private static cmsHPROFILE Create_GrayLab()
	{
	    cmsHPROFILE hProfile;
	    cmsToneCurve Curve = lcms2.cmsBuildGamma(DbgThread(), 1.0);
	    if (Curve == null)
	    {
	    	return null;
	    }
	    
	    hProfile = lcms2.cmsCreateGrayProfileTHR(DbgThread(), lcms2.cmsD50_xyY, Curve);
	    lcms2.cmsFreeToneCurve(Curve);
	    
	    lcms2.cmsSetPCS(hProfile, lcms2.cmsSigLabData);
	    return hProfile;
	}
	
	// A CMYK devicelink that adds gamma 3.0 to each channel
	private static cmsHPROFILE Create_CMYK_DeviceLink()
	{
	    cmsHPROFILE hProfile;
	    cmsToneCurve[] Tab = new cmsToneCurve[4];
	    cmsToneCurve Curve = lcms2.cmsBuildGamma(DbgThread(), 3.0);
	    if (Curve == null)
	    {
	    	return null;
	    }
	    
	    Tab[0] = Curve;
	    Tab[1] = Curve;
	    Tab[2] = Curve;
	    Tab[3] = Curve;
	    
	    hProfile = lcms2.cmsCreateLinearizationDeviceLinkTHR(DbgThread(), lcms2.cmsSigCmykData, Tab);
	    if (hProfile == null)
	    {
	    	return null;
	    }
	    
	    lcms2.cmsFreeToneCurve(Curve);
	    
	    return hProfile;
	}
	
	// Create a fake CMYK profile, without any other requeriment that being coarse CMYK. 
	// DONT USE THIS PROFILE FOR ANYTHING, IT IS USELESS BUT FOR TESTING PURPOSES.
	private static class FakeCMYKParams
	{
		public cmsHTRANSFORM hLab2sRGB;
		public cmsHTRANSFORM sRGB2Lab;
		public cmsHTRANSFORM hIlimit;
	}
	
	private static double Clip(double v)
	{
	    if (v < 0)
	    {
	    	return 0;
	    }
	    if (v > 1)
	    {
	    	return 1;
	    }
	    
	    return v;
	}
	
	private static final cmsSAMPLER16 ForwardSampler = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			FakeCMYKParams p = (FakeCMYKParams)Cargo;
		    double[] rgb = new double[3], cmyk = new double[4];
		    double c, m, y, k;
		    
		    lcms2.cmsDoTransform(p.hLab2sRGB, In, rgb, 1);
		    
		    c = 1 - rgb[0];
		    m = 1 - rgb[1];
		    y = 1 - rgb[2];
		    
		    k = (c < m ? Math.min(c, y) : Math.min(m, y));
		    
		    // NONSENSE WARNING!: I'm doing this just because this is a test 
		    // profile that may have ink limit up to 400%. There is no UCR here
		    // so the profile is basically useless for anything but testing.
		    
		    cmyk[0] = c;
		    cmyk[1] = m;
		    cmyk[2] = y;
		    cmyk[3] = k;
		    
		    lcms2.cmsDoTransform(p.hIlimit, cmyk, Out, 1);
		    
		    return 1;
		}
	};
	
	private static final cmsSAMPLER16 ReverseSampler = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			FakeCMYKParams p = (FakeCMYKParams)Cargo;
		    double c, m, y, k;
		    double[] rgb = new double[3];
		    
		    c = (In[0] & 0xFFFF) / 65535.0;
		    m = (In[1] & 0xFFFF) / 65535.0;
		    y = (In[2] & 0xFFFF) / 65535.0;
		    k = (In[3] & 0xFFFF) / 65535.0;
		    
		    if (k == 0)
		    {
		        rgb[0] = Clip(1 - c);
		        rgb[1] = Clip(1 - m);
		        rgb[2] = Clip(1 - y);
		    }
		    else
		    {
		        if (k == 1)
		        {
		            rgb[0] = rgb[1] = rgb[2] = 0;
		        }
		        else
		        {
		            rgb[0] = Clip((1 - c) * (1 - k));
		            rgb[1] = Clip((1 - m) * (1 - k));
		            rgb[2] = Clip((1 - y) * (1 - k));       
		        }
		    }
		    
		    lcms2.cmsDoTransform(p.sRGB2Lab, rgb, Out, 1);
		    return 1;
		}
	};
	
	private static cmsHPROFILE CreateFakeCMYK(double InkLimit, boolean lUseAboveRGB)
	{
	    cmsHPROFILE hICC;
	    cmsPipeline AToB0, BToA0;
	    cmsStage CLUT;
	    cmsContext ContextID;
	    FakeCMYKParams p = new FakeCMYKParams();
	    cmsHPROFILE hLab, hsRGB, hLimit;
	    int cmykfrm;
	    
	    if (lUseAboveRGB)
	    {
	    	hsRGB = Create_AboveRGB();
	    }
	    else
	    {
	    	hsRGB = lcms2.cmsCreate_sRGBProfile();
	    }
	    
	    hLab   = lcms2.cmsCreateLab4Profile(null);
	    hLimit = lcms2.cmsCreateInkLimitingDeviceLink(lcms2.cmsSigCmykData, InkLimit);
	    
	    cmykfrm = lcms2.FLOAT_SH(1) | lcms2.BYTES_SH(0)|lcms2.CHANNELS_SH(4);
	    p.hLab2sRGB = lcms2.cmsCreateTransform(hLab,  lcms2.TYPE_Lab_16,  hsRGB, lcms2.TYPE_RGB_DBL, lcms2.INTENT_PERCEPTUAL, lcms2.cmsFLAGS_NOOPTIMIZE|lcms2.cmsFLAGS_NOCACHE);
	    p.sRGB2Lab  = lcms2.cmsCreateTransform(hsRGB, lcms2.TYPE_RGB_DBL, hLab,  lcms2.TYPE_Lab_16,  lcms2.INTENT_PERCEPTUAL, lcms2.cmsFLAGS_NOOPTIMIZE|lcms2.cmsFLAGS_NOCACHE);
	    p.hIlimit   = lcms2.cmsCreateTransform(hLimit, cmykfrm, null, lcms2.TYPE_CMYK_16, lcms2.INTENT_PERCEPTUAL, lcms2.cmsFLAGS_NOOPTIMIZE|lcms2.cmsFLAGS_NOCACHE);
	    
	    lcms2.cmsCloseProfile(hLab); lcms2.cmsCloseProfile(hsRGB); lcms2.cmsCloseProfile(hLimit);
	    
	    ContextID = DbgThread();
	    hICC = lcms2.cmsCreateProfilePlaceholder(ContextID);
	    if (hICC == null)
	    {
	    	return null;
	    }
	    
	    lcms2.cmsSetProfileVersion(hICC, 4.2);
	    
	    lcms2.cmsSetDeviceClass(hICC, lcms2.cmsSigOutputClass);
	    lcms2.cmsSetColorSpace(hICC,  lcms2.cmsSigCmykData);
	    lcms2.cmsSetPCS(hICC,         lcms2.cmsSigLabData);
	    
	    BToA0 = lcms2.cmsPipelineAlloc(ContextID, 3, 4);
	    if (BToA0 == null)
	    {
	    	return null;
	    }
	    CLUT = lcms2.cmsStageAllocCLut16bit(ContextID, 17, 3, 4, null);
	    if (CLUT == null)
	    {
	    	return null;
	    }
	    if (!lcms2.cmsStageSampleCLut16bit(CLUT, ForwardSampler, p, 0))
	    {
	    	return null;
	    }
	    
	    lcms2.cmsPipelineInsertStage(BToA0, lcms2.cmsAT_BEGIN, lcms2_internal._cmsStageAllocIdentityCurves(ContextID, 3)); 
	    lcms2.cmsPipelineInsertStage(BToA0, lcms2.cmsAT_END, CLUT);
	    lcms2.cmsPipelineInsertStage(BToA0, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocIdentityCurves(ContextID, 4));
	    
	    if (!lcms2.cmsWriteTag(hICC, lcms2.cmsSigBToA0Tag, BToA0))
	    {
	    	return null;
	    }
	    lcms2.cmsPipelineFree(BToA0);
	    
	    AToB0 = lcms2.cmsPipelineAlloc(ContextID, 4, 3);
	    if (AToB0 == null)
	    {
	    	return null;
	    }
	    CLUT = lcms2.cmsStageAllocCLut16bit(ContextID, 17, 4, 3, null);
	    if (CLUT == null)
	    {
	    	return null;
	    }
	    if (!lcms2.cmsStageSampleCLut16bit(CLUT, ReverseSampler, p, 0))
	    {
	    	return null;
	    }
	    
	    lcms2.cmsPipelineInsertStage(AToB0, lcms2.cmsAT_BEGIN, lcms2_internal._cmsStageAllocIdentityCurves(ContextID, 4)); 
	    lcms2.cmsPipelineInsertStage(AToB0, lcms2.cmsAT_END, CLUT);
	    lcms2.cmsPipelineInsertStage(AToB0, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocIdentityCurves(ContextID, 3));
	    
	    if (!lcms2.cmsWriteTag(hICC, lcms2.cmsSigAToB0Tag, AToB0))
	    {
	    	return null;
	    }
	    lcms2.cmsPipelineFree(AToB0);
	    
	    lcms2.cmsDeleteTransform(p.hLab2sRGB);
	    lcms2.cmsDeleteTransform(p.sRGB2Lab);
	    lcms2.cmsDeleteTransform(p.hIlimit);
	    
	    lcms2.cmsLinkTag(hICC, lcms2.cmsSigAToB1Tag, lcms2.cmsSigAToB0Tag);
	    lcms2.cmsLinkTag(hICC, lcms2.cmsSigAToB2Tag, lcms2.cmsSigAToB0Tag);
	    lcms2.cmsLinkTag(hICC, lcms2.cmsSigBToA1Tag, lcms2.cmsSigBToA0Tag);
	    lcms2.cmsLinkTag(hICC, lcms2.cmsSigBToA2Tag, lcms2.cmsSigBToA0Tag);
	    
	    return hICC;    
	}
	
	// Does create several profiles for latter use------------------------------------------------------------------------------------------------
	
	private static int OneVirtual(cmsHPROFILE h, final String SubTestTxt, final String FileName)
	{
	    SubTest(SubTestTxt, null);
	    if (h == null)
	    {
	    	return 0;
	    }
	    
	    if (!lcms2.cmsSaveProfileToFile(h, FILE_PREFIX + FileName))
	    {
	    	return 0;
	    }
	    lcms2.cmsCloseProfile(h);
	    
	    h = lcms2.cmsOpenProfileFromFile(FILE_PREFIX + FileName, "r");
	    if (h == null)
	    {
	    	return 0;
	    }
	    
	    // Do some test....
	    
	    //XXX Temp
	    
	    lcms2.cmsCloseProfile(h);
	    
	    return 1;
	}
	
	// This test checks the ability of lcms2 to save its built-ins as valid profiles. 
	// It does not check the functionality of such profiles
	private static final TestFn CreateTestProfiles = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE h;
			
			//XXX Takes 6.24 minutes to complete, speed up
		    h = lcms2.cmsCreate_sRGBProfileTHR(DbgThread());
		    if (OneVirtual(h, "sRGB profile", "sRGBlcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ----
		    
		    h = Create_AboveRGB();
		    if (OneVirtual(h, "aRGB profile", "aRGBlcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ----
		    
		    h = Create_Gray22();
		    if (OneVirtual(h, "Gray profile", "graylcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ----
		    
		    h = Create_GrayLab();
		    if (OneVirtual(h, "Gray Lab profile", "glablcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ----
		    
		    h = Create_CMYK_DeviceLink();
		    if (OneVirtual(h, "Linearization profile", "linlcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // -------
		    h = lcms2.cmsCreateInkLimitingDeviceLinkTHR(DbgThread(), lcms2.cmsSigCmykData, 150);
		    if (h == null)
		    {
		    	return 0;
		    }
		    if (OneVirtual(h, "Ink-limiting profile", "limitlcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ------
		    
		    h = lcms2.cmsCreateLab2ProfileTHR(DbgThread(), null);
		    if (OneVirtual(h, "Lab 2 identity profile", "labv2lcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ----
		    
		    h = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    if (OneVirtual(h, "Lab 4 identity profile", "labv4lcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ----
		    
		    h = lcms2.cmsCreateXYZProfileTHR(DbgThread());
		    if (OneVirtual(h, "XYZ identity profile", "xyzlcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ----
		    
		    h = lcms2.cmsCreateNULLProfileTHR(DbgThread());
		    if (OneVirtual(h, "NULL profile", "nullcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ---
		    
		    h = lcms2.cmsCreateBCHSWabstractProfileTHR(DbgThread(), 17, 0, 0, 0, 0, 5000, 6000);
		    if (OneVirtual(h, "BCHS profile", "bchslcms2.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    // ---
		    
		    h = CreateFakeCMYK(300, false);
		    if (OneVirtual(h, "Fake CMYK profile", "lcms2cmyk.icc") == 0)
		    {
		    	return 0;
		    }
		    
		    //Now create the temporary, built-in profiles
		    if(fileOperation("bad.icc", createTemp) != 0)
		    {
		    	return 0;
		    }
		    if(fileOperation("sRGB_Color_Space_Profile.icm", createTemp) != 0)
		    {
		    	return 0;
		    }
		    if(fileOperation("sRGB_v4_ICC_preference.icc", createTemp) != 0)
		    {
		    	return 0;
		    }
		    if(fileOperation("sRGBSpac.icm", createTemp) != 0)
		    {
		    	return 0;
		    }
		    if(fileOperation("toosmall.icc", createTemp) != 0)
		    {
		    	return 0;
		    }
		    if(fileOperation("UncoatedFOGRA29.icc", createTemp) != 0)
		    {
		    	return 0;
		    }
		    if(fileOperation("USWebCoatedSWOP.icc", createTemp) != 0)
		    {
		    	return 0;
		    }
		    
		    return 1;
		}
	};
	
	private interface FileOp
	{
		public void run(String oFile, FileConnection file) throws IOException;
	}
	
	private static final FileOp remove = new FileOp()
	{
		public void run(String oFile, FileConnection file) throws IOException
		{
			if(file.exists())
			{
				file.delete();
			}
		}
	};
	
	private static final FileOp createTemp = new FileOp()
	{
		public void run(String oFile, FileConnection file) throws IOException
		{
			if(file.exists())
			{
				file.truncate(0L);
			}
			else
			{
				file.create();
			}
			byte[] buffer = new byte[4096];
    		OutputStream out = file.openOutputStream();
    		InputStream in = this.getClass().getResourceAsStream(RES_PREFIX + oFile);
    		int count = in.read(buffer, 0, 4096);
    		while(count > 0)
    		{
    			out.write(buffer, 0, count);
    			count = in.read(buffer, 0, 4096);
    		}
    		in.close();
    		out.close();
		}
	};
	
	private static int fileOperation(final String file, FileOp fop)
	{
		FileConnection ifile = null;
		boolean exp = false;
		try
		{
			ifile = (FileConnection)Connector.open(FILE_PREFIX + file, Connector.READ_WRITE);
			fop.run(file, ifile);
		}
		catch(Exception e)
		{
			exp = true;
		}
		finally
		{
			if(ifile != null)
			{
				try
				{
					ifile.close();
				}
				catch(Exception e)
				{
				}
			}
		}
		return exp ? 1 : 0;
	}
	
	private static void RemoveTestProfiles()
	{
	    fileOperation("sRGBlcms2.icc", remove);
	    fileOperation("aRGBlcms2.icc", remove);
	    fileOperation("graylcms2.icc", remove);
	    fileOperation("linlcms2.icc", remove);
	    fileOperation("limitlcms2.icc", remove);
	    fileOperation("labv2lcms2.icc", remove);
	    fileOperation("labv4lcms2.icc", remove);
	    fileOperation("xyzlcms2.icc", remove);
	    fileOperation("nullcms2.icc", remove);
	    fileOperation("bchslcms2.icc", remove);
	    fileOperation("lcms2cmyk.icc", remove);
	    fileOperation("glablcms2.icc", remove);
	    
	    //Also remove the temporary, built-in profiles
	    fileOperation("bad.icc", remove);
	    fileOperation("sRGB_Color_Space_Profile.icm", remove);
	    fileOperation("sRGB_v4_ICC_preference.icc", remove);
	    fileOperation("sRGBSpac.icm", remove);
	    fileOperation("toosmall.icc", remove);
	    fileOperation("UncoatedFOGRA29.icc", remove);
	    fileOperation("USWebCoatedSWOP.icc", remove);
	}
	
	// -------------------------------------------------------------------------------------------------
	
	// Are we little or big endian?  From Harbison&Steele.
	private static final TestFn CheckEndianess = new TestFn()
	{
		public int run()
		{
			boolean BigEndian, IsOk;
			VirtualPointer u = new VirtualPointer(8);
			u.getProcessor().write(1L);
			
		    BigEndian = u.readRaw(7) == 1;
		    
//#ifdef CMS_USE_BIG_ENDIAN
		    IsOk = BigEndian;
//#else
		    IsOk = !BigEndian;
//#endif
		    
		    if (!IsOk)
		    {
		        Fail("\nOOOPPSS! You have CMS_USE_BIG_ENDIAN toggle misconfigured!\n\n" + 
		            "Please, edit lcms2.h and %s the CMS_USE_BIG_ENDIAN toggle.\n", new Object[]{BigEndian? "uncomment" : "comment"});
		        return 0;
		    }
		    
		    return 1;
		}
	};
	
	// Check quick floor
	private static final TestFn CheckQuickFloor = new TestFn()
	{
		public int run()
		{
			if ((lcms2_internal._cmsQuickFloor(1.234) != 1) ||
		        (lcms2_internal._cmsQuickFloor(32767.234) != 32767) ||
		        (lcms2_internal._cmsQuickFloor(-1.234) != -2) ||
		        (lcms2_internal._cmsQuickFloor(-32767.1) != -32768))
			{
				Fail("\nOOOPPSS! _cmsQuickFloor() does not work as expected in your machine!\n\n" +
						"Please, edit lcms.h and uncomment the CMS_DONT_USE_FAST_FLOOR toggle.\n", null);
				return 0;
		    }
			
		    return 1;
		}
	};
	
	// Quick floor restricted to word
	private static final TestFn CheckQuickFloorWord = new TestFn()
	{
		public int run()
		{
			int i;
			
		    for (i=0; i < 65535; i++)
		    {
		        if ((lcms2_internal._cmsQuickFloorWord(i + 0.1234) & 0xFFFF) != i)
		        {
		            Fail("\nOOOPPSS! _cmsQuickFloorWord() does not work as expected in your machine!\n\n" +
		                "Please, edit lcms.h and uncomment the CMS_DONT_USE_FAST_FLOOR toggle.\n", null);
		            return 0;
		        }
		    }
		    
		    return 1;
		}
	};
	
	// -------------------------------------------------------------------------------------------------
	
	// Precision stuff. 
	
	// On 15.16 fixed point, this is the maximum we can obtain. Remember ICC profiles have storage limits on this number 
	private static final double FIXED_PRECISION_15_16 = (1.0 / 65535.0);
	
	// On 8.8 fixed point, that is the max we can obtain.
	private static final double FIXED_PRECISION_8_8 = (1.0 / 255.0);
	
	// On cmsFloat32Number type, this is the precision we expect
	private static final double FLOAT_PRECISSION = (0.00001);
	
	private static double MaxErr;
	private static double AllowedErr = FIXED_PRECISION_15_16;
	
	private static boolean IsGoodVal(final String title, double in, double out, double max)
	{
	    double Err = Math.abs(in - out);
	    
	    if (Err > MaxErr)
	    {
	    	MaxErr = Err;
	    }
	    
	    if ((Err > max ))
	    {
	    	Fail("(%s): Must be %f, But is %f ", new Object[]{title, new Double(in), new Double(out)});
	    	return false;
	    }
	    
	    return true;
	}
	
	private static boolean IsGoodFixed15_16(final String title, double in, double out)
	{   
	    return IsGoodVal(title, in, out, FIXED_PRECISION_15_16);
	}
	
	private static boolean IsGoodFixed8_8(final String title, double in, double out)
	{
	    return IsGoodVal(title, in, out, FIXED_PRECISION_8_8);
	}
	
	private static boolean IsGoodWord(final String title, short in, short out)
	{
	    if ((Math.abs(in - out) > 0 ))
	    {
	    	Fail("(%s): Must be %x, But is %x ", new Object[]{title, new Short(in), new Short(out)});
	        return false;
	    }
	    
	    return true;
	}
	
	private static boolean IsGoodWordPrec(final String title, short in, short out, short maxErr)
	{
	    if ((Math.abs(in - out) > maxErr ))
	    {
	    	Fail("(%s): Must be %x, But is %x ", new Object[]{title, new Short(in), new Short(out)});
	        return false;
	    }
	    
	    return true;
	}
	
	// Fixed point ----------------------------------------------------------------------------------------------
	
	private static int TestSingleFixed15_16(double d)
	{
	    int f = lcms2_internal._cmsDoubleTo15Fixed16(d);
	    double RoundTrip = lcms2_internal._cms15Fixed16toDouble(f);
	    double Error     = Math.abs(d - RoundTrip);
	    
	    return ( Error <= FIXED_PRECISION_15_16) ? 1 : 0;
	}
	
	private static final TestFn CheckFixedPoint15_16 = new TestFn()
	{
		public int run()
		{
			if (TestSingleFixed15_16(1.0) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(2.0) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(1.23456) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(0.99999) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(0.1234567890123456789099999) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(-1.0) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(-2.0) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(-1.23456) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(-1.1234567890123456789099999) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(+32767.1234567890123456789099999) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed15_16(-32767.1234567890123456789099999) == 0)
			{
				return 0;
			}
		    return 1;
		}
	};
	
	private static int TestSingleFixed8_8(double d)
	{
	    short f = lcms2_internal._cmsDoubleTo8Fixed8(d);
	    double RoundTrip = lcms2_internal._cms8Fixed8toDouble(f);
	    double Error     = Math.abs(d - RoundTrip);
	    
	    return ( Error <= FIXED_PRECISION_8_8) ? 1 : 0;
	}
	
	private static final TestFn CheckFixedPoint8_8 = new TestFn()
	{
		public int run()
		{
			if (TestSingleFixed8_8(1.0) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed8_8(2.0) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed8_8(1.23456) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed8_8(0.99999) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed8_8(0.1234567890123456789099999) == 0)
			{
				return 0;
			}
		    if (TestSingleFixed8_8(+255.1234567890123456789099999) == 0)
			{
				return 0;
			}
		    
		    return 1;
		}
	};
	
	// Linear interpolation -----------------------------------------------------------------------------------------------
	
	// Since prime factors of 65535 (FFFF) are,
	//
	//	            0xFFFF = 3 * 5 * 17 * 257
	//
	// I test tables of 2, 4, 6, and 18 points, that will be exact.
	
	private static void BuildTable(int n, short[] Tab, boolean Descending)
	{
	    int i;
	    
	    for (i=0; i < n; i++) {
	        double v = (65535.0 * i ) / (n-1);
	        
	        Tab[Descending ? (n - i - 1) : i ] = (short)Math.floor(v + 0.5);
	    }
	}
	
	// A single function that does check 1D interpolation
	// nNodesToCheck = number on nodes to check
	// Down = Create decreasing tables
	// Reverse = Check reverse interpolation
	// max_err = max allowed error 
	
	private static int Check1D(int nNodesToCheck, boolean Down, int max_err)
	{
	    int i;
	    short[] in = new short[1], out = new short[1];
	    cmsInterpParams p;
	    short[] Tab;
	    
	    Tab = new short[nNodesToCheck];
	    
	    p = lcms2_internal._cmsComputeInterpParams(DbgThread(), nNodesToCheck, 1, 1, Tab, lcms2_plugin.CMS_LERP_FLAGS_16BITS);
	    if (p == null)
	    {
	    	return 0;
	    }
	    
	    BuildTable(nNodesToCheck, Tab, Down);
	    
	    for (i=0; i <= 0xffff; i++)
	    {
	        in[0] = (short)i;
	        out[0] = 0;
	        
	        p.Interpolation.get16().run(in, out, p);
	        
	        if (Down)
	        {
	        	out[0] = (short)(0xffff - (out[0] & 0xFFFF));
	        }
	        
	        if (Math.abs((out[0] & 0xFFFF) - (in[0] & 0xFFFF)) > max_err)
	        {
	            Fail("(%dp): Must be %x, But is %x : ", new Object[]{new Integer(nNodesToCheck), new Short(in[0]), new Short(out[0])});
	            lcms2_internal._cmsFreeInterpParams(p);
	            return 0;
	        }
	    }
	    
	    lcms2_internal._cmsFreeInterpParams(p);
	    return 1;
	}
	
	private static final TestFn Check1DLERP2 = new TestFn()
	{
		public int run()
		{
			return Check1D(2, false, 0);
		}
	};
	
	private static final TestFn Check1DLERP3 = new TestFn()
	{
		public int run()
		{
			return Check1D(3, false, 1);
		}
	};
	
	private static final TestFn Check1DLERP4 = new TestFn()
	{
		public int run()
		{
			return Check1D(4, false, 0);
		}
	};
	
	private static final TestFn Check1DLERP6 = new TestFn()
	{
		public int run()
		{
			return Check1D(6, false, 0);
		}
	};
	
	private static final TestFn Check1DLERP18 = new TestFn()
	{
		public int run()
		{
			return Check1D(18, false, 0);
		}
	};
	
	private static final TestFn Check1DLERP2Down = new TestFn()
	{
		public int run()
		{
			return Check1D(2, true, 0);
		}
	};
	
	private static final TestFn Check1DLERP3Down = new TestFn()
	{
		public int run()
		{
			return Check1D(3, true, 1);
		}
	};
	
	private static final TestFn Check1DLERP6Down = new TestFn()
	{
		public int run()
		{
			return Check1D(6, true, 0);
		}
	};
	
	private static final TestFn Check1DLERP18Down = new TestFn()
	{
		public int run()
		{
			return Check1D(18, true, 0);
		}
	};
	
	private static final TestFn ExhaustiveCheck1DLERP = new TestFn()
	{
		public int run()
		{
			int j;
		    
		    Utility.fprintf(print, "\n", null);
		    for (j=10; j <= 4096; j++)
		    {
		        if ((j % 10) == 0)
		        {
		        	Utility.fprintf(print, "%d    \r", new Object[]{new Integer(j)});
		        }
		        
		        if (Check1D(j, false, 1) == 0)
		        {
		        	return 0;    
		        }
		    }
		    
		    Utility.fprintf(print, "\rResult is ", null);
		    return 1;
		}
	};
	
	private static final TestFn ExhaustiveCheck1DLERPDown = new TestFn()
	{
		public int run()
		{
			int j;
		    
			Utility.fprintf(print, "\n", null);
		    for (j=10; j <= 4096; j++)
		    {
		        if ((j % 10) == 0)
		        {
		        	Utility.fprintf(print, "%d    \r", new Object[]{new Integer(j)});
		        }
		        
		        if (Check1D(j, true, 1) == 0)
		        {
		        	return 0; 
		        }
		    }
		    
		    Utility.fprintf(print, "\rResult is ", null);
		    return 1;
		}
	};
	
	// 3D interpolation -------------------------------------------------------------------------------------------------
	
	private static final TestFn Check3DinterpolationFloatTetrahedral = new TestFn()
	{
		public int run()
		{
			cmsInterpParams p;
		    int i;
		    float[] In = new float[3], Out = new float[3];
		    float[] FloatTable = { //R     G    B
		        0,    0,   0,     // B=0,G=0,R=0
		        0,    0,  .25f,   // B=1,G=0,R=0
		        
		        0,   .5f,    0,   // B=0,G=1,R=0
		        0,   .5f,  .25f,  // B=1,G=1,R=0
		        
		        1,    0,    0,    // B=0,G=0,R=1
		        1,    0,  .25f,   // B=1,G=0,R=1
		        
		        1,    .5f,   0,   // B=0,G=1,R=1
		        1,    .5f,  .25f  // B=1,G=1,R=1
		    };
		    
		    p = lcms2_internal._cmsComputeInterpParams(DbgThread(), 2, 3, 3, FloatTable, lcms2_plugin.CMS_LERP_FLAGS_FLOAT);
		    
		    MaxErr = 0.0;
		    for (i=0; i < 0xffff; i++)
		    {
		    	In[0] = In[1] = In[2] = (i / 65535.0F);
		    	
		        p.Interpolation.getFloat().run(In, Out, p);
		        
		        if (!IsGoodFixed15_16("Channel 1", Out[0], In[0]))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		        if (!IsGoodFixed15_16("Channel 2", Out[1], In[1] / 2.F))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		        if (!IsGoodFixed15_16("Channel 3", Out[2], In[2] / 4.F))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		    }
		    
		    if (MaxErr > 0)
		    {
		    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr)});
		    }
		    lcms2_internal._cmsFreeInterpParams(p);
		    return 1;
		}
	};
	
	private static final TestFn Check3DinterpolationFloatTrilinear = new TestFn()
	{
		public int run()
		{
			cmsInterpParams p;
		    int i;
		    float[] In = new float[3], Out = new float[3];
		    float[] FloatTable = { //R     G    B
		        0,    0,   0,     // B=0,G=0,R=0
		        0,    0,  .25f,   // B=1,G=0,R=0
		        
		        0,   .5f,    0,   // B=0,G=1,R=0
		        0,   .5f,  .25f,  // B=1,G=1,R=0
		        
		        1,    0,    0,    // B=0,G=0,R=1
		        1,    0,  .25f,   // B=1,G=0,R=1
		        
		        1,    .5f,   0,   // B=0,G=1,R=1
		        1,    .5f,  .25f  // B=1,G=1,R=1
		    };
		    
		    p = lcms2_internal._cmsComputeInterpParams(DbgThread(), 2, 3, 3, FloatTable, lcms2_plugin.CMS_LERP_FLAGS_FLOAT|lcms2_plugin.CMS_LERP_FLAGS_TRILINEAR);
		    
		    MaxErr = 0.0;
		    for (i=0; i < 0xffff; i++)
		    {
		    	In[0] = In[1] = In[2] = (i / 65535.0F);
		    	
		        p.Interpolation.getFloat().run(In, Out, p);
		        
		        if (!IsGoodFixed15_16("Channel 1", Out[0], In[0]))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		        if (!IsGoodFixed15_16("Channel 2", Out[1], In[1] / 2.F))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		        if (!IsGoodFixed15_16("Channel 3", Out[2], In[2] / 4.F))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		    }
		    
		    if (MaxErr > 0)
		    {
		    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr)});
		    }
		    lcms2_internal._cmsFreeInterpParams(p);
		    return 1;
		}
	};
	
	private static final TestFn Check3DinterpolationTetrahedral16 = new TestFn()
	{
		public int run()
		{
			cmsInterpParams p;
		    int i;
		    short[] In = new short[3], Out = new short[3];
		    short[] Table = { 
		            0,    0,   0,     
		            0,    0,   (short)0xffff,    
		            
		            0,    (short)0xffff,    0,   
		            0,    (short)0xffff,    (short)0xffff,  
		            
		            (short)0xffff,    0,    0,    
		            (short)0xffff,    0,    (short)0xffff,   
		            
		            (short)0xffff,    (short)0xffff,   0,    
		            (short)0xffff,    (short)0xffff,   (short)0xffff    
		        };
		    
		    p = lcms2_internal._cmsComputeInterpParams(DbgThread(), 2, 3, 3, Table, lcms2_plugin.CMS_LERP_FLAGS_16BITS);
		    
		    MaxErr = 0.0;
		    for (i=0; i < 0xffff; i++)
		    {
		    	In[0] = In[1] = In[2] = (short)i;
		    	
		        p.Interpolation.get16().run(In, Out, p);
		        
		        if (!IsGoodWord("Channel 1", Out[0], In[0]))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		        if (!IsGoodWord("Channel 2", Out[1], In[1]))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		        if (!IsGoodWord("Channel 3", Out[2], In[2]))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		    }
		    
		    if (MaxErr > 0)
		    {
		    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr)});
		    }
		    lcms2_internal._cmsFreeInterpParams(p);
		    return 1;
		}
	};
	
	private static final TestFn Check3DinterpolationTrilinear16 = new TestFn()
	{
		public int run()
		{
			cmsInterpParams p;
		    int i;
		    short[] In = new short[3], Out = new short[3];
		    short[] Table = { 
		            0,    0,   0,     
		            0,    0,   (short)0xffff,    
		            
		            0,    (short)0xffff,    0,   
		            0,    (short)0xffff,    (short)0xffff,  
		            
		            (short)0xffff,    0,    0,    
		            (short)0xffff,    0,    (short)0xffff,   
		            
		            (short)0xffff,    (short)0xffff,   0,    
		            (short)0xffff,    (short)0xffff,   (short)0xffff    
		        };
		    
		    p = lcms2_internal._cmsComputeInterpParams(DbgThread(), 2, 3, 3, Table, lcms2_plugin.CMS_LERP_FLAGS_TRILINEAR);
		    
		    MaxErr = 0.0;
		    for (i=0; i < 0xffff; i++)
		    {
		    	In[0] = In[1] = In[2] = (short)i;
		    	
		        p.Interpolation.get16().run(In, Out, p);
		        
		        if (!IsGoodWord("Channel 1", Out[0], In[0]))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		        if (!IsGoodWord("Channel 2", Out[1], In[1]))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		        if (!IsGoodWord("Channel 3", Out[2], In[2]))
		        {
		        	lcms2_internal._cmsFreeInterpParams(p);
				    return 0;
		        }
		    }
		    
		    if (MaxErr > 0)
		    {
		    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr)});
		    }
		    lcms2_internal._cmsFreeInterpParams(p);
		    return 1;
		}
	};
	
	private static final TestFn ExaustiveCheck3DinterpolationFloatTetrahedral = new TestFn()
	{
		public int run()
		{
			cmsInterpParams p;
		    int r, g, b;
		    float[] In = new float[3], Out = new float[3];
		    float[] FloatTable = { //R     G    B
		        0,    0,   0,     // B=0,G=0,R=0
		        0,    0,  .25f,   // B=1,G=0,R=0
		        
		        0,   .5f,    0,   // B=0,G=1,R=0
		        0,   .5f,  .25f,  // B=1,G=1,R=0
		        
		        1,    0,    0,    // B=0,G=0,R=1
		        1,    0,  .25f,   // B=1,G=0,R=1
		        
		        1,    .5f,   0,   // B=0,G=1,R=1
		        1,    .5f,  .25f  // B=1,G=1,R=1
		    };
		    
		    p = lcms2_internal._cmsComputeInterpParams(DbgThread(), 2, 3, 3, FloatTable, lcms2_plugin.CMS_LERP_FLAGS_FLOAT);
		    
		    MaxErr = 0.0;
		    for (r=0; r < 0xff; r++)
		    {
		    	for (g=0; g < 0xff; g++)
		    	{
		            for (b=0; b < 0xff; b++) 
			        {
			            In[0] = r / 255.0F;
			            In[1] = g / 255.0F;
			            In[2] = b / 255.0F;
			            
				        p.Interpolation.getFloat().run(In, Out, p);
				        
				        if (!IsGoodFixed15_16("Channel 1", Out[0], In[0]))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				        if (!IsGoodFixed15_16("Channel 2", Out[1], In[1] / 2.F))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				        if (!IsGoodFixed15_16("Channel 3", Out[2], In[2] / 4.F))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
			        }
		    	}
		    }
		    
		    if (MaxErr > 0)
		    {
		    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr)});
		    }
		    lcms2_internal._cmsFreeInterpParams(p);
		    return 1;
		}
	};
	
	private static final TestFn ExaustiveCheck3DinterpolationFloatTrilinear = new TestFn()
	{
		public int run()
		{
			cmsInterpParams p;
		    int r, g, b;
		    float[] In = new float[3], Out = new float[3];
		    float[] FloatTable = { //R     G    B
		        0,    0,   0,     // B=0,G=0,R=0
		        0,    0,  .25f,   // B=1,G=0,R=0
		        
		        0,   .5f,    0,   // B=0,G=1,R=0
		        0,   .5f,  .25f,  // B=1,G=1,R=0
		        
		        1,    0,    0,    // B=0,G=0,R=1
		        1,    0,  .25f,   // B=1,G=0,R=1
		        
		        1,    .5f,   0,   // B=0,G=1,R=1
		        1,    .5f,  .25f  // B=1,G=1,R=1
		    };
		    
		    p = lcms2_internal._cmsComputeInterpParams(DbgThread(), 2, 3, 3, FloatTable, lcms2_plugin.CMS_LERP_FLAGS_FLOAT|lcms2_plugin.CMS_LERP_FLAGS_TRILINEAR);
		    
		    MaxErr = 0.0;
		    for (r=0; r < 0xff; r++)
		    {
		    	for (g=0; g < 0xff; g++)
		    	{
		            for (b=0; b < 0xff; b++) 
			        {
			            In[0] = r / 255.0F;
			            In[1] = g / 255.0F;
			            In[2] = b / 255.0F;
			            
				        p.Interpolation.getFloat().run(In, Out, p);
				        
				        if (!IsGoodFixed15_16("Channel 1", Out[0], In[0]))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				        if (!IsGoodFixed15_16("Channel 2", Out[1], In[1] / 2.F))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				        if (!IsGoodFixed15_16("Channel 3", Out[2], In[2] / 4.F))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
			        }
		    	}
		    }
		    
		    if (MaxErr > 0)
		    {
		    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr)});
		    }
		    lcms2_internal._cmsFreeInterpParams(p);
		    return 1;
		}
	};
	
	private static final TestFn ExhaustiveCheck3DinterpolationTetrahedral16 = new TestFn()
	{
		public int run()
		{
			cmsInterpParams p;
		    int r, g, b;
		    short[] In = new short[3], Out = new short[3];
		    short[] Table = { 
		            0,    0,   0,     
		            0,    0,   (short)0xffff,    
		            
		            0,    (short)0xffff,    0,   
		            0,    (short)0xffff,    (short)0xffff,  
		            
		            (short)0xffff,    0,    0,    
		            (short)0xffff,    0,    (short)0xffff,   
		            
		            (short)0xffff,    (short)0xffff,   0,    
		            (short)0xffff,    (short)0xffff,   (short)0xffff    
		        };
		    
		    p = lcms2_internal._cmsComputeInterpParams(DbgThread(), 2, 3, 3, Table, lcms2_plugin.CMS_LERP_FLAGS_16BITS);
		    
		    MaxErr = 0.0;
		    for (r=0; r < 0xff; r++)
		    {
		        for (g=0; g < 0xff; g++)
		        {
		            for (b=0; b < 0xff; b++)
				    {
		            	In[0] = (short)r;
		                In[1] = (short)g;
		                In[2] = (short)b;
				    	
				        p.Interpolation.get16().run(In, Out, p);
				        
				        if (!IsGoodWord("Channel 1", Out[0], In[0]))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				        if (!IsGoodWord("Channel 2", Out[1], In[1]))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				        if (!IsGoodWord("Channel 3", Out[2], In[2]))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				    }
		        }
		    }
		    
		    if (MaxErr > 0)
		    {
		    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr)});
		    }
		    lcms2_internal._cmsFreeInterpParams(p);
		    return 1;
		}
	};
	
	private static final TestFn ExhaustiveCheck3DinterpolationTrilinear16 = new TestFn()
	{
		public int run()
		{
			cmsInterpParams p;
		    int r, g, b;
		    short[] In = new short[3], Out = new short[3];
		    short[] Table = { 
		            0,    0,   0,     
		            0,    0,   (short)0xffff,    
		            
		            0,    (short)0xffff,    0,   
		            0,    (short)0xffff,    (short)0xffff,  
		            
		            (short)0xffff,    0,    0,    
		            (short)0xffff,    0,    (short)0xffff,   
		            
		            (short)0xffff,    (short)0xffff,   0,    
		            (short)0xffff,    (short)0xffff,   (short)0xffff    
		        };
		    
		    p = lcms2_internal._cmsComputeInterpParams(DbgThread(), 2, 3, 3, Table, lcms2_plugin.CMS_LERP_FLAGS_TRILINEAR);
		    
		    MaxErr = 0.0;
		    for (r=0; r < 0xff; r++)
		    {
		        for (g=0; g < 0xff; g++)
		        {
		            for (b=0; b < 0xff; b++)
				    {
		            	In[0] = (short)r;
		                In[1] = (short)g;
		                In[2] = (short)b;
				    	
				        p.Interpolation.get16().run(In, Out, p);
				        
				        if (!IsGoodWord("Channel 1", Out[0], In[0]))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				        if (!IsGoodWord("Channel 2", Out[1], In[1]))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				        if (!IsGoodWord("Channel 3", Out[2], In[2]))
				        {
				        	lcms2_internal._cmsFreeInterpParams(p);
						    return 0;
				        }
				    }
		        }
		    }
		    
		    if (MaxErr > 0)
		    {
		    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr)});
		    }
		    lcms2_internal._cmsFreeInterpParams(p);
		    return 1;
		}
	};
	
	// Check reverse interpolation on LUTS. This is right now exclusively used by K preservation algorithm
	private static final TestFn CheckReverseInterpolation3x3 = new TestFn()
	{
		public int run()
		{
			cmsPipeline Lut;
			cmsStage clut;
			float[] Target = new float[3], Result = new float[3], Hint = new float[3];
			float err, max;
			int i;
			short[] Table = new short[]{ 
			        0,    0,   0,										// 0 0 0  
			        0,    0,   (short)0xffff,							// 0 0 1  
			        
			        0,    (short)0xffff,    0,							// 0 1 0  
			        0,    (short)0xffff,    (short)0xffff,				// 0 1 1  
			        
			        (short)0xffff,    0,    0,							// 1 0 0  
			        (short)0xffff,    0,    (short)0xffff,				// 1 0 1  
			        
			        (short)0xffff,    (short)0xffff,   0,				// 1 1 0  
			        (short)0xffff,    (short)0xffff,   (short)0xffff,	// 1 1 1
			};
			
			Lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
			clut = lcms2.cmsStageAllocCLut16bit(DbgThread(), 2, 3, 3, Table);
			lcms2.cmsPipelineInsertStage(Lut, lcms2.cmsAT_BEGIN, clut);
			
			Target[0] = 0; Target[1] = 0; Target[2] = 0;
			Hint[0] = 0; Hint[1] = 0; Hint[2] = 0;
			lcms2.cmsPipelineEvalReverseFloat(Target, Result, null, Lut);
			if (Result[0] != 0 || Result[1] != 0 || Result[2] != 0)
			{
				Fail("Reverse interpolation didn't find zero", null);
				return 0;
			}
			
			// Transverse identity
			max = 0;
			for (i=0; i <= 100; i++)
			{
				float in = i / 100.0F;
				
				Target[0] = in; Target[1] = 0; Target[2] = 0;
				lcms2.cmsPipelineEvalReverseFloat(Target, Result, Hint, Lut);
				
				err = Math.abs(in - Result[0]);
				if (err > max)
				{
					max = err;
				}
				
				System.arraycopy(Result, 0, Hint, 0, Hint.length);
			}
			
			lcms2.cmsPipelineFree(Lut);
			return (max <= FLOAT_PRECISSION) ? 1 : 0;
		}
	};
	
	private static final TestFn CheckReverseInterpolation4x3 = new TestFn()
	{
		public int run()
		{
			cmsPipeline Lut;
			cmsStage clut;
			float[] Target = new float[4], Result = new float[4], Hint = new float[4];
			float err, max;
			int i;
			
			// 4 -> 3, output gets 3 first channels copied
			short[] Table = new short[]{ 
			        0,         0,         0,							//  0 0 0 0   = ( 0, 0, 0)
			        0,         0,         0,							//  0 0 0 1   = ( 0, 0, 0)
			        
			        0,         0,         (short)0xffff,				//  0 0 1 0   = ( 0, 0, 1)
			        0,         0,         (short)0xffff,				//  0 0 1 1   = ( 0, 0, 1)
			        
			        0,         (short)0xffff,    0,						//  0 1 0 0   = ( 0, 1, 0)
			        0,         (short)0xffff,    0,						//  0 1 0 1   = ( 0, 1, 0)
			        
			        0,         (short)0xffff,    (short)0xffff,			//  0 1 1 0    = ( 0, 1, 1)
			        0,         (short)0xffff,    (short)0xffff,			//  0 1 1 1    = ( 0, 1, 1)
			        
			        (short)0xffff,    0,         0,						//  1 0 0 0    = ( 1, 0, 0)
			        (short)0xffff,    0,         0,						//  1 0 0 1    = ( 1, 0, 0)
			        
			        (short)0xffff,    0,         (short)0xffff,			//  1 0 1 0    = ( 1, 0, 1)
			        (short)0xffff,    0,         (short)0xffff,			//  1 0 1 1    = ( 1, 0, 1)
			        
			        (short)0xffff,    (short)0xffff,    0,				//  1 1 0 0    = ( 1, 1, 0)
			        (short)0xffff,    (short)0xffff,    0,				//  1 1 0 1    = ( 1, 1, 0)
			        
			        (short)0xffff,    (short)0xffff,    (short)0xffff,	//  1 1 1 0    = ( 1, 1, 1)
			        (short)0xffff,    (short)0xffff,    (short)0xffff,	//  1 1 1 1    = ( 1, 1, 1)
			};
			
			Lut = lcms2.cmsPipelineAlloc(DbgThread(), 4, 3);
			
			clut = lcms2.cmsStageAllocCLut16bit(DbgThread(), 2, 4, 3, Table);
			lcms2.cmsPipelineInsertStage(Lut, lcms2.cmsAT_BEGIN, clut);
			
			// Check if the LUT is behaving as expected
			SubTest("4->3 feasibility", null);
			for (i=0; i <= 100; i++)
			{
				Target[0] = i / 100.0F;
				Target[1] = Target[0];
				Target[2] = 0;
				Target[3] = 12;
				
				lcms2.cmsPipelineEvalFloat(Target, Result, Lut);
				
				if (!IsGoodFixed15_16("0", Target[0], Result[0]))
				{
					return 0;
				}
				if (!IsGoodFixed15_16("1", Target[1], Result[1]))
				{
					return 0;
				}
				if (!IsGoodFixed15_16("2", Target[2], Result[2]))
				{
					return 0;
				}
			}
			
			SubTest("4->3 zero", null);
			Target[0] = 0;
			Target[1] = 0;
			Target[2] = 0;
			
			// This one holds the fixed K
			Target[3] = 0;
			
			// This is our hint (which is a big lie in this case)
			Hint[0] = 0.1F; Hint[1] = 0.1F; Hint[2] = 0.1F;
			
			lcms2.cmsPipelineEvalReverseFloat(Target, Result, Hint, Lut);
			
			if (Result[0] != 0 || Result[1] != 0 || Result[2] != 0 || Result[3] != 0)
			{
				Fail("Reverse interpolation didn't find zero", null);
				return 0;
			}
			
			SubTest("4->3 find CMY", null);
			max = 0;
			for (i=0; i <= 100; i++)
			{
				float in = i / 100.0F;
				
				Target[0] = in; Target[1] = 0; Target[2] = 0;
				lcms2.cmsPipelineEvalReverseFloat(Target, Result, Hint, Lut);
				
				err = Math.abs(in - Result[0]);
				if (err > max)
				{
					max = err;
				}
				
				System.arraycopy(Result, 0, Hint, 0, Hint.length);
			}
			
			lcms2.cmsPipelineFree(Lut);
			return (max <= FLOAT_PRECISSION) ? 1 : 0;
		}
	};
	
	// Check all interpolation.
	
	private static short Fn8D1(short a1, short a2, short a3, short a4, short a5, short a6, short a7, short a8, int m)
	{
	    return (short)((((a1 & 0xFFFF) + (a2 & 0xFFFF) + (a3 & 0xFFFF) + (a4 & 0xFFFF) + (a5 & 0xFFFF) + (a6 & 0xFFFF) + (a7 & 0xFFFF) + (a8 & 0xFFFF)) & ((1L << 32) - 1)) / m);
	}
	
	private static short Fn8D2(short a1, short a2, short a3, short a4, short a5, short a6, short a7, short a8, int m)
	{
	    return (short)((((a1 & 0xFFFF) + 3 * (a2 & 0xFFFF) + 3* (a3 & 0xFFFF) + (a4 & 0xFFFF) + (a5 & 0xFFFF) + (a6 & 0xFFFF) + (a7 & 0xFFFF) + (a8 & 0xFFFF)) & ((1L << 32) - 1)) / ((m + 4) & ((1L << 32) - 1)));
	}
	
	private static short Fn8D3(short a1, short a2, short a3, short a4, short a5, short a6, short a7, short a8, int m)
	{
	    return (short)(((3 * (a1 & 0xFFFF) + 2 * (a2 & 0xFFFF) + 3 * (a3 & 0xFFFF) + (a4 & 0xFFFF) + (a5 & 0xFFFF) + (a6 & 0xFFFF) + (a7 & 0xFFFF) + (a8 & 0xFFFF)) & ((1L << 32) - 1)) / ((m + 5) & ((1L << 32) - 1)));
	}
	
	private static final short SHORT_ZERO = 0;
	
	private static final cmsSAMPLER16 Sampler3D = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			Out[0] = Fn8D1(In[0], In[1], In[2], SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, 3);
		    Out[1] = Fn8D2(In[0], In[1], In[2], SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, 3);
		    Out[2] = Fn8D3(In[0], In[1], In[2], SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, 3);
		    
		    return 1;
		}
	};
	
	private static final cmsSAMPLER16 Sampler4D = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			Out[0] = Fn8D1(In[0], In[1], In[2], In[3], SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, 4);
		    Out[1] = Fn8D2(In[0], In[1], In[2], In[3], SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, 4);
		    Out[2] = Fn8D3(In[0], In[1], In[2], In[3], SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, 4);
		    
		    return 1;
		}
	};
	
	private static final cmsSAMPLER16 Sampler5D = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			Out[0] = Fn8D1(In[0], In[1], In[2], In[3], In[4], SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, 5);
		    Out[1] = Fn8D2(In[0], In[1], In[2], In[3], In[4], SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, 5);
		    Out[2] = Fn8D3(In[0], In[1], In[2], In[3], In[4], SHORT_ZERO, SHORT_ZERO, SHORT_ZERO, 5);
		    
		    return 1;
		}
	};
	
	private static final cmsSAMPLER16 Sampler6D = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			Out[0] = Fn8D1(In[0], In[1], In[2], In[3], In[4], In[5], SHORT_ZERO, SHORT_ZERO, 6);
		    Out[1] = Fn8D2(In[0], In[1], In[2], In[3], In[4], In[5], SHORT_ZERO, SHORT_ZERO, 6);
		    Out[2] = Fn8D3(In[0], In[1], In[2], In[3], In[4], In[5], SHORT_ZERO, SHORT_ZERO, 6);
		    
		    return 1;
		}
	};
	
	private static final cmsSAMPLER16 Sampler7D = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			Out[0] = Fn8D1(In[0], In[1], In[2], In[3], In[4], In[5], In[6], SHORT_ZERO, 7);
		    Out[1] = Fn8D2(In[0], In[1], In[2], In[3], In[4], In[5], In[6], SHORT_ZERO, 7);
		    Out[2] = Fn8D3(In[0], In[1], In[2], In[3], In[4], In[5], In[6], SHORT_ZERO, 7);
		    
		    return 1;
		}
	};
	
	private static final cmsSAMPLER16 Sampler8D = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			Out[0] = Fn8D1(In[0], In[1], In[2], In[3], In[4], In[5], In[6], In[7], 8);
		    Out[1] = Fn8D2(In[0], In[1], In[2], In[3], In[4], In[5], In[6], In[7], 8);
		    Out[2] = Fn8D3(In[0], In[1], In[2], In[3], In[4], In[5], In[6], In[7], 8);
		    
		    return 1;
		}
	};
	
	private static boolean CheckOne3D(cmsPipeline lut, short a1, short a2, short a3)
	{
		short[] In = new short[3], Out1 = new short[3], Out2 = new short[3];
		
	    In[0] = a1; In[1] = a2; In[2] = a3; 
	    
	    // This is the interpolated value
	    lcms2.cmsPipelineEval16(In, Out1, lut);
	    
	    // This is the real value
	    Sampler3D.run(In, Out2, null);
	    
	    // Let's see the difference
	    
	    if (!IsGoodWordPrec("Channel 1", Out1[0], Out2[0], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 2", Out1[1], Out2[1], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 3", Out1[2], Out2[2], (short)2))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static boolean CheckOne4D(cmsPipeline lut, short a1, short a2, short a3, short a4)
	{
		short[] In = new short[4], Out1 = new short[3], Out2 = new short[3];
		
	    In[0] = a1; In[1] = a2; In[2] = a3; In[3] = a4;
	    
	    // This is the interpolated value
	    lcms2.cmsPipelineEval16(In, Out1, lut);
	    
	    // This is the real value
	    Sampler4D.run(In, Out2, null);
	    
	    // Let's see the difference
	    
	    if (!IsGoodWordPrec("Channel 1", Out1[0], Out2[0], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 2", Out1[1], Out2[1], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 3", Out1[2], Out2[2], (short)2))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static boolean CheckOne5D(cmsPipeline lut, short a1, short a2, short a3, short a4, short a5)
	{
		short[] In = new short[5], Out1 = new short[3], Out2 = new short[3];
		
	    In[0] = a1; In[1] = a2; In[2] = a3; In[3] = a4; In[4] = a5;
	    
	    // This is the interpolated value
	    lcms2.cmsPipelineEval16(In, Out1, lut);
	    
	    // This is the real value
	    Sampler5D.run(In, Out2, null);
	    
	    // Let's see the difference
	    
	    if (!IsGoodWordPrec("Channel 1", Out1[0], Out2[0], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 2", Out1[1], Out2[1], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 3", Out1[2], Out2[2], (short)2))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static boolean CheckOne6D(cmsPipeline lut, short a1, short a2, short a3, short a4, short a5, short a6)
	{
		short[] In = new short[6], Out1 = new short[3], Out2 = new short[3];
		
	    In[0] = a1; In[1] = a2; In[2] = a3; In[3] = a4; In[4] = a5; In[5] = a6;
	    
	    // This is the interpolated value
	    lcms2.cmsPipelineEval16(In, Out1, lut);
	    
	    // This is the real value
	    Sampler6D.run(In, Out2, null);
	    
	    // Let's see the difference
	    
	    if (!IsGoodWordPrec("Channel 1", Out1[0], Out2[0], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 2", Out1[1], Out2[1], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 3", Out1[2], Out2[2], (short)2))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static boolean CheckOne7D(cmsPipeline lut, short a1, short a2, short a3, short a4, short a5, short a6, short a7)
	{
		short[] In = new short[7], Out1 = new short[3], Out2 = new short[3];
		
	    In[0] = a1; In[1] = a2; In[2] = a3; In[3] = a4; In[4] = a5; In[5] = a6; In[6] = a7;
	    
	    // This is the interpolated value
	    lcms2.cmsPipelineEval16(In, Out1, lut);
	    
	    // This is the real value
	    Sampler7D.run(In, Out2, null);
	    
	    // Let's see the difference
	    
	    if (!IsGoodWordPrec("Channel 1", Out1[0], Out2[0], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 2", Out1[1], Out2[1], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 3", Out1[2], Out2[2], (short)2))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static boolean CheckOne8D(cmsPipeline lut, short a1, short a2, short a3, short a4, short a5, short a6, short a7, short a8)
	{
		short[] In = new short[8], Out1 = new short[3], Out2 = new short[3];
		
	    In[0] = a1; In[1] = a2; In[2] = a3; In[3] = a4; In[4] = a5; In[5] = a6; In[6] = a7; In[7] = a8;
	    
	    // This is the interpolated value
	    lcms2.cmsPipelineEval16(In, Out1, lut);
	    
	    // This is the real value
	    Sampler8D.run(In, Out2, null);
	    
	    // Let's see the difference
	    
	    if (!IsGoodWordPrec("Channel 1", Out1[0], Out2[0], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 2", Out1[1], Out2[1], (short)2))
	    {
	    	return false;
	    }
	    if (!IsGoodWordPrec("Channel 3", Out1[2], Out2[2], (short)2))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static final TestFn Check3Dinterp = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut;
		    cmsStage mpe;
		    
		    lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
		    mpe = lcms2.cmsStageAllocCLut16bit(DbgThread(), 9, 3, 3, null);
		    lcms2.cmsStageSampleCLut16bit(mpe, Sampler3D, null, 0);
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_BEGIN, mpe);
		    
		    // Check accuracy
		    
		    if (!CheckOne3D(lut, (short)0, (short)0, (short)0))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0xffff, (short)0xffff, (short)0xffff))
		    {
		    	return 0;
		    }
		    
		    if (!CheckOne3D(lut, (short)0x8080, (short)0x8080, (short)0x8080))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0x0000, (short)0xFE00, (short)0x80FF))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0x1111, (short)0x2222, (short)0x3333))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0x0000, (short)0x0012, (short)0x0013))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0x3141, (short)0x1415, (short)0x1592))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0xFF00, (short)0xFF01, (short)0xFF12))
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return 1;
		}
	};
	
	private static final TestFn Check3DinterpGranular = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut;
		    cmsStage mpe;
		    int[] Dimensions = { 7, 8, 9 };
		    
		    lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
		    mpe = lcms2.cmsStageAllocCLut16bitGranular(DbgThread(), Dimensions, 3, 3, null);
		    lcms2.cmsStageSampleCLut16bit(mpe, Sampler3D, null, 0);
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_BEGIN, mpe);
		    
		    // Check accuracy
		    
		    if (!CheckOne3D(lut, (short)0, (short)0, (short)0))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0xffff, (short)0xffff, (short)0xffff))
		    {
		    	return 0;
		    }
		    
		    if (!CheckOne3D(lut, (short)0x8080, (short)0x8080, (short)0x8080))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0x0000, (short)0xFE00, (short)0x80FF))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0x1111, (short)0x2222, (short)0x3333))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0x0000, (short)0x0012, (short)0x0013))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0x3141, (short)0x1415, (short)0x1592))
		    {
		    	return 0;
		    }
		    if (!CheckOne3D(lut, (short)0xFF00, (short)0xFF01, (short)0xFF12))
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return 1;
		}
	};
	
	private static final TestFn Check4Dinterp = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut;
		    cmsStage mpe;
		    
		    lut = lcms2.cmsPipelineAlloc(DbgThread(), 4, 3);
		    mpe = lcms2.cmsStageAllocCLut16bit(DbgThread(), 9, 4, 3, null);
		    lcms2.cmsStageSampleCLut16bit(mpe, Sampler4D, null, 0);
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_BEGIN, mpe);
		    
		    // Check accuracy
		    
		    if (!CheckOne4D(lut, (short)0, (short)0, (short)0, (short)0))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff))
		    {
		    	return 0;
		    }
		    
		    if (!CheckOne4D(lut, (short)0x8080, (short)0x8080, (short)0x8080, (short)0x8080))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0x0000, (short)0xFE00, (short)0x80FF, (short)0x8888))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0x1111, (short)0x2222, (short)0x3333, (short)0x4444))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0x0000, (short)0x0012, (short)0x0013, (short)0x0014))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0x3141, (short)0x1415, (short)0x1592, (short)0x9261))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0xFF00, (short)0xFF01, (short)0xFF12, (short)0xFF13))
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return 1;
		}
	};
	
	private static final TestFn Check4DinterpGranular = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut;
		    cmsStage mpe;
		    int[] Dimensions = { 9, 8, 7, 6 };
		    
		    lut = lcms2.cmsPipelineAlloc(DbgThread(), 4, 3);
		    mpe = lcms2.cmsStageAllocCLut16bitGranular(DbgThread(), Dimensions, 4, 3, null);
		    lcms2.cmsStageSampleCLut16bit(mpe, Sampler4D, null, 0);
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_BEGIN, mpe);
		    
		    // Check accuracy
		    
		    if (!CheckOne4D(lut, (short)0, (short)0, (short)0, (short)0))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff))
		    {
		    	return 0;
		    }
		    
		    if (!CheckOne4D(lut, (short)0x8080, (short)0x8080, (short)0x8080, (short)0x8080))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0x0000, (short)0xFE00, (short)0x80FF, (short)0x8888))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0x1111, (short)0x2222, (short)0x3333, (short)0x4444))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0x0000, (short)0x0012, (short)0x0013, (short)0x0014))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0x3141, (short)0x1415, (short)0x1592, (short)0x9261))
		    {
		    	return 0;
		    }
		    if (!CheckOne4D(lut, (short)0xFF00, (short)0xFF01, (short)0xFF12, (short)0xFF13))
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return 1;
		}
	};
	
	private static final TestFn Check5DinterpGranular = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut;
		    cmsStage mpe;
		    int[] Dimensions = { 3, 2, 2, 2, 2 };
		    
		    lut = lcms2.cmsPipelineAlloc(DbgThread(), 5, 3);
		    mpe = lcms2.cmsStageAllocCLut16bitGranular(DbgThread(), Dimensions, 5, 3, null);
		    lcms2.cmsStageSampleCLut16bit(mpe, Sampler5D, null, 0);
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_BEGIN, mpe);
		    
		    // Check accuracy
		    
		    if (!CheckOne5D(lut, (short)0, (short)0, (short)0, (short)0, (short)0))
		    {
		    	return 0;
		    }
		    if (!CheckOne5D(lut, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff))
		    {
		    	return 0;
		    }
		    
		    if (!CheckOne5D(lut, (short)0x8080, (short)0x8080, (short)0x8080, (short)0x8080, (short)0x1234))
		    {
		    	return 0;
		    }
		    if (!CheckOne5D(lut, (short)0x0000, (short)0xFE00, (short)0x80FF, (short)0x8888, (short)0x8078))
		    {
		    	return 0;
		    }
		    if (!CheckOne5D(lut, (short)0x1111, (short)0x2222, (short)0x3333, (short)0x4444, (short)0x1455))
		    {
		    	return 0;
		    }
		    if (!CheckOne5D(lut, (short)0x0000, (short)0x0012, (short)0x0013, (short)0x0014, (short)0x2333))
		    {
		    	return 0;
		    }
		    if (!CheckOne5D(lut, (short)0x3141, (short)0x1415, (short)0x1592, (short)0x9261, (short)0x4567))
		    {
		    	return 0;
		    }
		    if (!CheckOne5D(lut, (short)0xFF00, (short)0xFF01, (short)0xFF12, (short)0xFF13, (short)0xF344))
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return 1;
		}
	};
	
	private static final TestFn Check6DinterpGranular = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut;
		    cmsStage mpe;
		    int[] Dimensions = { 4, 3, 3, 2, 2, 2 };
		    
		    lut = lcms2.cmsPipelineAlloc(DbgThread(), 6, 3);
		    mpe = lcms2.cmsStageAllocCLut16bitGranular(DbgThread(), Dimensions, 6, 3, null);
		    lcms2.cmsStageSampleCLut16bit(mpe, Sampler6D, null, 0);
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_BEGIN, mpe);
		    
		    // Check accuracy
		    
		    if (!CheckOne6D(lut, (short)0, (short)0, (short)0, (short)0, (short)0, (short)0))
		    {
		    	return 0;
		    }
		    if (!CheckOne6D(lut, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff))
		    {
		    	return 0;
		    }
		    
		    if (!CheckOne6D(lut, (short)0x8080, (short)0x8080, (short)0x8080, (short)0x8080, (short)0x1234, (short)0x1122))
		    {
		    	return 0;
		    }
		    if (!CheckOne6D(lut, (short)0x0000, (short)0xFE00, (short)0x80FF, (short)0x8888, (short)0x8078, (short)0x2233))
		    {
		    	return 0;
		    }
		    if (!CheckOne6D(lut, (short)0x1111, (short)0x2222, (short)0x3333, (short)0x4444, (short)0x1455, (short)0x3344))
		    {
		    	return 0;
		    }
		    if (!CheckOne6D(lut, (short)0x0000, (short)0x0012, (short)0x0013, (short)0x0014, (short)0x2333, (short)0x4455))
		    {
		    	return 0;
		    }
		    if (!CheckOne6D(lut, (short)0x3141, (short)0x1415, (short)0x1592, (short)0x9261, (short)0x4567, (short)0x5566))
		    {
		    	return 0;
		    }
		    if (!CheckOne6D(lut, (short)0xFF00, (short)0xFF01, (short)0xFF12, (short)0xFF13, (short)0xF344, (short)0x6677))
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return 1;
		}
	};
	
	private static final TestFn Check7DinterpGranular = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut;
		    cmsStage mpe;
		    int[] Dimensions = { 4, 3, 3, 2, 2, 2, 2 };
		    
		    lut = lcms2.cmsPipelineAlloc(DbgThread(), 7, 3);
		    mpe = lcms2.cmsStageAllocCLut16bitGranular(DbgThread(), Dimensions, 7, 3, null);
		    lcms2.cmsStageSampleCLut16bit(mpe, Sampler7D, null, 0);
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_BEGIN, mpe);
		    
		    // Check accuracy
		    
		    if (!CheckOne7D(lut, (short)0, (short)0, (short)0, (short)0, (short)0, (short)0, (short)0))
		    {
		    	return 0;
		    }
		    if (!CheckOne7D(lut, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff))
		    {
		    	return 0;
		    }
		    
		    if (!CheckOne7D(lut, (short)0x8080, (short)0x8080, (short)0x8080, (short)0x8080, (short)0x1234, (short)0x1122, (short)0x0056))
		    {
		    	return 0;
		    }
		    if (!CheckOne7D(lut, (short)0x0000, (short)0xFE00, (short)0x80FF, (short)0x8888, (short)0x8078, (short)0x2233, (short)0x0088))
		    {
		    	return 0;
		    }
		    if (!CheckOne7D(lut, (short)0x1111, (short)0x2222, (short)0x3333, (short)0x4444, (short)0x1455, (short)0x3344, (short)0x1987))
		    {
		    	return 0;
		    }
		    if (!CheckOne7D(lut, (short)0x0000, (short)0x0012, (short)0x0013, (short)0x0014, (short)0x2333, (short)0x4455, (short)0x9988))
		    {
		    	return 0;
		    }
		    if (!CheckOne7D(lut, (short)0x3141, (short)0x1415, (short)0x1592, (short)0x9261, (short)0x4567, (short)0x5566, (short)0xfe56))
		    {
		    	return 0;
		    }
		    if (!CheckOne7D(lut, (short)0xFF00, (short)0xFF01, (short)0xFF12, (short)0xFF13, (short)0xF344, (short)0x6677, (short)0xbabe))
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return 1;
		}
	};
	// Colorimetric conversions -------------------------------------------------------------------------------------------------
	
	// Lab to LCh and back should be performed at 1E-12 accuracy at least
	private static final TestFn CheckLab2LCh = new TestFn()
	{
		public int run()
		{
			int l, a, b;
		    double dist, Max = 0;
		    cmsCIELab Lab = new cmsCIELab(), Lab2 = new cmsCIELab();
		    cmsCIELCh LCh = new cmsCIELCh();
		    
		    for (l=0; l <= 100; l += 10)
		    {
		        for (a=-128; a <= +128; a += 8)
		        {
		            for (b=-128; b <= 128; b += 8)
		            {
		                Lab.L = l;
		                Lab.a = a;
		                Lab.b = b;
		                
		                lcms2.cmsLab2LCh(LCh, Lab);
		                lcms2.cmsLCh2Lab(Lab2, LCh);
		                
		                dist = lcms2.cmsDeltaE(Lab, Lab2);
		                if (dist > Max)
		                {
		                	Max = dist;
		                }
		            }
		        }
		    }
		    
		    return Max < 1E-12 ? 1 : 0;
		}
	};
	
	// Lab to LCh and back should be performed at 1E-12 accuracy at least
	private static final TestFn CheckLab2XYZ = new TestFn()
	{
		public int run()
		{
			int l, a, b;
		    double dist, Max = 0;
		    cmsCIELab Lab = new cmsCIELab(), Lab2 = new cmsCIELab();
		    cmsCIEXYZ XYZ = new cmsCIEXYZ();
		    
		    for (l=0; l <= 100; l += 10)
		    {
		        for (a=-128; a <= +128; a += 8)
		        {
		            for (b=-128; b <= 128; b += 8)
		            {
		                Lab.L = l;
		                Lab.a = a;
		                Lab.b = b;
		                
		                lcms2.cmsLab2XYZ(null, XYZ, Lab);
		                lcms2.cmsXYZ2Lab(null, Lab2, XYZ);
		                
		                dist = lcms2.cmsDeltaE(Lab, Lab2);
		                if (dist > Max)
		                {
		                	Max = dist;
		                }
		            }
		        }
		    }
		    
		    return Max < 1E-12 ? 1 : 0;
		}
	};
	
	// Lab to xyY and back should be performed at 1E-12 accuracy at least
	private static final TestFn CheckLab2xyY = new TestFn()
	{
		public int run()
		{
			int l, a, b;
		    double dist, Max = 0;
		    cmsCIELab Lab = new cmsCIELab(), Lab2 = new cmsCIELab();
		    cmsCIEXYZ XYZ = new cmsCIEXYZ();
		    cmsCIExyY xyY = new cmsCIExyY();
		    
		    for (l=0; l <= 100; l += 10)
		    {
		        for (a=-128; a <= +128; a += 8)
		        {
		            for (b=-128; b <= 128; b += 8)
		            {
		                Lab.L = l;
		                Lab.a = a;
		                Lab.b = b;
		                
		                lcms2.cmsLab2XYZ(null, XYZ, Lab);
		                lcms2.cmsXYZ2xyY(xyY, XYZ);
		                lcms2.cmsxyY2XYZ(XYZ, xyY);
		                lcms2.cmsXYZ2Lab(null, Lab2, XYZ);
		                
		                dist = lcms2.cmsDeltaE(Lab, Lab2);
		                if (dist > Max)
		                {
		                	Max = dist;
		                }
		            }
		        }
		    }
		    
		    return Max < 1E-12 ? 1 : 0;
		}
	};
	
	private static final TestFn CheckLabV2encoding = new TestFn()
	{
		public int run()
		{
			int n2, i, j;
		    short[] Inw = new short[3], aw = new short[3];
		    cmsCIELab Lab = new cmsCIELab();
		    
		    n2=0;
		    
		    for (j=0; j < 65535; j++)
		    {
		        Inw[0] = Inw[1] = Inw[2] = (short)j;
		        
		        lcms2.cmsLabEncoded2FloatV2(Lab, Inw);
		        lcms2.cmsFloat2LabEncodedV2(aw, Lab);
		        
		        for (i=0; i < 3; i++)
		        {
		        	if ((aw[i] & 0xFFFF) != j)
		        	{
		        		n2++;
		        	}
		        }
		    }
		    return (n2 == 0) ? 1 : 0;
		}
	};
	
	private static final TestFn CheckLabV4encoding = new TestFn()
	{
		public int run()
		{
			int n2, i, j;
		    short[] Inw = new short[3], aw = new short[3];
		    cmsCIELab Lab = new cmsCIELab();
		    
		    n2=0;
		    
		    for (j=0; j < 65535; j++)
		    {
		        Inw[0] = Inw[1] = Inw[2] = (short)j;
		        
		        lcms2.cmsLabEncoded2Float(Lab, Inw);
		        lcms2.cmsFloat2LabEncoded(aw, Lab);
		        
		        for (i=0; i < 3; i++)
		        {
		        	if ((aw[i] & 0xFFFF) != j)
		        	{
		        		n2++;
		        	}
		        }
		    }
		    return (n2 == 0) ? 1 : 0;
		}
	};
	
	// BlackBody -----------------------------------------------------------------------------------------------------
	
	private static final TestFn CheckTemp2CHRM = new TestFn()
	{
		public int run()
		{
			int j;
		    double d, Max = 0;
		    double[] v = new double[1];
		    cmsCIExyY White = new cmsCIExyY();
		    
		    for (j=4000; j < 25000; j++)
		    {
		        lcms2.cmsWhitePointFromTemp(White, j);
		        if (!lcms2.cmsTempFromWhitePoint(v, White))
		        {
		        	return 0;
		        }
		        
		        d = Math.abs(v[0] - j);
		        if (d > Max)
		        {
		        	Max = d;
		        }
		    }
		    
		    // 100 degree is the actual resolution
		    return (Max < 100) ? 1 : 0;
		}
	};
	
	// Tone curves -----------------------------------------------------------------------------------------------------
	
	private static int CheckGammaEstimation(cmsToneCurve c, double g)
	{
	    double est = lcms2.cmsEstimateGamma(c, 0.001);
	    
	    SubTest("Gamma estimation", null);
	    if (Math.abs(est - g) > 0.001)
	    {
	    	return 0;
	    }
	    return 1;
	}
	
	private static final TestFn CheckGammaCreation16 = new TestFn()
	{
		public int run()
		{
			cmsToneCurve LinGamma = lcms2.cmsBuildGamma(DbgThread(), 1.0);
		    int i;
		    short in, out;
		    
		    for (i=0; i < 0xffff; i++)
		    {
		        in = (short) i;
		        out = lcms2.cmsEvalToneCurve16(LinGamma, in);
		        if (in != out)
		        {
		            Fail("(lin gamma): Must be %x, But is %x : ", new Object[]{new Short(in), new Short(out)});
		            lcms2.cmsFreeToneCurve(LinGamma);
		            return 0;
		        }
		    }       
		    
		    if (CheckGammaEstimation(LinGamma, 1.0) == 0)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsFreeToneCurve(LinGamma);
		    return 1;
		}
	};
	
	private static final TestFn CheckGammaCreationFlt = new TestFn()
	{
		public int run()
		{
			cmsToneCurve LinGamma = lcms2.cmsBuildGamma(DbgThread(), 1.0);
		    int i;
		    float in, out;
		    
		    for (i=0; i < 0xffff; i++)
		    {
		        in = (i / 65535.0f);
		        out = lcms2.cmsEvalToneCurveFloat(LinGamma, in);
		        if (Math.abs(in - out) > (1/65535.0))
		        {
		            Fail("(lin gamma): Must be %f, But is %f : ", new Object[]{new Float(in), new Float(out)});
		            lcms2.cmsFreeToneCurve(LinGamma);
		            return 0;
		        }
		    }       
		    
		    if (CheckGammaEstimation(LinGamma, 1.0) == 0)
		    {
		    	return 0;
		    }
		    lcms2.cmsFreeToneCurve(LinGamma);
		    return 1;
		}
	};
	
	// Curve curves using a single power function
	// Error is given in 0..ffff counts
	private static int CheckGammaFloat(double g)
	{
	    cmsToneCurve Curve = lcms2.cmsBuildGamma(DbgThread(), g);
	    int i;
	    float in, out;
	    double val, Err;
	    
	    MaxErr = 0.0;
	    for (i=0; i < 0xffff; i++)
	    {
	        in = (i / 65535.0f);
	        out = lcms2.cmsEvalToneCurveFloat(Curve, in);
//#ifndef BlackBerrySDK4.5.0
	        val = MathUtilities.pow(in, g);
//#else
	        val = Utility.pow(in, g);
//#endif
	        
	        Err = Math.abs(val - out);
	        if (Err > MaxErr)
	        {
	        	MaxErr = Err;     
	        }
	    }
	    
	    if (MaxErr > 0)
	    {
	    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr * 65535.0)});
	    }
	    
	    if (CheckGammaEstimation(Curve, g) == 0)
	    {
	    	return 0;
	    }
	    
	    lcms2.cmsFreeToneCurve(Curve);
	    return 1;
	}
	
	private static final TestFn CheckGamma18 = new TestFn()
	{
		public int run()
		{
			return CheckGammaFloat(1.8);
		}
	};
	
	private static final TestFn CheckGamma22 = new TestFn()
	{
		public int run()
		{
			return CheckGammaFloat(2.2);
		}
	};
	
	private static final TestFn CheckGamma30 = new TestFn()
	{
		public int run()
		{
			return CheckGammaFloat(3.0);
		}
	};
	
	// Check table-based gamma functions
	private static int CheckGammaFloatTable(double g)
	{
	    float[] Values = new float[1025];
	    cmsToneCurve Curve; 
	    int i;
	    float in, out;
	    double val, Err;
	    
	    for (i=0; i <= 1024; i++)
	    {
	        in = (i / 1024.0f);
//#ifndef BlackBerrySDK4.5.0
	        Values[i] = (float)MathUtilities.pow(in, g);
//#else
	        Values[i] = (float)Utility.pow(in, g);
//#endif
	    }
	    
	    Curve = lcms2.cmsBuildTabulatedToneCurveFloat(DbgThread(), 1025, Values);
	    
	    MaxErr = 0.0;
	    for (i=0; i <= 0xffff; i++)
	    {
	        in = (i / 65535.0f);
	        out = lcms2.cmsEvalToneCurveFloat(Curve, in);
//#ifndef BlackBerrySDK4.5.0
	        val = MathUtilities.pow(in, g);
//#else
	        val = Utility.pow(in, g);
//#endif
	        
	        Err = Math.abs(val - out);
	        if (Err > MaxErr)
	        {
	        	MaxErr = Err;     
	        }
	    }
	    
	    if (MaxErr > 0)
	    {
	    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr * 65535.0)});
	    }
	    
	    if (CheckGammaEstimation(Curve, g) == 0)
	    {
	    	return 0;
	    }
	    
	    lcms2.cmsFreeToneCurve(Curve);
	    return 1;
	}
	
	private static final TestFn CheckGamma18Table = new TestFn()
	{
		public int run()
		{
			return CheckGammaFloatTable(1.8);
		}
	};
	
	private static final TestFn CheckGamma22Table = new TestFn()
	{
		public int run()
		{
			return CheckGammaFloatTable(2.2);
		}
	};
	
	private static final TestFn CheckGamma30Table = new TestFn()
	{
		public int run()
		{
			return CheckGammaFloatTable(3.0);
		}
	};
	
	// Create a curve from a table (which is a pure gamma function) and check it against the pow function.
	private static int CheckGammaWordTable(double g)
	{
	    short[] Values = new short[1025];
	    cmsToneCurve Curve; 
	    int i;
	    float in, out;
	    double val, Err;
	    
	    for (i=0; i <= 1024; i++)
	    {
	        in = (i / 1024.0f);
//#ifndef BlackBerrySDK4.5.0
	        Values[i] = (short)Math.floor(MathUtilities.pow(in, g) * 65535.0 + 0.5);
//#else
	        Values[i] = (short)Math.floor(Utility.pow(in, g) * 65535.0 + 0.5);
//#endif
	    }
	    
	    Curve = lcms2.cmsBuildTabulatedToneCurve16(DbgThread(), 1025, Values);
	    
	    MaxErr = 0.0;
	    for (i=0; i <= 0xffff; i++)
	    {
	        in = (i / 65535.0f);
	        out = lcms2.cmsEvalToneCurveFloat(Curve, in);
//#ifndef BlackBerrySDK4.5.0
	        val = MathUtilities.pow(in, g);
//#else
	        val = Utility.pow(in, g);
//#endif
	        
	        Err = Math.abs(val - out);
	        if (Err > MaxErr)
	        {
	        	MaxErr = Err;
	        }
	    }
	    
	    if (MaxErr > 0)
	    {
	    	Utility.fprintf(print, "|Err|<%lf ", new Object[]{new Double(MaxErr * 65535.0)});
	    }
	    
	    if (CheckGammaEstimation(Curve, g) == 0)
	    {
	    	return 0;
	    }
	    
	    lcms2.cmsFreeToneCurve(Curve);
	    return 1;
	}
	
	private static final TestFn CheckGamma18TableWord = new TestFn()
	{
		public int run()
		{
			return CheckGammaWordTable(1.8);
		}
	};
	
	private static final TestFn CheckGamma22TableWord = new TestFn()
	{
		public int run()
		{
			return CheckGammaWordTable(2.2);
		}
	};
	
	private static final TestFn CheckGamma30TableWord = new TestFn()
	{
		public int run()
		{
			return CheckGammaWordTable(3.0);
		}
	};
	
	// Curve joining test. Joining two high-gamma of 3.0 curves should
	// give something like linear
	private static final TestFn CheckJointCurves = new TestFn()
	{
		public int run()
		{
			cmsToneCurve Forward, Reverse, Result;
		    boolean rc;
		    
		    Forward = lcms2.cmsBuildGamma(DbgThread(), 3.0);
		    Reverse = lcms2.cmsBuildGamma(DbgThread(), 3.0);
		    
		    Result = lcms2.cmsJoinToneCurve(DbgThread(), Forward, Reverse, 256);
		    
		    lcms2.cmsFreeToneCurve(Forward); lcms2.cmsFreeToneCurve(Reverse); 
		    
		    rc = lcms2.cmsIsToneCurveLinear(Result);
		    lcms2.cmsFreeToneCurve(Result); 
		    
		    if (!rc)
		    {
		    	Fail("Joining same curve twice does not result in a linear ramp", null);
		    }
		    
		    return rc ? 1 : 0;
		}
	};
	
	// Create a gamma curve by cheating the table
	private static cmsToneCurve GammaTableLinear(int nEntries, boolean Dir)
	{
	    int i;
	    cmsToneCurve g = lcms2.cmsBuildTabulatedToneCurve16(DbgThread(), nEntries, null);
	    
	    for (i=0; i < nEntries; i++)
	    {
	        int v = lcms2_internal._cmsQuantizeVal(i, nEntries);
	        
	        if (Dir)
	        {
	        	g.Table16[i] = (short)v;
	        }
	        else
	        {
	        	g.Table16[i] = (short)(0xFFFF - v);
	        }
	    }
	    
	    return g;
	}
	
	private static final TestFn CheckJointCurvesDescending = new TestFn()
	{
		public int run()
		{
			cmsToneCurve Forward, Reverse, Result;
		    int i, rc;
		    
		    Forward = lcms2.cmsBuildGamma(DbgThread(), 2.2); 
		    
		    // Fake the curve to be table-based
		    
		    for (i=0; i < 4096; i++)
		    {
		    	Forward.Table16[i] = (short)(0xffff - (Forward.Table16[i] & 0xFFFF));
		    }
		    Forward.Segments[0].Type = 0;
		    
		    Reverse = lcms2.cmsReverseToneCurve(Forward); 
		    
		    Result = lcms2.cmsJoinToneCurve(DbgThread(), Reverse, Reverse, 256);
		    
		    lcms2.cmsFreeToneCurve(Forward);
		    lcms2.cmsFreeToneCurve(Reverse);
		    
		    rc = lcms2.cmsIsToneCurveLinear(Result) ? 1 : 0;
		    lcms2.cmsFreeToneCurve(Result); 
		    
		    return rc;
		}
	};
	
	private static int CheckFToneCurvePoint(cmsToneCurve c, short Point, int Value)
	{
	    int Result;
	    
	    Result = (lcms2.cmsEvalToneCurve16(c, Point) & 0xFFFF);
	    
	    return (Math.abs(Value - Result) < 2) ? 1 : 0;
	}
	
	private static final TestFn CheckReverseDegenerated = new TestFn()
	{
		public int run()
		{
			cmsToneCurve p, g;
		    short[] Tab = new short[16];
		    
		    Tab[0] = 0;
		    Tab[1] = 0;
		    Tab[2] = 0;
		    Tab[3] = 0;
		    Tab[4] = 0;
		    Tab[5] = 0x5555;
		    Tab[6] = 0x6666;
		    Tab[7] = 0x7777;
		    Tab[8] = (short)0x8888;
		    Tab[9] = (short)0x9999;
		    Tab[10]= (short)0xffff;
		    Tab[11]= (short)0xffff;
		    Tab[12]= (short)0xffff;
		    Tab[13]= (short)0xffff;
		    Tab[14]= (short)0xffff;
		    Tab[15]= (short)0xffff;
		    
		    p = lcms2.cmsBuildTabulatedToneCurve16(DbgThread(), 16, Tab);
		    g = lcms2.cmsReverseToneCurve(p);
		    
		    // Now let's check some points
		    if (CheckFToneCurvePoint(g, (short)0x5555, 0x5555) == 0)
		    {
		    	return 0;
		    }
		    if (CheckFToneCurvePoint(g, (short)0x7777, 0x7777) == 0)
		    {
		    	return 0;
		    }
		    
		    // First point for zero
		    if (CheckFToneCurvePoint(g, (short)0x0000, 0x4444) == 0)
		    {
		    	return 0;
		    }
		    
		    // Last point
		    if (CheckFToneCurvePoint(g, (short)0xFFFF, 0xFFFF) == 0)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsFreeToneCurve(p);
		    lcms2.cmsFreeToneCurve(g);
		    
		    return 1;
		}
	};
	
	// Build a parametric sRGB-like curve
	private static cmsToneCurve Build_sRGBGamma()
	{
	    double[] Parameters = new double[5];
	    
	    Parameters[0] = 2.4;
	    Parameters[1] = 1. / 1.055;
	    Parameters[2] = 0.055 / 1.055;
	    Parameters[3] = 1. / 12.92;
	    Parameters[4] = 0.04045;    // d
	    
	    return lcms2.cmsBuildParametricToneCurve(DbgThread(), 4, Parameters);
	}
	
	// Join two gamma tables in floting point format. Result should be a straight line
	private static cmsToneCurve CombineGammaFloat(cmsToneCurve g1, cmsToneCurve g2)
	{
	    short[] Tab = new short[256];
	    float f;
	    int i;
	    
	    for (i=0; i < 256; i++)
	    {
	        f = i / 255.0F;
	        f = lcms2.cmsEvalToneCurveFloat(g2, lcms2.cmsEvalToneCurveFloat(g1, f));
	        
	        Tab[i] = (short)Math.floor(f * 65535.0 + 0.5);
	    }
	    
	    return  lcms2.cmsBuildTabulatedToneCurve16(DbgThread(), 256, Tab);
	}
	
	// Same of anterior, but using quantized tables
	private static cmsToneCurve CombineGamma16(cmsToneCurve g1, cmsToneCurve g2)
	{
	    short[] Tab = new short[256];
	    
	    int i;
	    
	    for (i=0; i < 256; i++)
	    {
	        short wValIn;
	        
	        wValIn = lcms2_internal._cmsQuantizeVal(i, 256);     
	        Tab[i] = lcms2.cmsEvalToneCurve16(g2, lcms2.cmsEvalToneCurve16(g1, wValIn));
	    }
	    
	    return lcms2.cmsBuildTabulatedToneCurve16(DbgThread(), 256, Tab);
	}
	
	private static final TestFn CheckJointFloatCurves_sRGB = new TestFn()
	{
		public int run()
		{
			cmsToneCurve Forward, Reverse, Result;
		    boolean rc;
		    
		    Forward = Build_sRGBGamma();
		    Reverse = lcms2.cmsReverseToneCurve(Forward);
		    Result = CombineGammaFloat(Forward, Reverse);
		    lcms2.cmsFreeToneCurve(Forward); lcms2.cmsFreeToneCurve(Reverse); 
		    
		    rc = lcms2.cmsIsToneCurveLinear(Result);
		    lcms2.cmsFreeToneCurve(Result); 
		    
		    return rc ? 1 : 0;
		}
	};
	
	private static final TestFn CheckJoint16Curves_sRGB = new TestFn()
	{
		public int run()
		{
			cmsToneCurve Forward, Reverse, Result;
		    boolean rc;
		    
		    Forward = Build_sRGBGamma();
		    Reverse = lcms2.cmsReverseToneCurve(Forward);
		    Result = CombineGamma16(Forward, Reverse);
		    lcms2.cmsFreeToneCurve(Forward); lcms2.cmsFreeToneCurve(Reverse); 
		    
		    rc = lcms2.cmsIsToneCurveLinear(Result);
		    lcms2.cmsFreeToneCurve(Result); 
		    
		    return rc ? 1 : 0;
		}
	};
	
	// sigmoidal curve f(x) = (1-x^g) ^(1/g)
	
	private static final TestFn CheckJointCurvesSShaped = new TestFn()
	{
		public int run()
		{
			double[] p = new double[]{3.2};
		    cmsToneCurve Forward, Reverse, Result;
		    int rc;
		    
		    Forward = lcms2.cmsBuildParametricToneCurve(DbgThread(), 108, p);    
		    Reverse = lcms2.cmsReverseToneCurve(Forward);
		    Result = lcms2.cmsJoinToneCurve(DbgThread(), Forward, Forward, 4096);
		    
		    lcms2.cmsFreeToneCurve(Forward);
		    lcms2.cmsFreeToneCurve(Reverse);
		    
		    rc = lcms2.cmsIsToneCurveLinear(Result) ? 1 : 0;
		    lcms2.cmsFreeToneCurve(Result); 
		    return rc;
		}
	};
	
	// --------------------------------------------------------------------------------------------------------
	
	// Implementation of some tone curve functions
	private static final dblfnptr Gamma = new dblfnptr()
	{
		public float run(float x, double[] Params)
		{
//#ifndef BlackBerrySDK4.5.0
			return (float)MathUtilities.pow(x, Params[0]);
//#else
			return (float)Utility.pow(x, Params[0]);
//#endif
		}
	};
	
	private static final dblfnptr CIE122 = new dblfnptr()
	{
		public float run(float x, double[] Params)
		{
			double e, Val;
			
		    if (x >= -Params[2] / Params[1])
		    {
		        e = Params[1]*x + Params[2];
		        
		        if (e > 0)
		        {
//#ifndef BlackBerrySDK4.5.0
		        	Val = MathUtilities.pow(e, Params[0]);
//#else
		        	Val = Utility.pow(e, Params[0]);
//#endif
		        }
		        else
		        {
		        	Val = 0;
		        }
		    }
		    else
		    {
		    	Val = 0;
		    }
		    
		    return (float)Val;
		}
	};
	
	private static final dblfnptr IEC61966_3 = new dblfnptr()
	{
		public float run(float x, double[] Params)
		{
			double e, Val;
			
		    if (x >= -Params[2] / Params[1])
		    {
		        e = Params[1]*x + Params[2];
		        
		        if (e > 0)
		        {
//#ifndef BlackBerrySDK4.5.0
		        	Val = MathUtilities.pow(e, Params[0]) + Params[3];
//#else
		        	Val = Utility.pow(e, Params[0]) + Params[3];
//#endif
		        }
		        else
		        {
		        	Val = 0;
		        }
		    }
		    else
		    {
		    	Val = Params[3];
		    }
		    
		    return (float)Val;
		}
	};
	
	private static final dblfnptr IEC61966_21 = new dblfnptr()
	{
		public float run(float x, double[] Params)
		{
			double e, Val;
			
		    if (x >= Params[4])
		    {
		        e = Params[1]*x + Params[2];
		        
		        if (e > 0)
		        {
//#ifndef BlackBerrySDK4.5.0
		        	Val = MathUtilities.pow(e, Params[0]);
//#else
		        	Val = Utility.pow(e, Params[0]);
//#endif
		        }
		        else
		        {
		        	Val = 0;
		        }
		    }
		    else
		    {
		    	Val = x * Params[3];
		    }
		    
		    return (float)Val;
		}
	};
	
	private static final dblfnptr param_5 = new dblfnptr()
	{
		public float run(float x, double[] Params)
		{
			double e, Val;
			// Y = (aX + b)^Gamma + e | X >= d
		    // Y = cX + f             | else
		    if (x >= Params[4])
		    {
		        e = Params[1]*x + Params[2];
		        
		        if (e > 0)
		        {
//#ifndef BlackBerrySDK4.5.0
		        	Val = MathUtilities.pow(e, Params[0]) + Params[5];
//#else
		        	Val = Utility.pow(e, Params[0]) + Params[5];
//#endif
		        }
		        else
		        {
		        	Val = 0;
		        }
		    }
		    else
		    {
		    	Val = x*Params[3] + Params[6];
		    }
		    
		    return (float)Val;
		}
	};
	
	private static final dblfnptr param_6 = new dblfnptr()
	{
		public float run(float x, double[] Params)
		{
			double e, Val;
			
			e = Params[1]*x + Params[2];
		    if (e > 0)
		    {
//#ifndef BlackBerrySDK4.5.0
		    	Val = MathUtilities.pow(e, Params[0]) + Params[3];
//#else
		    	Val = Utility.pow(e, Params[0]) + Params[3];
//#endif
		    }
		    else
		    {
		    	Val = 0;
		    }
		    
		    return (float)Val;
		}
	};
	
	private static final dblfnptr param_7 = new dblfnptr()
	{
		public float run(float x, double[] Params)
		{
			double Val;
			
//#ifndef BlackBerrySDK4.5.0
			Val = Params[1]*Utility.log10(Params[2] * MathUtilities.pow(x, Params[0]) + Params[3]) + Params[4];
//#else
			Val = Params[1]*Utility.log10(Params[2] * Utility.pow(x, Params[0]) + Params[3]) + Params[4];
//#endif
			
		    return (float)Val;
		}
	};
	
	private static final dblfnptr param_8 = new dblfnptr()
	{
		public float run(float x, double[] Params)
		{
			double Val;
			
//#ifndef BlackBerrySDK4.5.0
			Val = (Params[0] * MathUtilities.pow(Params[1], Params[2] * x + Params[3]) + Params[4]);
//#else
			Val = (Params[0] * Utility.pow(Params[1], Params[2] * x + Params[3]) + Params[4]);
//#endif
			
		    return (float)Val;
		}
	};
	
	private static final dblfnptr sigmoidal = new dblfnptr()
	{
		public float run(float x, double[] Params)
		{
			double Val;
			
//#ifndef BlackBerrySDK4.5.0
			Val = MathUtilities.pow(1.0 - MathUtilities.pow(1 - x, 1/Params[0]), 1/Params[0]);
//#else
			Val = Utility.pow(1.0 - Utility.pow(1 - x, 1/Params[0]), 1/Params[0]);
//#endif
			
		    return (float)Val;
		}
	};
	
	private static boolean CheckSingleParametric(final String Name, dblfnptr fn, int Type, final double[] Params)
	{
	    int i;
	    cmsToneCurve tc;
	    cmsToneCurve tc_1;
	    StringBuffer InverseText = new StringBuffer(256);
	    
	    tc = lcms2.cmsBuildParametricToneCurve(DbgThread(), Type, Params);
	    tc_1 = lcms2.cmsBuildParametricToneCurve(DbgThread(), -Type, Params);
	    
	    for (i=0; i <= 1000; i++)
	    {
	        float x = i / 1000f;
	        float y_fn, y_param, x_param, y_param2;
	        
	        y_fn = fn.run(x, Params);
	        y_param = lcms2.cmsEvalToneCurveFloat(tc, x);
	        x_param = lcms2.cmsEvalToneCurveFloat(tc_1, y_param);
	        
	        y_param2 = fn.run(x_param, Params);
	        
	        if (!IsGoodVal(Name, y_fn, y_param, FIXED_PRECISION_15_16))
	        {
	        	lcms2.cmsFreeToneCurve(tc);
	    	    lcms2.cmsFreeToneCurve(tc_1);
	    	    return false;
	        }
	        
	        Utility.sprintf(InverseText, "Inverse %s", new Object[]{Name});
	        if (!IsGoodVal(InverseText.toString(), y_fn, y_param2, FIXED_PRECISION_15_16))
	        {
	        	lcms2.cmsFreeToneCurve(tc);
	    	    lcms2.cmsFreeToneCurve(tc_1);
	    	    return false;
	        }
	    }

	    lcms2.cmsFreeToneCurve(tc);
	    lcms2.cmsFreeToneCurve(tc_1);
	    return true;
	}
	
	// Check against some known values
	private static final TestFn CheckParametricToneCurves = new TestFn()
	{
		public int run()
		{
			double[] Params = new double[10];
			
			// 1) X = Y ^ Gamma
			
			Params[0] = 2.2;
			
			if (!CheckSingleParametric("Gamma", Gamma, 1, Params))
			{
				return 0;
			}
			
			// 2) CIE 122-1966
			// Y = (aX + b)^Gamma  | X >= -b/a
			// Y = 0               | else
			
			Params[0] = 2.2;
			Params[1] = 1.5;
			Params[2] = -0.5;
			
			if (!CheckSingleParametric("CIE122-1966", CIE122, 2, Params))
			{
				return 0;
			}
			
			// 3) IEC 61966-3
			// Y = (aX + b)^Gamma | X <= -b/a
			// Y = c              | else
			
			Params[0] = 2.2;
			Params[1] = 1.5;
			Params[2] = -0.5;
			Params[3] = 0.3;
			
			if (!CheckSingleParametric("IEC 61966-3", IEC61966_3, 3, Params))
			{
				return 0;
			}
			
			// 4) IEC 61966-2.1 (sRGB)
			// Y = (aX + b)^Gamma | X >= d
			// Y = cX             | X < d
			
			Params[0] = 2.4;
			Params[1] = 1. / 1.055;
			Params[2] = 0.055 / 1.055;
			Params[3] = 1. / 12.92;
			Params[4] = 0.04045;
			
			if (!CheckSingleParametric("IEC 61966-2.1", IEC61966_21, 4, Params))
			{
				return 0;
			}
			
			// 5) Y = (aX + b)^Gamma + e | X >= d
			// Y = cX + f             | else
			
			Params[0] = 2.2;
			Params[1] = 0.7;
			Params[2] = 0.2;
			Params[3] = 0.3;
			Params[4] = 0.1;
			Params[5] = 0.5;
			Params[6] = 0.2;
			
			if (!CheckSingleParametric("param_5", param_5, 5, Params))
			{
				return 0;
			}
			
			// 6) Y = (aX + b) ^ Gamma + c
			
			Params[0] = 2.2;
			Params[1] = 0.7;
			Params[2] = 0.2;
			Params[3] = 0.3;
			
			if (!CheckSingleParametric("param_6", param_6, 6, Params))
			{
				return 0;
			}
			
			// 7) Y = a * log (b * X^Gamma + c) + d
			
			Params[0] = 2.2;
			Params[1] = 0.9;
			Params[2] = 0.9;
			Params[3] = 0.02;
			Params[4] = 0.1;
			
			if (!CheckSingleParametric("param_7", param_7, 7, Params))
			{
				return 0;
			}
			
			// 8) Y = a * b ^ (c*X+d) + e
			
			Params[0] = 0.9;
			Params[1] = 0.9;
			Params[2] = 1.02;
			Params[3] = 0.1;
			Params[4] = 0.2;
			
			if (!CheckSingleParametric("param_8", param_8, 8, Params))
			{
				return 0;
			}
			
			// 108: S-Shaped: (1 - (1-x)^1/g)^1/g
			
			Params[0] = 1.9;
			if (!CheckSingleParametric("sigmoidal", sigmoidal, 108, Params))
			{
				return 0;
			}
			
			// All OK
			
			return 1;
		}
	};
	
	// LUT checks ------------------------------------------------------------------------------
	
	private static final TestFn CheckLUTcreation = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut;
		    cmsPipeline lut2;
		    int n1, n2;
		    
		    lut = lcms2.cmsPipelineAlloc(DbgThread(), 1, 1);
		    n1 = lcms2.cmsPipelineStageCount(lut);
		    lut2 = lcms2.cmsPipelineDup(lut);
		    n2 = lcms2.cmsPipelineStageCount(lut2);
		    
		    lcms2.cmsPipelineFree(lut);
		    lcms2.cmsPipelineFree(lut2);
		    
		    return ((n1 == 0) && (n2 == 0)) ? 1 : 0;
		}
	};
	
	// Create a MPE for a identity matrix
	private static void AddIdentityMatrix(cmsPipeline lut)
	{
	    final double[] Identity = { 1, 0, 0,
	                          0, 1, 0, 
	                          0, 0, 1, 
	                          0, 0, 0 };

	    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2.cmsStageAllocMatrix(DbgThread(), 3, 3, Identity, null));
	}
	
	// Create a MPE for identity cmsFloat32Number CLUT
	private static void AddIdentityCLUTfloat(cmsPipeline lut)
	{
	    final float[] Table = { 
	        0,    0,    0,    
	        0,    0,    1.0f,
	        
	        0,    1.0f,    0, 
	        0,    1.0f,    1.0f,
	        
	        1.0f,    0,    0,    
	        1.0f,    0,    1.0f,
	        
	        1.0f,    1.0f,    0, 
	        1.0f,    1.0f,    1.0f
	    };

	    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2.cmsStageAllocCLutFloat(DbgThread(), 2, 3, 3, Table));
	}
	
	// Create a MPE for identity cmsFloat32Number CLUT
	private static void AddIdentityCLUT16(cmsPipeline lut)
	{
	    final short[] Table = { 
	        0,    0,    0,    
	        0,    0,    (short)0xffff,
	        
	        0,    (short)0xffff,    0, 
	        0,    (short)0xffff,    (short)0xffff,
	        
	        (short)0xffff,    0,    0,    
	        (short)0xffff,    0,    (short)0xffff,
	        
	        (short)0xffff,    (short)0xffff,    0, 
	        (short)0xffff,    (short)0xffff,    (short)0xffff
	    };
	    
	    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2.cmsStageAllocCLut16bit(DbgThread(), 2, 3, 3, Table));
	}
	
	// Create a 3 fn identity curves
	
	private static void Add3GammaCurves(cmsPipeline lut, double Curve)
	{
	    cmsToneCurve id = lcms2.cmsBuildGamma(DbgThread(), Curve);
	    cmsToneCurve[] id3 = new cmsToneCurve[3];
	    
	    id3[0] = id;
	    id3[1] = id;
	    id3[2] = id;
	    
	    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2.cmsStageAllocToneCurves(DbgThread(), 3, id3));
	    
	    lcms2.cmsFreeToneCurve(id);
	}
	
	private static int CheckFloatLUT(cmsPipeline lut)
	{
	    int n1, i, j;
	    float[] Inf = new float[3], Outf = new float[3];
	    
	    n1=0;
	    
	    for (j=0; j < 65535; j++)
	    {
	        int[] af = new int[3];
	        
	        Inf[0] = Inf[1] = Inf[2] = j / 65535.0F;
	        lcms2.cmsPipelineEvalFloat(Inf, Outf, lut);
	        
	        af[0] = (int)Math.floor(Outf[0]*65535.0 + 0.5);
	        af[1] = (int)Math.floor(Outf[1]*65535.0 + 0.5);
	        af[2] = (int)Math.floor(Outf[2]*65535.0 + 0.5);
	        
	        for (i=0; i < 3; i++)
	        {
	            if (af[i] != j)
	            {
	                n1++;
	            }
	        }
	    }
	    
	    return (n1 == 0) ? 1 : 0;
	}
	
	private static int Check16LUT(cmsPipeline lut)
	{
	    int n2, i, j;
	    short[] Inw = new short[3], Outw = new short[3];
	    
	    n2=0;
	    
	    for (j=0; j < 65535; j++)
	    {
	        int[] aw = new int[3];

	        Inw[0] = Inw[1] = Inw[2] = (short)j;
	        lcms2.cmsPipelineEval16(Inw, Outw, lut);
	        aw[0] = Outw[0];
	        aw[1] = Outw[1];
	        aw[2] = Outw[2];
	        
	        for (i=0; i < 3; i++)
	        {
	        	if ((aw[i] & 0xFFFF) != j)
	        	{
	        		n2++;
	        	}
	        }
	    }
	    
	    return (n2 == 0) ? 1 : 0;
	}
	
	// Check any LUT that is linear
	private static int CheckStagesLUT(cmsPipeline lut, int ExpectedStages)
	{
	    int nInpChans, nOutpChans, nStages;
	    
	    nInpChans  = lcms2.cmsPipelineInputChannels(lut);
	    nOutpChans = lcms2.cmsPipelineOutputChannels(lut);
	    nStages    = lcms2.cmsPipelineStageCount(lut);
	    
	    return ((nInpChans == 3) && (nOutpChans == 3) && (nStages == ExpectedStages)) ? 1 : 0;
	}
	
	private static int CheckFullLUT(cmsPipeline lut, int ExpectedStages)
	{
	    int rc = (CheckStagesLUT(lut, ExpectedStages) != 0 && Check16LUT(lut) != 0 && CheckFloatLUT(lut) != 0) ? 1 : 0;
	    
	    lcms2.cmsPipelineFree(lut);
	    return rc;
	}
	
	private static final TestFn Check1StageLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    return CheckFullLUT(lut, 1);
		}
	};
	
	private static final TestFn Check2StageLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    AddIdentityCLUTfloat(lut);
		    
		    return CheckFullLUT(lut, 2);
		}
	};
	
	private static final TestFn Check2Stage16LUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    AddIdentityCLUT16(lut);
		    
		    return CheckFullLUT(lut, 2);
		}
	};
	
	private static final TestFn Check3StageLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    AddIdentityCLUTfloat(lut);
		    Add3GammaCurves(lut, 1.0);
		    
		    return CheckFullLUT(lut, 3);
		}
	};
	
	private static final TestFn Check3Stage16LUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    AddIdentityCLUT16(lut);
		    Add3GammaCurves(lut, 1.0);
		    
		    return CheckFullLUT(lut, 3);
		}
	};
	
	private static final TestFn Check4StageLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    AddIdentityCLUTfloat(lut);
		    Add3GammaCurves(lut, 1.0);
		    AddIdentityMatrix(lut);
		    
		    return CheckFullLUT(lut, 4);
		}
	};
	
	private static final TestFn Check4Stage16LUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    AddIdentityCLUT16(lut);
		    Add3GammaCurves(lut, 1.0);
		    AddIdentityMatrix(lut);
		    
		    return CheckFullLUT(lut, 4);
		}
	};
	
	private static final TestFn Check5StageLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    AddIdentityCLUTfloat(lut);
		    Add3GammaCurves(lut, 1.0);
		    AddIdentityMatrix(lut);
		    Add3GammaCurves(lut, 1.0);
		    
		    return CheckFullLUT(lut, 5);
		}
	};
	
	private static final TestFn Check5Stage16LUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    AddIdentityCLUT16(lut);
		    Add3GammaCurves(lut, 1.0);
		    AddIdentityMatrix(lut);
		    Add3GammaCurves(lut, 1.0);
		    
		    return CheckFullLUT(lut, 5);
		}
	};
	
	private static final TestFn Check6StageLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    Add3GammaCurves(lut, 1.0);
		    AddIdentityCLUTfloat(lut);
		    Add3GammaCurves(lut, 1.0);
		    AddIdentityMatrix(lut);
		    Add3GammaCurves(lut, 1.0);
		    
		    return CheckFullLUT(lut, 6);
		}
	};
	
	private static final TestFn Check6Stage16LUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
			
		    AddIdentityMatrix(lut);
		    Add3GammaCurves(lut, 1.0);
		    AddIdentityCLUT16(lut);
		    Add3GammaCurves(lut, 1.0);
		    AddIdentityMatrix(lut);
		    Add3GammaCurves(lut, 1.0);
		    
		    return CheckFullLUT(lut, 6);
		}
	};
	
	private static final TestFn CheckLab2LabLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
		    int rc;
		    
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocLab2XYZ(DbgThread()));
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocXYZ2Lab(DbgThread()));
		    
		    rc = (CheckFloatLUT(lut) != 0 && CheckStagesLUT(lut, 2) != 0) ? 1 : 0;
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return rc;
		}
	};
	
	private static final TestFn CheckXYZ2XYZLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
		    int rc;
		    
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocXYZ2Lab(DbgThread()));
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocLab2XYZ(DbgThread()));
		    
		    rc = (CheckFloatLUT(lut) != 0 && CheckStagesLUT(lut, 2) != 0) ? 1 : 0;
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return rc;
		}
	};
	
	private static final TestFn CheckLab2LabMatLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
		    int rc;
		    
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocLab2XYZ(DbgThread()));
		    AddIdentityMatrix(lut);
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocXYZ2Lab(DbgThread()));
		    
		    rc = (CheckFloatLUT(lut) != 0 && CheckStagesLUT(lut, 3) != 0) ? 1 : 0;
		    
		    lcms2.cmsPipelineFree(lut);
		    
		    return rc;
		}
	};
	
	private static final TestFn CheckNamedColorLUT = new TestFn()
	{
		public int run()
		{
			cmsPipeline lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
		    cmsNAMEDCOLORLIST nc;
		    int i,j, rc = 1, n2;
		    short[] PCS = new short[3];
		    short[] Colorant = new short[lcms2.cmsMAXCHANNELS];
		    StringBuffer Name = new StringBuffer(255);
		    short[] Inw = new short[3], Outw = new short[3];
		    
		    nc = lcms2.cmsAllocNamedColorList(DbgThread(), 256, 3, "pre", "post");
		    if (nc == null)
		    {
		    	return 0;
		    }
		    
		    for (i=0; i < 256; i++)
		    {
		        PCS[0] = PCS[1] = PCS[2] = (short) i;
		        Colorant[0] = Colorant[1] = Colorant[2] = Colorant[3] = (short) i;
		        
		        Utility.sprintf(Name, "#%d", new Object[]{new Integer(i)});
		        if (!lcms2.cmsAppendNamedColor(nc, Name.toString(), PCS, Colorant))
		        {
		        	rc = 0;
		        	break;
		        }
		    }
		    
		    lcms2.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocNamedColor(nc));
		    
		    lcms2.cmsFreeNamedColorList(nc);
		    if (rc == 0)
		    {
		    	return 0;
		    }
		    
		    n2=0;
		    
		    for (j=0; j < 256; j++)
		    {
		        Inw[0] = (short) j;
		        
		        lcms2.cmsPipelineEval16(Inw, Outw, lut);
		        for (i=0; i < 3; i++)
		        {
		        	if (Outw[i] != j)
		        	{
		                n2++;
		            }
		        }
		    }
		    
		    lcms2.cmsPipelineFree(lut);
		    return (n2 == 0) ? 1 : 0;
		}
	};
	
	// --------------------------------------------------------------------------------------------
	
	private static int strcmp(StringBuffer b1, StringBuffer b2)
	{
		String s1 = b1.toString().trim();
		String s2 = b2.toString().trim();
		int in = s1.indexOf('\0');
		if(in >= 0)
		{
			s1 = s1.substring(0, in);
		}
		in = s2.indexOf('\0');
		if(in >= 0)
		{
			s2 = s2.substring(0, in);
		}
		return s1.compareTo(s2);
	}
	
	private static int strcmp(StringBuffer b1, String b2)
	{
		return strcmp(b1, new StringBuffer(b2));
	}
	
	// A lightweight test of multilocalized unicode structures.
	
	private static final TestFn CheckMLU = new TestFn()
	{
		public int run()
		{
			cmsMLU mlu, mlu2, mlu3;
		    StringBuffer Buffer = new StringBuffer(256), Buffer2 = new StringBuffer(256);
		    int rc = 1;
		    int i;
		    cmsHPROFILE h= null;
		    
		    // Allocate a MLU structure, no preferred size
		    mlu = lcms2.cmsMLUalloc(DbgThread(), 0);
		    
		    // Add some localizations
		    lcms2.cmsMLUsetWide(mlu, "en", "US", "Hello, world");
		    lcms2.cmsMLUsetWide(mlu, "es", "ES", "Hola, mundo");
		    lcms2.cmsMLUsetWide(mlu, "fr", "FR", "Bonjour, le monde");
		    lcms2.cmsMLUsetWide(mlu, "ca", "CA", "Hola, mon");
		    
		    // Check the returned string for each language
		    
		    lcms2.cmsMLUgetASCII(mlu, "en", "US", Buffer, 256);
		    if (strcmp(Buffer, "Hello, world") != 0)
		    {
		    	rc = 0;
		    }
		    
		    lcms2.cmsMLUgetASCII(mlu, "es", "ES", Buffer, 256);
		    if (strcmp(Buffer, "Hola, mundo") != 0)
		    {
		    	rc = 0;
		    }
		    
		    lcms2.cmsMLUgetASCII(mlu, "fr", "FR", Buffer, 256);
		    if (strcmp(Buffer, "Bonjour, le monde") != 0)
		    {
		    	rc = 0;
		    }
		    
		    lcms2.cmsMLUgetASCII(mlu, "ca", "CA", Buffer, 256);
		    if (strcmp(Buffer, "Hola, mon") != 0)
		    {
		    	rc = 0;
		    }
		    
		    if (rc == 0)
		    {
		    	Fail("Unexpected string '%s'", new Object[]{Buffer});
		    }
		    
		    // So far, so good.
		    lcms2.cmsMLUfree(mlu);
		    
		    // Now for performance, allocate an empty struct
		    mlu = lcms2.cmsMLUalloc(DbgThread(), 0);
		    
		    // Fill it with several thousands of different languages
		    for (i=0; i < 4096; i++)
		    {
		        char[] Lang = new char[3];
		        
		        Lang[0] = (char) (i % 255);
		        Lang[1] = (char) (i / 255);
		        Lang[2] = 0;
		        
		        Utility.sprintf(Buffer, "String #%i", new Object[]{new Integer(i)});
		        lcms2.cmsMLUsetASCII(mlu, new String(Lang), new String(Lang), Buffer.toString());
		    }
		    
		    // Duplicate it
		    mlu2 = lcms2.cmsMLUdup(mlu);
		    
		    // Get rid of original
		    lcms2.cmsMLUfree(mlu);
		    
		    // Check all is still in place
		    for (i=0; i < 4096; i++)
		    {
		        char[] Lang = new char[3];
		        
		        Lang[0] = (char)(i % 255);
		        Lang[1] = (char)(i / 255);
		        Lang[2] = 0;
		        
		        lcms2.cmsMLUgetASCII(mlu2, new String(Lang), new String(Lang), Buffer2, 256);
		        Utility.sprintf(Buffer, "String #%i", new Object[]{new Integer(i)});
		        
		        if (strcmp(Buffer, Buffer2) != 0)
		        {
		        	rc = 0;
		        	break;
		        }
		    }
		    
		    if (rc == 0)
		    {
		    	Fail("Unexpected string '%s'", new Object[]{Buffer2});
		    }
		    
		    // Check profile IO
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "mlucheck.icc", "w");
		    
		    lcms2.cmsSetProfileVersion(h, 4.2);
		    
		    lcms2.cmsWriteTag(h, lcms2.cmsSigProfileDescriptionTag, mlu2);
		    lcms2.cmsCloseProfile(h);
		    lcms2.cmsMLUfree(mlu2);
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "mlucheck.icc", "r");
		    
		    mlu3 = (cmsMLU)lcms2.cmsReadTag(h, lcms2.cmsSigProfileDescriptionTag);
		    if (mlu3 == null)
		    {
		    	Fail("Profile didn't get the MLU\n", null);
		    	rc = 0;
		    	if (h != null)
		    	{
		    		lcms2.cmsCloseProfile(h);
		    	}
			    fileOperation("mlucheck.icc", remove);

			    return rc;
		    }
		    
		    // Check all is still in place
		    for (i=0; i < 4096; i++)
		    {
		        char[] Lang = new char[3];
		        
		        Lang[0] = (char) (i % 255);
		        Lang[1] = (char) (i / 255);
		        Lang[2] = 0;
		        
		        lcms2.cmsMLUgetASCII(mlu3, new String(Lang), new String(Lang), Buffer2, 256);
		        Utility.sprintf(Buffer, "String #%i", new Object[]{new Integer(i)});
		        
		        if (strcmp(Buffer, Buffer2) != 0)
		        {
		        	rc = 0;
		        	break;
		        }
		    }
		    
		    if (rc == 0)
		    {
		    	Fail("Unexpected string '%s'", new Object[]{Buffer2});
		    }
		    
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		    }
		    fileOperation("mlucheck.icc", remove);

		    return rc;
		}
	};
	
	// A lightweight test of named color structures.
	private static final TestFn CheckNamedColorList = new TestFn()
	{
		public int run()
		{
			cmsNAMEDCOLORLIST nc = null, nc2;
		    int i, j, rc=1;
		    StringBuffer Name = new StringBuffer(255);
		    short[] PCS = new short[3];
		    short[] Colorant = new short[lcms2.cmsMAXCHANNELS];
		    StringBuffer CheckName = new StringBuffer(255);
		    short[] CheckPCS = new short[3];
		    short[] CheckColorant = new short[lcms2.cmsMAXCHANNELS];
		    cmsHPROFILE h;
		    
		    nc = lcms2.cmsAllocNamedColorList(DbgThread(), 0, 4, "prefix", "suffix");
		    if (nc == null)
		    {
		    	return 0;
		    }
		    
		    for (i=0; i < 4096; i++)
		    {
		        PCS[0] = PCS[1] = PCS[2] = (short) i;
		        Colorant[0] = Colorant[1] = Colorant[2] = Colorant[3] = (short) (4096 - i);
		        
		        Utility.sprintf(Name, "#%d", new Object[]{new Integer(i)});
		        if (!lcms2.cmsAppendNamedColor(nc, Name.toString(), PCS, Colorant))
		        {
		        	rc = 0;
		        	break;
		        }
		    }
		    
		    for (i=0; i < 4096; i++)
		    {
		        CheckPCS[0] = CheckPCS[1] = CheckPCS[2] = (short) i;
		        CheckColorant[0] = CheckColorant[1] = CheckColorant[2] = CheckColorant[3] = (short) (4096 - i);
		        
		        Utility.sprintf(CheckName, "#%d", new Object[]{new Integer(i)});
		        if (!lcms2.cmsNamedColorInfo(nc, i, Name, null, null, PCS, Colorant))
		        {
		        	rc = 0;
		        	if (nc != null)
				    {
				    	lcms2.cmsFreeNamedColorList(nc);
				    }
				    return rc;
		        }
		        
		        for (j=0; j < 3; j++)
		        {
		            if (CheckPCS[j] != PCS[j])
		            {
		            	rc = 0;
		            	Fail("Invalid PCS", null);
		            	if (nc != null)
		    		    {
		    		    	lcms2.cmsFreeNamedColorList(nc);
		    		    }
		    		    return rc;
		            }
		        }
		        
		        for (j=0; j < 4; j++)
		        {
		            if (CheckColorant[j] != Colorant[j])
		            {
		            	rc = 0;
		            	Fail("Invalid Colorant", null);
		            	if (nc != null)
		    		    {
		    		    	lcms2.cmsFreeNamedColorList(nc);
		    		    }
		    		    return rc;
		            };
		        }
		        
		        if (strcmp(Name, CheckName) != 0)
		        {
		        	rc = 0;
		        	Fail("Invalid Name", null);
		        	if (nc != null)
				    {
				    	lcms2.cmsFreeNamedColorList(nc);
				    }
				    return rc;
		        };
		    }
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "namedcol.icc", "w");
		    if (h == null)
		    {
		    	return 0;
		    }
		    if (!lcms2.cmsWriteTag(h, lcms2.cmsSigNamedColor2Tag, nc))
		    {
		    	return 0;
		    }
		    lcms2.cmsCloseProfile(h);
		    lcms2.cmsFreeNamedColorList(nc);
		    nc = null;
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "namedcol.icc","r");
		    nc2 = (cmsNAMEDCOLORLIST)lcms2.cmsReadTag(h, lcms2.cmsSigNamedColor2Tag);
		    
		    if (lcms2.cmsNamedColorCount(nc2) != 4096)
		    {
		    	rc = 0;
		    	Fail("Invalid count", null);
		    	if (nc != null)
			    {
			    	lcms2.cmsFreeNamedColorList(nc);
			    }
			    return rc;
		    }
		    
		    i = lcms2.cmsNamedColorIndex(nc2, "#123");
		    if (i != 123)
		    {
		    	rc = 0;
		    	Fail("Invalid index", null);
		    	if (nc != null)
			    {
			    	lcms2.cmsFreeNamedColorList(nc);
			    }
			    return rc;
		    }
		    
		    for (i=0; i < 4096; i++)
		    {
		        CheckPCS[0] = CheckPCS[1] = CheckPCS[2] = (short) i;
		        CheckColorant[0] = CheckColorant[1] = CheckColorant[2] = CheckColorant[3] = (short) (4096 - i);
		        
		        Utility.sprintf(CheckName, "#%d", new Object[]{new Integer(i)});
		        if (!lcms2.cmsNamedColorInfo(nc2, i, Name, null, null, PCS, Colorant))
		        {
		        	rc = 0;
		        	if (nc != null)
				    {
				    	lcms2.cmsFreeNamedColorList(nc);
				    }
				    return rc;
		        }
		        
		        for (j=0; j < 3; j++)
		        {
		            if (CheckPCS[j] != PCS[j])
		            {
		            	rc = 0;
		            	Fail("Invalid PCS", null);
		            	if (nc != null)
		    		    {
		    		    	lcms2.cmsFreeNamedColorList(nc);
		    		    }
		    		    return rc;
		            }
		        }
		        
		        for (j=0; j < 4; j++)
		        {
		            if (CheckColorant[j] != Colorant[j])
		            {
		            	rc = 0;
		            	Fail("Invalid Colorant", null);
		            	if (nc != null)
		    		    {
		    		    	lcms2.cmsFreeNamedColorList(nc);
		    		    }
		    		    return rc;
		            };
		        }
		        
		        if (strcmp(Name, CheckName) != 0)
		        {
		        	rc = 0;
		        	Fail("Invalid Name", null);
		        	if (nc != null)
				    {
				    	lcms2.cmsFreeNamedColorList(nc);
				    }
				    return rc;
		        };
		    }
		    
		    lcms2.cmsCloseProfile(h);
		    fileOperation("namedcol.icc", remove);
		    
		    if (nc != null)
		    {
		    	lcms2.cmsFreeNamedColorList(nc);
		    }
		    return rc;
		}
	};
	
	// ----------------------------------------------------------------------------------------------------------
	
	// Formatters
	
	private static boolean FormatterFailed;
	
	private static void CheckSingleFormatter16(int Type, final String Text)
	{
	    short[] Values = new short[lcms2.cmsMAXCHANNELS];
	    VirtualPointer Buffer = new VirtualPointer(1024);
	    cmsFormatter f, b;
	    int i, j, nChannels, bytes;
	    _cmsTRANSFORM info;
	    
	    // Already failed?
	    if (FormatterFailed)
	    {
	    	return;
	    }
	    
	    info = new _cmsTRANSFORM();
	    info.OutputFormat = info.InputFormat = Type;
	    
	    // Go forth and back
	    f = lcms2_internal._cmsGetFormatter(Type, lcms2_plugin.cmsFormatterInput, 0);
	    b = lcms2_internal._cmsGetFormatter(Type, lcms2_plugin.cmsFormatterOutput, 0);
	    
	    if (!f.hasValue() || !b.hasValue())
	    {
	        Fail("no formatter for %s", new Object[]{Text});
	        FormatterFailed = true;
	        
	        // Useful for debug
	        f = lcms2_internal._cmsGetFormatter(Type, lcms2_plugin.cmsFormatterInput, 0);
	        b = lcms2_internal._cmsGetFormatter(Type, lcms2_plugin.cmsFormatterOutput, 0);
	        return;
	    }
	    
	    nChannels = lcms2.T_CHANNELS(Type);
	    bytes     = lcms2.T_BYTES(Type);
	    
	    for (j=0; j < 5; j++)
	    {
	        for (i=0; i < nChannels; i++)
	        { 
	            Values[i] = (short) (i+j);
	            // For 8-bit
	            if (bytes == 1)
	            {
	            	Values[i] <<= 8;
	            }
	        }
	        
	        b.get16().run(info, Values, Buffer, 1);
	        Arrays.zero(Values);
	        f.get16().run(info, Values, Buffer, 1);
	        
		    for (i=0; i < nChannels; i++)
		    {
		        if (bytes == 1)
		        {
		        	Values[i] >>>= 8;
		        }
		        
		        if ((Values[i] & 0xFFFF) != i+j)
		        {
		            Fail("%s failed", new Object[]{Text});
		            FormatterFailed = true;
		            
		            // Useful for debug
		            for (i=0; i < nChannels; i++)
		            {
		                Values[i] = (short) (i+j);
		                // For 8-bit
		                if (bytes == 1)
		                {
		                	Values[i] <<= 8;
		                }
		            }
		            
		            b.get16().run(info, Values, Buffer, 1);
		            f.get16().run(info, Values, Buffer, 1);
		            return;
		        }
		    }
	    }
	}
	
	// Check all formatters
	private static final TestFn CheckFormatters16 = new TestFn()
	{
		public int run()
		{
			FormatterFailed = false;
			
			CheckSingleFormatter16(lcms2.TYPE_GRAY_8, "TYPE_GRAY_8");
			CheckSingleFormatter16(lcms2.TYPE_GRAY_8_REV, "TYPE_GRAY_8_REV");
			CheckSingleFormatter16(lcms2.TYPE_GRAY_16, "TYPE_GRAY_16");
			CheckSingleFormatter16(lcms2.TYPE_GRAY_16_REV, "TYPE_GRAY_16_REV");
			CheckSingleFormatter16(lcms2.TYPE_GRAY_16_SE, "TYPE_GRAY_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_GRAYA_8, "TYPE_GRAYA_8");
			CheckSingleFormatter16(lcms2.TYPE_GRAYA_16, "TYPE_GRAYA_16");
			CheckSingleFormatter16(lcms2.TYPE_GRAYA_16_SE, "TYPE_GRAYA_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_GRAYA_8_PLANAR, "TYPE_GRAYA_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_GRAYA_16_PLANAR, "TYPE_GRAYA_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_RGB_8, "TYPE_RGB_8");
			CheckSingleFormatter16(lcms2.TYPE_RGB_8_PLANAR, "TYPE_RGB_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_BGR_8, "TYPE_BGR_8");
			CheckSingleFormatter16(lcms2.TYPE_BGR_8_PLANAR, "TYPE_BGR_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_RGB_16, "TYPE_RGB_16");
			CheckSingleFormatter16(lcms2.TYPE_RGB_16_PLANAR, "TYPE_RGB_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_RGB_16_SE, "TYPE_RGB_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_BGR_16, "TYPE_BGR_16");
			CheckSingleFormatter16(lcms2.TYPE_BGR_16_PLANAR, "TYPE_BGR_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_BGR_16_SE, "TYPE_BGR_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_RGBA_8, "TYPE_RGBA_8");
			CheckSingleFormatter16(lcms2.TYPE_RGBA_8_PLANAR, "TYPE_RGBA_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_RGBA_16, "TYPE_RGBA_16");
			CheckSingleFormatter16(lcms2.TYPE_RGBA_16_PLANAR, "TYPE_RGBA_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_RGBA_16_SE, "TYPE_RGBA_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_ARGB_8, "TYPE_ARGB_8");
			CheckSingleFormatter16(lcms2.TYPE_ARGB_16, "TYPE_ARGB_16");
			CheckSingleFormatter16(lcms2.TYPE_ABGR_8, "TYPE_ABGR_8");
			CheckSingleFormatter16(lcms2.TYPE_ABGR_16, "TYPE_ABGR_16");
			CheckSingleFormatter16(lcms2.TYPE_ABGR_16_PLANAR, "TYPE_ABGR_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_ABGR_16_SE, "TYPE_ABGR_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_BGRA_8, "TYPE_BGRA_8");
			CheckSingleFormatter16(lcms2.TYPE_BGRA_16, "TYPE_BGRA_16");
			CheckSingleFormatter16(lcms2.TYPE_BGRA_16_SE, "TYPE_BGRA_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMY_8, "TYPE_CMY_8");
			CheckSingleFormatter16(lcms2.TYPE_CMY_8_PLANAR, "TYPE_CMY_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_CMY_16, "TYPE_CMY_16");
			CheckSingleFormatter16(lcms2.TYPE_CMY_16_PLANAR, "TYPE_CMY_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_CMY_16_SE, "TYPE_CMY_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMYK_8, "TYPE_CMYK_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYKA_8, "TYPE_CMYKA_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK_8_REV, "TYPE_CMYK_8_REV");
			CheckSingleFormatter16(lcms2.TYPE_YUVK_8, "TYPE_YUVK_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK_8_PLANAR, "TYPE_CMYK_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_CMYK_16, "TYPE_CMYK_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK_16_REV, "TYPE_CMYK_16_REV");
			CheckSingleFormatter16(lcms2.TYPE_YUVK_16, "TYPE_YUVK_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK_16_PLANAR, "TYPE_CMYK_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_CMYK_16_SE, "TYPE_CMYK_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_KYMC_8, "TYPE_KYMC_8");
			CheckSingleFormatter16(lcms2.TYPE_KYMC_16, "TYPE_KYMC_16");
			CheckSingleFormatter16(lcms2.TYPE_KYMC_16_SE, "TYPE_KYMC_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_KCMY_8, "TYPE_KCMY_8");
			CheckSingleFormatter16(lcms2.TYPE_KCMY_8_REV, "TYPE_KCMY_8_REV");
			CheckSingleFormatter16(lcms2.TYPE_KCMY_16, "TYPE_KCMY_16");
			CheckSingleFormatter16(lcms2.TYPE_KCMY_16_REV, "TYPE_KCMY_16_REV");
			CheckSingleFormatter16(lcms2.TYPE_KCMY_16_SE, "TYPE_KCMY_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMYK5_8, "TYPE_CMYK5_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK5_16, "TYPE_CMYK5_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK5_16_SE, "TYPE_CMYK5_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_KYMC5_8, "TYPE_KYMC5_8");
			CheckSingleFormatter16(lcms2.TYPE_KYMC5_16, "TYPE_KYMC5_16");
			CheckSingleFormatter16(lcms2.TYPE_KYMC5_16_SE, "TYPE_KYMC5_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMYK6_8, "TYPE_CMYK6_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK6_8_PLANAR, "TYPE_CMYK6_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_CMYK6_16, "TYPE_CMYK6_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK6_16_PLANAR, "TYPE_CMYK6_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_CMYK6_16_SE, "TYPE_CMYK6_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMYK7_8, "TYPE_CMYK7_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK7_16, "TYPE_CMYK7_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK7_16_SE, "TYPE_CMYK7_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_KYMC7_8, "TYPE_KYMC7_8");
			CheckSingleFormatter16(lcms2.TYPE_KYMC7_16, "TYPE_KYMC7_16");
			CheckSingleFormatter16(lcms2.TYPE_KYMC7_16_SE, "TYPE_KYMC7_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMYK8_8, "TYPE_CMYK8_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK8_16, "TYPE_CMYK8_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK8_16_SE, "TYPE_CMYK8_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_KYMC8_8, "TYPE_KYMC8_8");
			CheckSingleFormatter16(lcms2.TYPE_KYMC8_16, "TYPE_KYMC8_16");
			CheckSingleFormatter16(lcms2.TYPE_KYMC8_16_SE, "TYPE_KYMC8_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMYK9_8, "TYPE_CMYK9_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK9_16, "TYPE_CMYK9_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK9_16_SE, "TYPE_CMYK9_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_KYMC9_8, "TYPE_KYMC9_8");
			CheckSingleFormatter16(lcms2.TYPE_KYMC9_16, "TYPE_KYMC9_16");
			CheckSingleFormatter16(lcms2.TYPE_KYMC9_16_SE, "TYPE_KYMC9_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMYK10_8, "TYPE_CMYK10_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK10_16, "TYPE_CMYK10_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK10_16_SE, "TYPE_CMYK10_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_KYMC10_8, "TYPE_KYMC10_8");
			CheckSingleFormatter16(lcms2.TYPE_KYMC10_16, "TYPE_KYMC10_16");
			CheckSingleFormatter16(lcms2.TYPE_KYMC10_16_SE, "TYPE_KYMC10_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMYK11_8, "TYPE_CMYK11_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK11_16, "TYPE_CMYK11_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK11_16_SE, "TYPE_CMYK11_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_KYMC11_8, "TYPE_KYMC11_8");
			CheckSingleFormatter16(lcms2.TYPE_KYMC11_16, "TYPE_KYMC11_16");
			CheckSingleFormatter16(lcms2.TYPE_KYMC11_16_SE, "TYPE_KYMC11_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_CMYK12_8, "TYPE_CMYK12_8");
			CheckSingleFormatter16(lcms2.TYPE_CMYK12_16, "TYPE_CMYK12_16");
			CheckSingleFormatter16(lcms2.TYPE_CMYK12_16_SE, "TYPE_CMYK12_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_KYMC12_8, "TYPE_KYMC12_8");
			CheckSingleFormatter16(lcms2.TYPE_KYMC12_16, "TYPE_KYMC12_16");
			CheckSingleFormatter16(lcms2.TYPE_KYMC12_16_SE, "TYPE_KYMC12_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_XYZ_16, "TYPE_XYZ_16");
			CheckSingleFormatter16(lcms2.TYPE_Lab_8, "TYPE_Lab_8");
			CheckSingleFormatter16(lcms2.TYPE_ALab_8, "TYPE_ALab_8");
			CheckSingleFormatter16(lcms2.TYPE_Lab_16, "TYPE_Lab_16");
			CheckSingleFormatter16(lcms2.TYPE_Yxy_16, "TYPE_Yxy_16");
			CheckSingleFormatter16(lcms2.TYPE_YCbCr_8, "TYPE_YCbCr_8");
			CheckSingleFormatter16(lcms2.TYPE_YCbCr_8_PLANAR, "TYPE_YCbCr_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_YCbCr_16, "TYPE_YCbCr_16");
			CheckSingleFormatter16(lcms2.TYPE_YCbCr_16_PLANAR, "TYPE_YCbCr_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_YCbCr_16_SE, "TYPE_YCbCr_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_YUV_8, "TYPE_YUV_8");
			CheckSingleFormatter16(lcms2.TYPE_YUV_8_PLANAR, "TYPE_YUV_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_YUV_16, "TYPE_YUV_16");
			CheckSingleFormatter16(lcms2.TYPE_YUV_16_PLANAR, "TYPE_YUV_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_YUV_16_SE, "TYPE_YUV_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_HLS_8, "TYPE_HLS_8");
			CheckSingleFormatter16(lcms2.TYPE_HLS_8_PLANAR, "TYPE_HLS_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_HLS_16, "TYPE_HLS_16");
			CheckSingleFormatter16(lcms2.TYPE_HLS_16_PLANAR, "TYPE_HLS_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_HLS_16_SE, "TYPE_HLS_16_SE");
			CheckSingleFormatter16(lcms2.TYPE_HSV_8, "TYPE_HSV_8");
			CheckSingleFormatter16(lcms2.TYPE_HSV_8_PLANAR, "TYPE_HSV_8_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_HSV_16, "TYPE_HSV_16");
			CheckSingleFormatter16(lcms2.TYPE_HSV_16_PLANAR, "TYPE_HSV_16_PLANAR");
			CheckSingleFormatter16(lcms2.TYPE_HSV_16_SE, "TYPE_HSV_16_SE");
			
			CheckSingleFormatter16(lcms2.TYPE_XYZ_FLT, "TYPE_XYZ_FLT");
			CheckSingleFormatter16(lcms2.TYPE_Lab_FLT, "TYPE_Lab_FLT");
			CheckSingleFormatter16(lcms2.TYPE_GRAY_FLT, "TYPE_GRAY_FLT");
			CheckSingleFormatter16(lcms2.TYPE_RGB_FLT, "TYPE_RGB_FLT");
			CheckSingleFormatter16(lcms2.TYPE_CMYK_FLT, "TYPE_CMYK_FLT");
			
			CheckSingleFormatter16(lcms2.TYPE_XYZ_DBL, "TYPE_XYZ_DBL");
			CheckSingleFormatter16(lcms2.TYPE_Lab_DBL, "TYPE_Lab_DBL");
			CheckSingleFormatter16(lcms2.TYPE_GRAY_DBL, "TYPE_GRAY_DBL");
			CheckSingleFormatter16(lcms2.TYPE_RGB_DBL, "TYPE_RGB_DBL");
			CheckSingleFormatter16(lcms2.TYPE_CMYK_DBL, "TYPE_CMYK_DBL");
			
			CheckSingleFormatter16(lcms2.TYPE_LabV2_8, "TYPE_LabV2_8");
			CheckSingleFormatter16(lcms2.TYPE_ALabV2_8, "TYPE_ALabV2_8");
			CheckSingleFormatter16(lcms2.TYPE_LabV2_16, "TYPE_LabV2_16");
			
			return !FormatterFailed ? 1 : 0;
		}
	};
	
	private static void CheckSingleFormatterFloat(int Type, final String Text)
	{
	    float[] Values = new float[lcms2.cmsMAXCHANNELS];
	    VirtualPointer Buffer = new VirtualPointer(1024);
	    cmsFormatter f, b;
	    int i, j, nChannels;
	    _cmsTRANSFORM info;
	    
	    // Already failed?
	    if (FormatterFailed)
	    {
	    	return;
	    }
	    
	    info = new _cmsTRANSFORM();
	    info.OutputFormat = info.InputFormat = Type;
	    
	    // Go forth and back
	    f = lcms2_internal._cmsGetFormatter(Type, lcms2_plugin.cmsFormatterInput, lcms2_plugin.CMS_PACK_FLAGS_FLOAT);
	    b = lcms2_internal._cmsGetFormatter(Type, lcms2_plugin.cmsFormatterOutput, lcms2_plugin.CMS_PACK_FLAGS_FLOAT);
	    
	    if (!f.hasValue() || !b.hasValue())
	    {
	        Fail("no formatter for %s", new Object[]{Text});
	        FormatterFailed = true;
	        
	        // Useful for debug
	        f = lcms2_internal._cmsGetFormatter(Type, lcms2_plugin.cmsFormatterInput, lcms2_plugin.CMS_PACK_FLAGS_FLOAT);
	        b = lcms2_internal._cmsGetFormatter(Type, lcms2_plugin.cmsFormatterOutput, lcms2_plugin.CMS_PACK_FLAGS_FLOAT);
	        return;
	    }
	    
	    nChannels = lcms2.T_CHANNELS(Type);
	    
	    for (j=0; j < 5; j++)
	    {
	        for (i=0; i < nChannels; i++)
	        { 
	            Values[i] = (float) (i+j);
	        }
	        
	        b.getFloat().run(info, Values, Buffer, 1);
	        for(i = 0; i < nChannels; i++)
	        {
	        	Values[i] = 0;
	        }
	        f.getFloat().run(info, Values, Buffer, 1);
	        
	        for (i=0; i < nChannels; i++)
	        {
	            double delta = Math.abs(Values[i] - ( i+j));
	            
	            if (delta > 0.000000001)
	            {
	                Fail("%s failed", new Object[]{Text});        
	                FormatterFailed = true;
	                
	                // Useful for debug
	                for (i=0; i < nChannels; i++)
	                { 
	                    Values[i] = (float) (i+j);
	                }
	                
	                b.getFloat().run(info, Values, Buffer, 1);
	                f.getFloat().run(info, Values, Buffer, 1);
	                return;
	            }
	        }
	    }
	}
	
	private static final TestFn CheckFormattersFloat = new TestFn()
	{
		public int run()
		{
			FormatterFailed = false;
			
			CheckSingleFormatterFloat(lcms2.TYPE_XYZ_FLT, "TYPE_XYZ_FLT");
			CheckSingleFormatterFloat(lcms2.TYPE_Lab_FLT, "TYPE_Lab_FLT");
			CheckSingleFormatterFloat(lcms2.TYPE_GRAY_FLT, "TYPE_GRAY_FLT");
			CheckSingleFormatterFloat(lcms2.TYPE_RGB_FLT, "TYPE_RGB_FLT");
			CheckSingleFormatterFloat(lcms2.TYPE_CMYK_FLT, "TYPE_CMYK_FLT");
			
			CheckSingleFormatterFloat(lcms2.TYPE_XYZ_DBL, "TYPE_XYZ_DBL");
			CheckSingleFormatterFloat(lcms2.TYPE_Lab_DBL, "TYPE_Lab_DBL");
			CheckSingleFormatterFloat(lcms2.TYPE_GRAY_DBL, "TYPE_GRAY_DBL");
			CheckSingleFormatterFloat(lcms2.TYPE_RGB_DBL, "TYPE_RGB_DBL");
			CheckSingleFormatterFloat(lcms2.TYPE_CMYK_DBL, "TYPE_CMYK_DBL");
			
			return !FormatterFailed ? 1 : 0;
		}
	};
	
	// Write tag testbed ----------------------------------------------------------------------------------------
	
	private static int CheckXYZ(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsCIEXYZ XYZ, Pt;
	    
	    switch (Pass)
	    {
	        case 1:
	        	XYZ = new cmsCIEXYZ();
	            XYZ.X = 1.0; XYZ.Y = 1.1; XYZ.Z = 1.2;
	            return lcms2.cmsWriteTag(hProfile, tag, XYZ) ? 1 : 0;
	            
	        case 2:
	            Pt = (cmsCIEXYZ)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            return (IsGoodFixed15_16("X", 1.0, Pt.X) &&
	                   IsGoodFixed15_16("Y", 1.1, Pt.Y) &&
	                   IsGoodFixed15_16("Z", 1.2, Pt.Z)) ? 1 : 0;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckGamma(int Pass, cmsHPROFILE hProfile, int tag)
	{
		cmsToneCurve g, Pt;
	    int rc;
	    
	    switch (Pass)
	    {
	        case 1:
	        	g = lcms2.cmsBuildGamma(DbgThread(), 1.0);
	            rc = lcms2.cmsWriteTag(hProfile, tag, g) ? 1 : 0;
	            lcms2.cmsFreeToneCurve(g);
	            return rc;
	            
	        case 2:
	        	Pt = (cmsToneCurve)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            return lcms2.cmsIsToneCurveLinear(Pt) ? 1 : 0;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckText(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsMLU m, Pt;
	    int rc;
	    StringBuffer Buffer = new StringBuffer(256);
	    
	    switch (Pass)
	    {
	        case 1:
	            m = lcms2.cmsMLUalloc(DbgThread(), 0);
	            lcms2.cmsMLUsetASCII(m, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, "Test test");
	            rc = lcms2.cmsWriteTag(hProfile, tag, m) ? 1 : 0;
	            lcms2.cmsMLUfree(m);
	            return rc;
	            
	        case 2:
	            Pt = (cmsMLU)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            lcms2.cmsMLUgetASCII(Pt, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, Buffer, 256);
	            return strcmp(Buffer, "Test test") == 0 ? 1 : 0;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckData(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsICCData Pt;
	    cmsICCData d = new cmsICCData();
	    d.len = 1;
	    d.flag = 0;
	    d.data.getProcessor().write((byte)'?');
	    int rc;
	    
	    switch (Pass)
	    {
	        case 1:
	            rc = lcms2.cmsWriteTag(hProfile, tag, d) ? 1 : 0;
	            return rc;
	            
	        case 2:
	            Pt = (cmsICCData)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            return ((Pt.data.getProcessor().readInt8() == (byte)'?') && (Pt.flag == 0) && (Pt.len == 1)) ? 1 : 0;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckSignature(int Pass, cmsHPROFILE hProfile, int tag)
	{
		Integer Pt, Holder;
	    
	    switch (Pass)
	    {
	        case 1:
	            Holder = new Integer(lcms2.cmsSigPerceptualReferenceMediumGamut);
	            return lcms2.cmsWriteTag(hProfile, tag, Holder) ? 1 : 0;
	    
	        case 2:
	            Pt = (Integer)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            return Pt.intValue() == lcms2.cmsSigPerceptualReferenceMediumGamut ? 1 : 0;

	        default:
	            return 0;
	    }
	}
	
	private static int CheckDateTime(int Pass, cmsHPROFILE hProfile, int tag)
	{
		Calendar Pt, Holder;

	    switch (Pass)
	    {
	        case 1:
	        	Holder = Calendar.getInstance();
	        	Holder.set(Calendar.HOUR_OF_DAY, 1);
	        	Holder.set(Calendar.MINUTE, 2);
	        	Holder.set(Calendar.SECOND, 3);
	        	Holder.set(Calendar.DAY_OF_MONTH, 4);
	        	Holder.set(Calendar.MONTH, 5);
	        	Holder.set(Calendar.YEAR, 2009);
	            return lcms2.cmsWriteTag(hProfile, tag, Holder) ? 1 : 0;
	            
	        case 2:
	            Pt = (Calendar)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            return (Pt.get(Calendar.HOUR_OF_DAY) == 1 && 
	            		Pt.get(Calendar.MINUTE) == 2 && 
	            		Pt.get(Calendar.SECOND) == 3 &&
	            		Pt.get(Calendar.DAY_OF_MONTH) == 4 &&
	            		Pt.get(Calendar.MONTH) == 5 &&
	            		Pt.get(Calendar.YEAR) == 2009) ? 1 : 0;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckNamedColor(int Pass, cmsHPROFILE hProfile, int tag, int max_check, boolean colorant_check)
	{
	    cmsNAMEDCOLORLIST nc;
	    int i, j, rc;
	    StringBuffer Name = new StringBuffer(255);
	    short[] PCS = new short[3];
	    short[] Colorant = new short[lcms2.cmsMAXCHANNELS];
	    StringBuffer CheckName = new StringBuffer(255);
	    short[] CheckPCS = new short[3];
	    short[] CheckColorant = new short[lcms2.cmsMAXCHANNELS];
	    
	    switch (Pass)
	    {
		    case 1:
		        nc = lcms2.cmsAllocNamedColorList(DbgThread(), 0, 4, "prefix", "suffix");
		        if (nc == null)
		        {
		        	return 0;
		        }
		        
		        for (i=0; i < max_check; i++)
		        {
		            PCS[0] = PCS[1] = PCS[2] = (short) i;
		            Colorant[0] = Colorant[1] = Colorant[2] = Colorant[3] = (short) (max_check - i);
		            
		            Utility.sprintf(Name, "#%d", new Object[]{new Integer(i)});
		            if (!lcms2.cmsAppendNamedColor(nc, Name.toString(), PCS, Colorant))
		            {
		            	Fail("Couldn't append named color", null);
		            	return 0;
		            }
		        }
		        
		        rc = lcms2.cmsWriteTag(hProfile, tag, nc) ? 1 : 0;
		        lcms2.cmsFreeNamedColorList(nc);
		        return rc;
		        
		    case 2:
	
		        nc = (cmsNAMEDCOLORLIST)lcms2.cmsReadTag(hProfile, tag);
		        if (nc == null)
		        {
		        	return 0;
		        }
		        
		        for (i=0; i < max_check; i++)
		        {
		            CheckPCS[0] = CheckPCS[1] = CheckPCS[2] = (short) i;
		            CheckColorant[0] = CheckColorant[1] = CheckColorant[2] = CheckColorant[3] = (short) (max_check - i);
		            
		            Utility.sprintf(CheckName, "#%d", new Object[]{new Integer(i)});
		            if (!lcms2.cmsNamedColorInfo(nc, i, Name, null, null, PCS, Colorant))
		            {
		            	Fail("Invalid string", null);
		            	return 0;
		            }
		            
		            for (j=0; j < 3; j++)
		            {
		                if (CheckPCS[j] != PCS[j])
		                {
		                	Fail("Invalid PCS", null);
		                	return 0;
		                }
		            }
		            
		            // This is only used on named color list
		            if (colorant_check)
		            {
		            	for (j=0; j < 4; j++)
		            	{
		            		if (CheckColorant[j] != Colorant[j])
		            		{
		            			Fail("Invalid Colorant", null);
		            			return 0;
		            		}
		            	}
		            }
		            
		            if (strcmp(Name, CheckName) != 0)
		            {
		            	Fail("Invalid Name", null);
		            	return 0;
		            };
		        }
		        return 1;
		        
		    default: return 0;
	    }
	}
	
	private static int CheckLUT(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsPipeline Lut, Pt;
	    int rc;
	    
	    switch (Pass)
	    {
	        case 1:
	            Lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
	            if (Lut == null)
	            {
	            	return 0;
	            }
	            
	            // Create an identity LUT
	            lcms2.cmsPipelineInsertStage(Lut, lcms2.cmsAT_BEGIN, lcms2_internal._cmsStageAllocIdentityCurves(DbgThread(), 3));
	            lcms2.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocIdentityCLut(DbgThread(), 3));
	            lcms2.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocIdentityCurves(DbgThread(), 3));
	            
	            rc =  lcms2.cmsWriteTag(hProfile, tag, Lut) ? 1 : 0;
	            lcms2.cmsPipelineFree(Lut);
	            return rc;
	            
	        case 2:
	            Pt = (cmsPipeline)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            
	            // Transform values, check for identity
	            return Check16LUT(Pt);
	            
	        default:
	            return 0;
	    }
	}
	
	static int CheckCHAD(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    double[] Pt;
	    double[] CHAD = { 0, .1, .2, .3, .4, .5, .6, .7, .8 };
	    int i;
	    
	    switch (Pass)
	    {
	        case 1:         
	            return lcms2.cmsWriteTag(hProfile, tag, CHAD) ? 1 : 0;
	            
	        case 2:
	            Pt = (double[])lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            
	            for (i=0; i < 9; i++)
	            {
	                if (!IsGoodFixed15_16("CHAD", Pt[i], CHAD[i]))
	                {
	                	return 0;
	                }
	            }
	            
	            return 1;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckChromaticity(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsCIExyYTRIPLE Pt, c = new cmsCIExyYTRIPLE(new double[]{0, .1, 1 , .3, .4, 1 , .6, .7, 1});
	    
	    switch (Pass)
	    {
	        case 1:         
	            return lcms2.cmsWriteTag(hProfile, tag, c) ? 1 : 0;
	            
	        case 2:
	            Pt = (cmsCIExyYTRIPLE)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            
	            if (!IsGoodFixed15_16("xyY", Pt.Red.x, c.Red.x))
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("xyY", Pt.Red.y, c.Red.y))
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("xyY", Pt.Green.x, c.Green.x))
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("xyY", Pt.Green.y, c.Green.y))
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("xyY", Pt.Blue.x, c.Blue.x))
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("xyY", Pt.Blue.y, c.Blue.y))
	            {
	            	return 0;
	            }
	            return 1;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckColorantOrder(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    byte[] Pt, c = new byte[lcms2.cmsMAXCHANNELS];
	    int i;
	    
	    switch (Pass)
	    {
	        case 1:         
	            for (i=0; i < lcms2.cmsMAXCHANNELS; i++)
	            {
	            	c[i] = (byte)(lcms2.cmsMAXCHANNELS - i - 1);
	            }
	            return lcms2.cmsWriteTag(hProfile, tag, c) ? 1 : 0;
	            
	        case 2:
	            Pt = (byte[])lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            
	            for (i=0; i < lcms2.cmsMAXCHANNELS; i++)
	            {
	                if (Pt[i] != (lcms2.cmsMAXCHANNELS - i - 1 ))
	                {
	                	return 0;
	                }
	            }
	            return 1;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckMeasurement(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsICCMeasurementConditions Pt, m;
	    
	    switch (Pass)
	    {
	        case 1:
	        	m = new cmsICCMeasurementConditions();
	            m.Backing.X = 0.1;
	            m.Backing.Y = 0.2;
	            m.Backing.Z = 0.3;
	            m.Flare = 1.0;
	            m.Geometry = 1;
	            m.IlluminantType = lcms2.cmsILLUMINANT_TYPE_D50;
	            m.Observer = 1;
	            return lcms2.cmsWriteTag(hProfile, tag, m) ? 1 : 0;
	            
	        case 2:
	            Pt = (cmsICCMeasurementConditions)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            
	            if (!IsGoodFixed15_16("Backing", Pt.Backing.X, 0.1))
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("Backing", Pt.Backing.Y, 0.2))
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("Backing", Pt.Backing.Z, 0.3))
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("Flare",   Pt.Flare, 1.0))
	            {
	            	return 0;
	            }
	            
	            if (Pt.Geometry != 1)
	            {
	            	return 0;
	            }
	            if (Pt.IlluminantType != lcms2.cmsILLUMINANT_TYPE_D50)
	            {
	            	return 0;
	            }
	            if (Pt.Observer != 1)
	            {
	            	return 0;
	            }
	            return 1;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckUcrBg(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsUcrBg Pt, m;
	    int rc;
	    StringBuffer Buffer = new StringBuffer(256);
	    
	    switch (Pass)
	    {
	        case 1:
	        	m = new cmsUcrBg();
	            m.Ucr = lcms2.cmsBuildGamma(DbgThread(), 2.4);
	            m.Bg  = lcms2.cmsBuildGamma(DbgThread(), -2.2);
	            m.Desc = lcms2.cmsMLUalloc(DbgThread(), 1);
	            lcms2.cmsMLUsetASCII(m.Desc, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, "test UCR/BG");         
	            rc = lcms2.cmsWriteTag(hProfile, tag, m) ? 1 : 0;
	            lcms2.cmsMLUfree(m.Desc);
	            lcms2.cmsFreeToneCurve(m.Bg);
	            lcms2.cmsFreeToneCurve(m.Ucr);
	            return rc;
	            
	        case 2:
	            Pt = (cmsUcrBg)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            
	            lcms2.cmsMLUgetASCII(Pt.Desc, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, Buffer, 256);
	            if (strcmp(Buffer, "test UCR/BG") != 0)
	            {
	            	return 0;       
	            }
	            return 1;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckCRDinfo(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsMLU mlu;
	    StringBuffer Buffer = new StringBuffer(256);
	    int rc;
	    
	    switch (Pass)
	    {
	        case 1:
	            mlu = lcms2.cmsMLUalloc(DbgThread(), 5);
	            
	            lcms2.cmsMLUsetWide(mlu,  "PS", "nm", "test postscript");
	            lcms2.cmsMLUsetWide(mlu,  "PS", "#0", "perceptual");
	            lcms2.cmsMLUsetWide(mlu,  "PS", "#1", "relative_colorimetric");
	            lcms2.cmsMLUsetWide(mlu,  "PS", "#2", "saturation");
	            lcms2.cmsMLUsetWide(mlu,  "PS", "#3", "absolute_colorimetric");  
	            rc = lcms2.cmsWriteTag(hProfile, tag, mlu) ? 1 : 0;
	            lcms2.cmsMLUfree(mlu);
	            return rc;
	            
	        case 2:
	            mlu = (cmsMLU)lcms2.cmsReadTag(hProfile, tag);
	            if (mlu == null)
	            {
	            	return 0;
	            }
	            
	            lcms2.cmsMLUgetASCII(mlu, "PS", "nm", Buffer, 256);
	            if (strcmp(Buffer, "test postscript") != 0)
	            {
	            	return 0;
	            }
	            
	            lcms2.cmsMLUgetASCII(mlu, "PS", "#0", Buffer, 256);
	            if (strcmp(Buffer, "perceptual") != 0)
	            {
	            	return 0;
	            }
	            
	            lcms2.cmsMLUgetASCII(mlu, "PS", "#1", Buffer, 256);
	            if (strcmp(Buffer, "relative_colorimetric") != 0)
	            {
	            	return 0;
	            }
	            
	            lcms2.cmsMLUgetASCII(mlu, "PS", "#2", Buffer, 256);
	            if (strcmp(Buffer, "saturation") != 0)
	            {
	            	return 0;
	            }
	            
	            lcms2.cmsMLUgetASCII(mlu, "PS", "#3", Buffer, 256);
	            if (strcmp(Buffer, "absolute_colorimetric") != 0)
	            {
	            	return 0;
	            }
	            return 1;
	             
	        default:
	            return 0;
	    }
	}
	
	private static cmsToneCurve CreateSegmentedCurve()
	{
	    lcms2.cmsCurveSegment[] Seg = new lcms2.cmsCurveSegment[3];
	    float[] Sampled = { 0, 1};
	    
	    Seg[0] = new lcms2.cmsCurveSegment();
	    Seg[0].Type = 6;
	    Seg[0].Params[0] = 1;
	    Seg[0].Params[1] = 0;
	    Seg[0].Params[2] = 0;
	    Seg[0].Params[3] = 0;
	    Seg[0].x0 = -1E22F;
	    Seg[0].x1 = 0;
	    
	    Seg[1] = new lcms2.cmsCurveSegment();
	    Seg[1].Type = 0;
	    Seg[1].nGridPoints = 2;
	    Seg[1].SampledPoints = Sampled;
	    Seg[1].x0 = 0;
	    Seg[1].x1 = 1;
	    
	    Seg[2] = new lcms2.cmsCurveSegment();
	    Seg[2].Type = 6;
	    Seg[2].Params[0] = 1;
	    Seg[2].Params[1] = 0;
	    Seg[2].Params[2] = 0;
	    Seg[2].Params[3] = 0;               
	    Seg[2].x0 = 1;
	    Seg[2].x1 = 1E22F;
	    
	    return lcms2.cmsBuildSegmentedToneCurve(DbgThread(), 3, Seg);
	}
	
	private static int CheckMPE(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsPipeline Lut, Pt;
	    cmsToneCurve[] G = new cmsToneCurve[3];
	    int rc;
	    
	    switch (Pass)
	    {
	        case 1: 
	            Lut = lcms2.cmsPipelineAlloc(DbgThread(), 3, 3);
	            
	            lcms2.cmsPipelineInsertStage(Lut, lcms2.cmsAT_BEGIN, lcms2_internal._cmsStageAllocLabV2ToV4(DbgThread()));
	            lcms2.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, lcms2_internal._cmsStageAllocLabV4ToV2(DbgThread()));
	            AddIdentityCLUTfloat(Lut);
	            
	            G[0] = G[1] = G[2] = CreateSegmentedCurve();
	            lcms2.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, lcms2.cmsStageAllocToneCurves(DbgThread(), 3, G));
	            lcms2.cmsFreeToneCurve(G[0]);
	            
	            rc = lcms2.cmsWriteTag(hProfile, tag, Lut) ? 1 : 0;
	            lcms2.cmsPipelineFree(Lut);
	            return rc;
	            
	        case 2:
	            Pt = (cmsPipeline)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;           
	            }
	            return CheckFloatLUT(Pt);
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckScreening(int Pass, cmsHPROFILE hProfile, int tag)
	{
	    cmsScreening Pt, sc;
	    int rc;
	    
	    switch (Pass)
	    {
	        case 1:
	        	sc = new cmsScreening();
	            sc.Flag = 0;
	            sc.nChannels = 1;
	            sc.Channels[0] = new lcms2.cmsScreeningChannel();
	            sc.Channels[0].Frequency = 2.0;
	            sc.Channels[0].ScreenAngle = 3.0;
	            sc.Channels[0].SpotShape = lcms2.cmsSPOT_ELLIPSE;
	            
	            rc = lcms2.cmsWriteTag(hProfile, tag, sc) ? 1 : 0;
	            return rc;
	            
	        case 2:
	            Pt = (cmsScreening)lcms2.cmsReadTag(hProfile, tag);
	            if (Pt == null)
	            {
	            	return 0;
	            }
	            
	            if (Pt.nChannels != 1)
	            {
	            	return 0;
	            }
	            if (Pt.Flag      != 0)
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("Freq", Pt.Channels[0].Frequency, 2.0))
	            {
	            	return 0;
	            }
	            if (!IsGoodFixed15_16("Angle", Pt.Channels[0].ScreenAngle, 3.0))
	            {
	            	return 0;
	            }
	            if (Pt.Channels[0].SpotShape != lcms2.cmsSPOT_ELLIPSE)
	            {
	            	return 0;
	            }
	            return 1;
	            
	        default:
	            return 0;
	    }
	}
	
	private static boolean CheckOneStr(cmsMLU mlu, int n)
	{
		StringBuffer Buffer = new StringBuffer(256), Buffer2 = new StringBuffer(256);
		
	    lcms2.cmsMLUgetASCII(mlu, "en", "US", Buffer, 255);
	    Utility.sprintf(Buffer2, "Hello, world %d", new Object[]{new Integer(n)});
	    if (strcmp(Buffer, Buffer2) != 0)
	    {
	    	return false;
	    }
	    
	    lcms2.cmsMLUgetASCII(mlu, "es", "ES", Buffer, 255);
	    Utility.sprintf(Buffer2, "Hola, mundo %d", new Object[]{new Integer(n)});
	    if (strcmp(Buffer, Buffer2) != 0)
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static cmsMLU SetOneStr(cmsMLU mlu, String s1, String s2)
	{
	    mlu = lcms2.cmsMLUalloc(DbgThread(), 0);
	    lcms2.cmsMLUsetWide(mlu, "en", "US", s1);
	    lcms2.cmsMLUsetWide(mlu, "es", "ES", s2);
	    return mlu;
	}
	
	private static int CheckProfileSequenceTag(int Pass, cmsHPROFILE hProfile)
	{
	    cmsSEQ s;
	    int i;
	    
	    switch (Pass)
	    {
		    case 1:
		        s = lcms2.cmsAllocProfileSequenceDescription(DbgThread(), 3);
		        if (s == null)
	            {
	            	return 0;
	            }
		        
		        s.seq[0].Manufacturer = SetOneStr(s.seq[0].Manufacturer, "Hello, world 0", "Hola, mundo 0");
		        s.seq[0].Model = SetOneStr(s.seq[0].Model, "Hello, world 0", "Hola, mundo 0");
		        s.seq[1].Manufacturer = SetOneStr(s.seq[1].Manufacturer, "Hello, world 1", "Hola, mundo 1");
		        s.seq[1].Model = SetOneStr(s.seq[1].Model, "Hello, world 1", "Hola, mundo 1");
		        s.seq[2].Manufacturer = SetOneStr(s.seq[2].Manufacturer, "Hello, world 2", "Hola, mundo 2");
		        s.seq[2].Model = SetOneStr(s.seq[2].Model, "Hello, world 2", "Hola, mundo 2");
		        
		        s.seq[0].attributes = lcms2.cmsTransparency|lcms2.cmsMatte;
		        
		        s.seq[1].attributes = lcms2.cmsReflective|lcms2.cmsMatte;
		        
		        s.seq[2].attributes = lcms2.cmsTransparency|lcms2.cmsGlossy;
		        
		        if (!lcms2.cmsWriteTag(hProfile, lcms2.cmsSigProfileSequenceDescTag, s))
	            {
	            	return 0;
	            }  
		        lcms2.cmsFreeProfileSequenceDescription(s);
		        return 1;
		        
		    case 2:
		        s = (cmsSEQ)lcms2.cmsReadTag(hProfile, lcms2.cmsSigProfileSequenceDescTag);
		        if (s == null)
	            {
	            	return 0;
	            }
		        
		        if (s.n != 3)
	            {
	            	return 0;
	            }
		        
		        if (s.seq[0].attributes != (lcms2.cmsTransparency|lcms2.cmsMatte))
	            {
	            	return 0;
	            }
		        
		        if (s.seq[1].attributes != (lcms2.cmsReflective|lcms2.cmsMatte))
	            {
	            	return 0;
	            }
		        
		        if (s.seq[2].attributes != (lcms2.cmsTransparency|lcms2.cmsGlossy))
	            {
	            	return 0;
	            }
		        
		        // Check MLU
		        for (i=0; i < 3; i++)
		        {
		            if (!CheckOneStr(s.seq[i].Manufacturer, i))
		            {
		            	return 0;
		            }
		            if (!CheckOneStr(s.seq[i].Model, i))
		            {
		            	return 0;
		            }
		        }
		        return 1;
		        
		    default:
		        return 0;
	    }
	}
	
	private static int CheckProfileSequenceIDTag(int Pass, cmsHPROFILE hProfile)
	{
	    cmsSEQ s;
	    int i;
	    
	    switch (Pass)
	    {
		    case 1:
		        s = lcms2.cmsAllocProfileSequenceDescription(DbgThread(), 3);
		        if (s == null)
	            {
	            	return 0;
	            }
		        
		        s.seq[0].ProfileID.setID8("0123456789ABCDEF".getBytes());
		        s.seq[1].ProfileID.setID8("1111111111111111".getBytes());
		        s.seq[2].ProfileID.setID8("2222222222222222".getBytes());
		        
		        s.seq[0].Description = SetOneStr(s.seq[0].Description, "Hello, world 0", "Hola, mundo 0");
		        s.seq[1].Description = SetOneStr(s.seq[1].Description, "Hello, world 1", "Hola, mundo 1");
		        s.seq[2].Description = SetOneStr(s.seq[2].Description, "Hello, world 2", "Hola, mundo 2");
		        
		        if (!lcms2.cmsWriteTag(hProfile, lcms2.cmsSigProfileSequenceIdTag, s))
	            {
	            	return 0;
	            }
		        lcms2.cmsFreeProfileSequenceDescription(s);
		        return 1;
		        
		    case 2:
		        s = (cmsSEQ)lcms2.cmsReadTag(hProfile, lcms2.cmsSigProfileSequenceIdTag);
		        if (s == null)
	            {
	            	return 0;
	            }
		        
		        if (s.n != 3)
	            {
	            	return 0;
	            }
		        
		        if (new String(s.seq[0].ProfileID.getID8()).compareTo("0123456789ABCDEF") != 0)
	            {
	            	return 0;
	            }
		        if (new String(s.seq[1].ProfileID.getID8()).compareTo("1111111111111111") != 0)
	            {
	            	return 0;
	            }
		        if (new String(s.seq[2].ProfileID.getID8()).compareTo("2222222222222222") != 0)
	            {
	            	return 0;
	            }
		        
		        for (i=0; i < 3; i++)
		        {
		            if (!CheckOneStr(s.seq[i].Description, i))
		            {
		            	return 0;
		            }
		        }
		        
		        return 1;
		        
		    default:
		        return 0;
	    }
	}
	
	private static int CheckICCViewingConditions(int Pass, cmsHPROFILE hProfile)
	{
	    cmsICCViewingConditions v;
	    cmsICCViewingConditions s;
	    
	    switch (Pass)
	    {
	        case 1:
	        	s = new cmsICCViewingConditions();
	            s.IlluminantType = 1;
	            s.IlluminantXYZ.X = 0.1;
	            s.IlluminantXYZ.Y = 0.2;
	            s.IlluminantXYZ.Z = 0.3;
	            s.SurroundXYZ.X = 0.4;
	            s.SurroundXYZ.Y = 0.5;
	            s.SurroundXYZ.Z = 0.6;
	            
	            if (!lcms2.cmsWriteTag(hProfile, lcms2.cmsSigViewingConditionsTag, s))
	            {
	            	return 0;
	            }
	            return 1;
	            
	        case 2:
	            v = (cmsICCViewingConditions)lcms2.cmsReadTag(hProfile, lcms2.cmsSigViewingConditionsTag);
	            if (v == null)
	            {
	            	return 0;
	            }
	            
	            if (v.IlluminantType != 1) return 0;
	            if (!IsGoodVal("IlluminantXYZ.X", v.IlluminantXYZ.X, 0.1, 0.001))
	            {
	            	return 0;
	            }
	            if (!IsGoodVal("IlluminantXYZ.Y", v.IlluminantXYZ.Y, 0.2, 0.001))
	            {
	            	return 0;
	            }
	            if (!IsGoodVal("IlluminantXYZ.Z", v.IlluminantXYZ.Z, 0.3, 0.001))
	            {
	            	return 0;
	            }
	            
	            if (!IsGoodVal("SurroundXYZ.X", v.SurroundXYZ.X, 0.4, 0.001))
	            {
	            	return 0;
	            }
	            if (!IsGoodVal("SurroundXYZ.Y", v.SurroundXYZ.Y, 0.5, 0.001))
	            {
	            	return 0;
	            }
	            if (!IsGoodVal("SurroundXYZ.Z", v.SurroundXYZ.Z, 0.6, 0.001))
	            {
	            	return 0;
	            }
	            
	            return 1;
	            
	        default:
	            return 0;
	    }
	}
	
	private static int CheckVCGT(int Pass, cmsHPROFILE hProfile)
	{
	    cmsToneCurve[] Curves = new cmsToneCurve[3];
	    cmsToneCurve[] PtrCurve;
	    
	    switch (Pass)
	    {
	        case 1:
	            Curves[0] = lcms2.cmsBuildGamma(DbgThread(), 1.1);
	            Curves[1] = lcms2.cmsBuildGamma(DbgThread(), 2.2);
	            Curves[2] = lcms2.cmsBuildGamma(DbgThread(), 3.4);
	            
	            if (!lcms2.cmsWriteTag(hProfile, lcms2.cmsSigVcgtTag, Curves))
	            {
	            	return 0;
	            }
	            
	            lcms2.cmsFreeToneCurveTriple(Curves);
	            return 1;
	            
	        case 2:
	        	PtrCurve = (cmsToneCurve[])lcms2.cmsReadTag(hProfile, lcms2.cmsSigVcgtTag);
	        	if (PtrCurve == null)
	            {
	            	return 0;
	            }
	        	if (!IsGoodVal("VCGT R", lcms2.cmsEstimateGamma(PtrCurve[0], 0.01), 1.1, 0.001))
	            {
	            	return 0;
	            }
	        	if (!IsGoodVal("VCGT G", lcms2.cmsEstimateGamma(PtrCurve[1], 0.01), 2.2, 0.001))
	            {
	            	return 0;
	            }
	        	if (!IsGoodVal("VCGT B", lcms2.cmsEstimateGamma(PtrCurve[2], 0.01), 3.4, 0.001))
	            {
	            	return 0;
	            }
	        	return 1;
	             
	        default:;
	    }
	    
	    return 0;
	}
	
	public static int CheckRAWtags(int Pass, cmsHPROFILE hProfile)
	{
	    byte[] Buffer = new byte[7];
	    
	    switch (Pass)
	    {
	        case 1:
	            return lcms2.cmsWriteRawTag(hProfile, 0x31323334, "data123".getBytes(), 7) ? 1 : 0;
	            
	        case 2:
	            if (lcms2.cmsReadRawTag(hProfile, 0x31323334, Buffer, 7) == 0)
	            {
	            	return 0;
	            }
	            
	            if (strcmp(new StringBuffer(new String(Buffer)), "data123") != 0)
	            {
	            	return 0;
	            }
	            return 1;
	            
	        default:
	            return 0;
	    }
	}
	
	// This is a very big test that checks every single tag
	private static final TestFn CheckProfileCreation = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE h;
		    int Pass;
		    
		    h = lcms2.cmsCreateProfilePlaceholder(DbgThread());
		    if (h == null)
		    {
		    	return 0;
		    }
		    
		    if (lcms2.cmsGetTagCount(h) != 0)
		    {
		    	Fail("Empty profile with nonzero number of tags", null);
		    	return 0;
		    }
		    if (lcms2.cmsIsTag(h, lcms2.cmsSigAToB0Tag))
		    {
		    	Fail("Found a tag in an empty profile", null);
		    	return 0;
		    }
		    
		    lcms2.cmsSetColorSpace(h, lcms2.cmsSigRgbData);
		    if (lcms2.cmsGetColorSpace(h) != lcms2.cmsSigRgbData)
		    {
		    	Fail("Unable to set colorspace", null);
		    	return 0;
		    }
		    
		    lcms2.cmsSetPCS(h, lcms2.cmsSigLabData);
		    if (lcms2.cmsGetPCS(h) != lcms2.cmsSigLabData)
		    {
		    	Fail("Unable to set colorspace", null);
		    	return 0;
		    }

		    lcms2.cmsSetDeviceClass(h, lcms2.cmsSigDisplayClass);
		    if (lcms2.cmsGetDeviceClass(h) != lcms2.cmsSigDisplayClass)
		    {
		    	Fail("Unable to set deviceclass", null);
		    	return 0;
		    }
		    
		    lcms2.cmsSetHeaderRenderingIntent(h, lcms2.INTENT_SATURATION);
		    if (lcms2.cmsGetHeaderRenderingIntent(h) != lcms2.INTENT_SATURATION)
		    {
		    	Fail("Unable to set rendering intent", null);
		    	return 0;
		    }
		    
		    for (Pass = 1; Pass <= 2; Pass++)
		    {
		        SubTest("Tags holding XYZ", null);
		        
		        if (CheckXYZ(Pass, h, lcms2.cmsSigBlueColorantTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckXYZ(Pass, h, lcms2.cmsSigGreenColorantTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckXYZ(Pass, h, lcms2.cmsSigRedColorantTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckXYZ(Pass, h, lcms2.cmsSigMediaBlackPointTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckXYZ(Pass, h, lcms2.cmsSigMediaWhitePointTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckXYZ(Pass, h, lcms2.cmsSigLuminanceTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding curves", null);
		        
		        if (CheckGamma(Pass, h, lcms2.cmsSigBlueTRCTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckGamma(Pass, h, lcms2.cmsSigGrayTRCTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckGamma(Pass, h, lcms2.cmsSigGreenTRCTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckGamma(Pass, h, lcms2.cmsSigRedTRCTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding text", null);
		        
		        if (CheckText(Pass, h, lcms2.cmsSigCharTargetTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckText(Pass, h, lcms2.cmsSigCopyrightTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckText(Pass, h, lcms2.cmsSigProfileDescriptionTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckText(Pass, h, lcms2.cmsSigDeviceMfgDescTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckText(Pass, h, lcms2.cmsSigDeviceModelDescTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckText(Pass, h, lcms2.cmsSigViewingCondDescTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckText(Pass, h, lcms2.cmsSigScreeningDescTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding cmsICCData", null);
		        
		        if (CheckData(Pass, h, lcms2.cmsSigPs2CRD0Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckData(Pass, h, lcms2.cmsSigPs2CRD1Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckData(Pass, h, lcms2.cmsSigPs2CRD2Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckData(Pass, h, lcms2.cmsSigPs2CRD3Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckData(Pass, h, lcms2.cmsSigPs2CSATag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckData(Pass, h, lcms2.cmsSigPs2RenderingIntentTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding signatures", null);
		        
		        if (CheckSignature(Pass, h, lcms2.cmsSigColorimetricIntentImageStateTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckSignature(Pass, h, lcms2.cmsSigPerceptualRenderingIntentGamutTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckSignature(Pass, h, lcms2.cmsSigSaturationRenderingIntentGamutTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckSignature(Pass, h, lcms2.cmsSigTechnologyTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding date_time", null);
		        
		        if (CheckDateTime(Pass, h, lcms2.cmsSigCalibrationDateTimeTag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckDateTime(Pass, h, lcms2.cmsSigDateTimeTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding named color lists", null);
		        
		        if (CheckNamedColor(Pass, h, lcms2.cmsSigColorantTableTag, 15, false) == 0)
		        {
		        	return 0;
		        }
		        if (CheckNamedColor(Pass, h, lcms2.cmsSigColorantTableOutTag, 15, false) == 0)
		        {
		        	return 0;
		        }
		        if (CheckNamedColor(Pass, h, lcms2.cmsSigNamedColor2Tag, 4096, true) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding LUTs", null);
		        
		        if (CheckLUT(Pass, h, lcms2.cmsSigAToB0Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckLUT(Pass, h, lcms2.cmsSigAToB1Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckLUT(Pass, h, lcms2.cmsSigAToB2Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckLUT(Pass, h, lcms2.cmsSigBToA0Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckLUT(Pass, h, lcms2.cmsSigBToA1Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckLUT(Pass, h, lcms2.cmsSigBToA2Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckLUT(Pass, h, lcms2.cmsSigPreview0Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckLUT(Pass, h, lcms2.cmsSigPreview1Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckLUT(Pass, h, lcms2.cmsSigPreview2Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckLUT(Pass, h, lcms2.cmsSigGamutTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding CHAD", null);
		        if (CheckCHAD(Pass, h, lcms2.cmsSigChromaticAdaptationTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding Chromaticity", null);
		        if (CheckChromaticity(Pass, h, lcms2.cmsSigChromaticityTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding colorant order", null);
		        if (CheckColorantOrder(Pass, h, lcms2.cmsSigColorantOrderTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding measurement", null);
		        if (CheckMeasurement(Pass, h, lcms2.cmsSigMeasurementTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding CRD info", null);
		        if (CheckCRDinfo(Pass, h, lcms2.cmsSigCrdInfoTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding UCR/BG", null);
		        if (CheckUcrBg(Pass, h, lcms2.cmsSigUcrBgTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding MPE", null);
		        if (CheckMPE(Pass, h, lcms2.cmsSigDToB0Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckMPE(Pass, h, lcms2.cmsSigDToB1Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckMPE(Pass, h, lcms2.cmsSigDToB2Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckMPE(Pass, h, lcms2.cmsSigDToB3Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckMPE(Pass, h, lcms2.cmsSigBToD0Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckMPE(Pass, h, lcms2.cmsSigBToD1Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckMPE(Pass, h, lcms2.cmsSigBToD2Tag) == 0)
		        {
		        	return 0;
		        }
		        if (CheckMPE(Pass, h, lcms2.cmsSigBToD3Tag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags using screening", null);
		        if (CheckScreening(Pass, h, lcms2.cmsSigScreeningTag) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding profile sequence description", null);
		        if (CheckProfileSequenceTag(Pass, h) == 0)
		        {
		        	return 0;
		        }
		        if (CheckProfileSequenceIDTag(Pass, h) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("Tags holding ICC viewing conditions", null);
		        if (CheckICCViewingConditions(Pass, h) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("VCGT tags", null);
		        if (CheckVCGT(Pass, h) == 0)
		        {
		        	return 0;
		        }
		        
		        SubTest("RAW tags", null);
		        if (CheckRAWtags(Pass, h) == 0)
		        {
		        	return 0;
		        }
		        
		        if (Pass == 1)
		        {
		        	lcms2.cmsSaveProfileToFile(h, FILE_PREFIX + "alltags.icc");
		        	lcms2.cmsCloseProfile(h);
		            h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "alltags.icc", "r");
		        }
		    }
		    
		    /*    
		    Not implemented (by design):
		    
		    cmsSigDataTag                           = 0x64617461,  // 'data'  -- Unused   
		    cmsSigDeviceSettingsTag                 = 0x64657673,  // 'devs'  -- Unused
		    cmsSigNamedColorTag                     = 0x6E636f6C,  // 'ncol'  -- Don't use this one, deprecated by ICC
		    cmsSigOutputResponseTag                 = 0x72657370,  // 'resp'  -- Possible patent on this 
		    */
		    
		    lcms2.cmsCloseProfile(h);
		    fileOperation("alltags.icc", remove);
		    return 1;
		}
	};
	
	// Error reporting  -------------------------------------------------------------------------------------------------------
	
	private static final cmsLogErrorHandlerFunction ErrorReportingFunction = new cmsLogErrorHandlerFunction()
	{
		public void run(cmsContext ContextID, int ErrorCode, String Text)
		{
			TrappedError = true;
		    SimultaneousErrors++;
		    Utility.strncpy(ReasonToFailBuffer, Text, TEXT_ERROR_BUFFER_SIZE-1);
		}
	};
	
	private static final TestFn CheckBadProfiles = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE h;
			
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "IDoNotExist.icc", "r");
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		        return 0;
		    }
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "IAmIllFormed*.icc", "r");
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		        return 0;
		    }
		    
		    // No profile name given
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), "", "r");
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		        return 0;
		    }
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "..", "r");
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		        return 0;
		    }
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "IHaveBadAccessMode.icc", "@");
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		        return 0;
		    }
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "bad.icc", "r");
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		        return 0;
		    }
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "toosmall.icc", "r");
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		        return 0;
		    }
		    
		    h = lcms2.cmsOpenProfileFromMemTHR(DbgThread(), null, 3);
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		        return 0;
		    }
		    
		    h = lcms2.cmsOpenProfileFromMemTHR(DbgThread(), "123".getBytes(), 3);
		    if (h != null)
		    {
		    	lcms2.cmsCloseProfile(h);
		        return 0;
		    }
		    
		    if (SimultaneousErrors != 9)
		    {
		    	return 0;      
		    }
		    
		    return 1;
		}
	};
	
	private static final TestFn CheckErrReportingOnBadProfiles = new TestFn()
	{
		public int run()
		{
			int rc;
			
		    lcms2.cmsSetLogErrorHandler(ErrorReportingFunction);
		    rc = CheckBadProfiles.run();
		    lcms2.cmsSetLogErrorHandler(FatalErrorQuit);
		    
		    // Reset the error state
		    TrappedError = false;
		    return rc;
		}
	};
	
	private static final TestFn CheckBadTransforms = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE h1 = lcms2.cmsCreate_sRGBProfile();
			cmsHTRANSFORM x1;
			
			x1 = lcms2.cmsCreateTransform(null, 0, null, 0, 0, 0);
			if (x1 != null)
			{
				lcms2.cmsDeleteTransform(x1);
				return 0;
			}
			
			x1 = lcms2.cmsCreateTransform(h1, 0, h1, 0, 0, 0);
		    if (x1 != null)
		    {
		    	lcms2.cmsDeleteTransform(x1);
				return 0;
			}
		    
			x1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_RGB_8, h1, lcms2.TYPE_RGB_8, 12345, 0);
			if (x1 != null)
			{
				lcms2.cmsDeleteTransform(x1);
				return 0;
			}
			
		    x1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_CMYK_8, h1, lcms2.TYPE_RGB_8, 0, 0);
			if (x1 != null)
			{
				lcms2.cmsDeleteTransform(x1);
				return 0;
			}
			
			x1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_RGB_8, h1, lcms2.TYPE_CMYK_8, 1, 0);
			if (x1 != null)
			{
				lcms2.cmsDeleteTransform(x1);
				return 0;
			}
			
			// sRGB does its output as XYZ!
			x1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_RGB_8, null, lcms2.TYPE_Lab_8, 1, 0);
			if (x1 != null)
			{
				lcms2.cmsDeleteTransform(x1);
				return 0;
			}
			
			lcms2.cmsCloseProfile(h1);
			return 1;
		}
	};
	
	private static final TestFn CheckErrReportingOnBadTransforms = new TestFn()
	{
		public int run()
		{
			int rc;
			
			lcms2.cmsSetLogErrorHandler(ErrorReportingFunction);
		    rc = CheckBadTransforms.run();
		    lcms2.cmsSetLogErrorHandler(FatalErrorQuit);
		    
		    // Reset the error state
		    TrappedError = false;
		    return rc;
		}
	};
	
	// ---------------------------------------------------------------------------------------------------------
	
	// Check a linear xform
	private static int Check8linearXFORM(cmsHTRANSFORM xform, int nChan)
	{
	    int n2, i, j;
	    byte[] Inw = new byte[lcms2.cmsMAXCHANNELS], Outw = new byte[lcms2.cmsMAXCHANNELS];
	    
	    n2=0;
	    
	    for (j=0; j < 0xFF; j++)
	    {
	    	Arrays.fill(Inw, (byte)j);
	        lcms2.cmsDoTransform(xform, Inw, Outw, 1);
	        
	        for (i=0; i < nChan; i++)
	        {
	        	int dif = Math.abs((Outw[i] & 0xFF) - j);
	        	if (dif > n2)
	        	{
	        		n2 = dif;
	        	}
	        }
	    }
	    
	    // We allow 2 contone of difference on 8 bits 
	    if (n2 > 2)
	    {
	        Fail("Differences too big (%x)", new Object[]{new Integer(n2)});
	        return 0;
	    }
	    
	    return 1;
	}
	
	private static int Compare8bitXFORM(cmsHTRANSFORM xform1, cmsHTRANSFORM xform2, int nChan)
	{
	    int n2, i, j;
	    byte[] Inw = new byte[lcms2.cmsMAXCHANNELS], Outw1 = new byte[lcms2.cmsMAXCHANNELS], Outw2 = new byte[lcms2.cmsMAXCHANNELS];;
	    
	    n2=0;
	    
	    for (j=0; j < 0xFF; j++)
	    {
	    	Arrays.fill(Inw, (byte)j);
	        lcms2.cmsDoTransform(xform1, Inw, Outw1, 1);
	        lcms2.cmsDoTransform(xform2, Inw, Outw2, 1);
	        
	        for (i=0; i < nChan; i++)
	        {
	        	int dif = Math.abs((Outw2[i] & 0xFF) - (Outw1[i] & 0xFF));
	        	if (dif > n2)
	        	{
	        		n2 = dif;
	        	}
	        }
	    }
	    
	    // We allow 2 contone of difference on 8 bits 
	    if (n2 > 2)
	    {
	        Fail("Differences too big (%x)", new Object[]{new Integer(n2)});
	        return 0;
	    }
	    
	    return 1;
	}
	
	// Check a linear xform
	private static int Check16linearXFORM(cmsHTRANSFORM xform, int nChan)
	{
	    int n2, i, j;
	    short[] Inw = new short[lcms2.cmsMAXCHANNELS], Outw = new short[lcms2.cmsMAXCHANNELS];
	    
	    n2=0;    
	    for (j=0; j < 0xFFFF; j++)
	    {
	    	Arrays.fill(Inw, (short)j);
	    	
	        lcms2.cmsDoTransform(xform, Inw, Outw, 1);
	        
	        for (i=0; i < nChan; i++)
	        {
	        	int dif = Math.abs((Outw[i] & 0xFFFF) - j);
	        	if (dif > n2)
	        	{
	        		n2 = dif;
	        	}
	        }
	    }
	    
	    // We allow 2 contone of difference on 16 bits
        if (n2 > 0x200)
        {
        	Fail("Differences too big (%x)", new Object[]{new Integer(n2)});
        	return 0;
        }
	    
	    return 1;
	}
	
	private static int Compare16bitXFORM(cmsHTRANSFORM xform1, cmsHTRANSFORM xform2, int nChan)
	{
	    int n2, i, j;
	    short[] Inw = new short[lcms2.cmsMAXCHANNELS], Outw1 = new short[lcms2.cmsMAXCHANNELS], Outw2 = new short[lcms2.cmsMAXCHANNELS];;
	    
	    n2=0;
	    
	    for (j=0; j < 0xFFFF; j++)
	    {
	    	Arrays.fill(Inw, (short)j);
	    	
	        lcms2.cmsDoTransform(xform1, Inw, Outw1, 1);
	        lcms2.cmsDoTransform(xform2, Inw, Outw2, 1);
	        
	        for (i=0; i < nChan; i++)
	        {
	        	int dif = Math.abs((Outw2[i] & 0xFFFF) - (Outw1[i] & 0xFFFF));
	        	if (dif > n2)
	        	{
	        		n2 = dif;
	        	}
	        }
	    }
	    
	    // We allow 2 contone of difference on 16 bits 
	    if (n2 > 0x200) {

	        Fail("Differences too big (%x)", new Object[]{new Integer(n2)});
	        return 0;
	    }
	    
	    return 1;
	}
	
	// Check a linear xform
	private static int CheckFloatlinearXFORM(cmsHTRANSFORM xform, int nChan)
	{
	    int n2, i, j;
	    float[] In = new float[lcms2.cmsMAXCHANNELS], Out = new float[lcms2.cmsMAXCHANNELS];
	    
	    n2=0;
	    
	    for (j=0; j < 0xFFFF; j++)
	    {
	        for (i=0; i < nChan; i++)
	        {
	        	In[i] = (j / 65535.0f);
	        }
	        
	        lcms2.cmsDoTransform(xform, In, Out, 1);
	        
	        for (i=0; i < nChan; i++)
	        {
	        	// We allow no difference in floating point
	            if (!IsGoodFixed15_16("linear xform cmsFloat32Number", Out[i], (j / 65535.0f)))
	            {
	            	return 0;
	            }
	        }        
	    }
	    
	    return 1;
	}
	
	// Check a linear xform
	private static int CompareFloatXFORM(cmsHTRANSFORM xform1, cmsHTRANSFORM xform2, int nChan)
	{
	    int n2, i, j;
	    float[] In = new float[lcms2.cmsMAXCHANNELS], Out1 = new float[lcms2.cmsMAXCHANNELS], Out2 = new float[lcms2.cmsMAXCHANNELS];
	    
	    n2=0;
	    
	    for (j=0; j < 0xFFFF; j++)
	    {
	        for (i=0; i < nChan; i++)
	        {
	        	In[i] = (j / 65535.0f);
	        }
	        
	        lcms2.cmsDoTransform(xform1, In, Out1, 1);
	        lcms2.cmsDoTransform(xform2, In, Out2, 1);
	        
	        for (i=0; i < nChan; i++)
	        {
	        	// We allow no difference in floating point
	            if (!IsGoodFixed15_16("linear xform cmsFloat32Number", Out1[i], Out2[i]))
	            {
	            	return 0;
	            }
	        }
	    }
	    
	    return 1;
	}
	
	// Curves only transforms ----------------------------------------------------------------------------------------
	
	private static final TestFn CheckCurvesOnlyTransforms = new TestFn()
	{
		public int run()
		{
			cmsHTRANSFORM xform1, xform2;
		    cmsHPROFILE h1, h2, h3;
		    cmsToneCurve c1, c2, c3;
		    int rc = 1;
		    
		    c1 = lcms2.cmsBuildGamma(DbgThread(), 2.2);
		    c2 = lcms2.cmsBuildGamma(DbgThread(), 1/2.2);
		    c3 = lcms2.cmsBuildGamma(DbgThread(), 4.84);
		    
		    h1 = lcms2.cmsCreateLinearizationDeviceLinkTHR(DbgThread(), lcms2.cmsSigGrayData, new cmsToneCurve[]{c1});
		    h2 = lcms2.cmsCreateLinearizationDeviceLinkTHR(DbgThread(), lcms2.cmsSigGrayData, new cmsToneCurve[]{c2});
		    h3 = lcms2.cmsCreateLinearizationDeviceLinkTHR(DbgThread(), lcms2.cmsSigGrayData, new cmsToneCurve[]{c3});
		    
		    SubTest("Gray float optimizeable transform", null);
		    xform1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_GRAY_FLT, h2, lcms2.TYPE_GRAY_FLT, lcms2.INTENT_PERCEPTUAL, 0);    
		    rc &= CheckFloatlinearXFORM(xform1, 1);
		    lcms2.cmsDeleteTransform(xform1);
		    if (rc == 0)
		    {
		    	lcms2.cmsCloseProfile(h1); lcms2.cmsCloseProfile(h2); lcms2.cmsCloseProfile(h3);
			    lcms2.cmsFreeToneCurve(c1); lcms2.cmsFreeToneCurve(c2); lcms2.cmsFreeToneCurve(c3);
			    
			    return rc;
		    }
		    
		    SubTest("Gray 8 optimizeable transform", null);
		    xform1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_GRAY_8, h2, lcms2.TYPE_GRAY_8, lcms2.INTENT_PERCEPTUAL, 0);    
		    rc &= Check8linearXFORM(xform1, 1);
		    lcms2.cmsDeleteTransform(xform1);
		    if (rc == 0)
		    {
		    	lcms2.cmsCloseProfile(h1); lcms2.cmsCloseProfile(h2); lcms2.cmsCloseProfile(h3);
			    lcms2.cmsFreeToneCurve(c1); lcms2.cmsFreeToneCurve(c2); lcms2.cmsFreeToneCurve(c3);
			    
			    return rc;
		    }
		    
		    SubTest("Gray 16 optimizeable transform", null);
		    xform1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_GRAY_16, h2, lcms2.TYPE_GRAY_16, lcms2.INTENT_PERCEPTUAL, 0);  
		    rc &= Check16linearXFORM(xform1, 1);
		    lcms2.cmsDeleteTransform(xform1);
		    if (rc == 0)
		    {
		    	lcms2.cmsCloseProfile(h1); lcms2.cmsCloseProfile(h2); lcms2.cmsCloseProfile(h3);
			    lcms2.cmsFreeToneCurve(c1); lcms2.cmsFreeToneCurve(c2); lcms2.cmsFreeToneCurve(c3);
			    
			    return rc;
		    }
		    
		    SubTest("Gray float non-optimizeable transform", null);
		    xform1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_GRAY_FLT, h1, lcms2.TYPE_GRAY_FLT, lcms2.INTENT_PERCEPTUAL, 0);    
		    xform2 = lcms2.cmsCreateTransform(h3, lcms2.TYPE_GRAY_FLT, null, lcms2.TYPE_GRAY_FLT, lcms2.INTENT_PERCEPTUAL, 0);  
		    
		    rc &= CompareFloatXFORM(xform1, xform2, 1);
		    lcms2.cmsDeleteTransform(xform1);
		    lcms2.cmsDeleteTransform(xform2);
		    if (rc == 0)
		    {
		    	lcms2.cmsCloseProfile(h1); lcms2.cmsCloseProfile(h2); lcms2.cmsCloseProfile(h3);
			    lcms2.cmsFreeToneCurve(c1); lcms2.cmsFreeToneCurve(c2); lcms2.cmsFreeToneCurve(c3);
			    
			    return rc;
		    }
		    
		    SubTest("Gray 8 non-optimizeable transform", null);
		    xform1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_GRAY_8, h1, lcms2.TYPE_GRAY_8, lcms2.INTENT_PERCEPTUAL, 0);    
		    xform2 = lcms2.cmsCreateTransform(h3, lcms2.TYPE_GRAY_8, null, lcms2.TYPE_GRAY_8, lcms2.INTENT_PERCEPTUAL, 0);  
		    
		    rc &= Compare8bitXFORM(xform1, xform2, 1);
		    lcms2.cmsDeleteTransform(xform1);
		    lcms2.cmsDeleteTransform(xform2);
		    if (rc == 0)
		    {
		    	lcms2.cmsCloseProfile(h1); lcms2.cmsCloseProfile(h2); lcms2.cmsCloseProfile(h3);
			    lcms2.cmsFreeToneCurve(c1); lcms2.cmsFreeToneCurve(c2); lcms2.cmsFreeToneCurve(c3);
			    
			    return rc;
		    }
		    
		    SubTest("Gray 16 non-optimizeable transform", null);
		    xform1 = lcms2.cmsCreateTransform(h1, lcms2.TYPE_GRAY_16, h1, lcms2.TYPE_GRAY_16, lcms2.INTENT_PERCEPTUAL, 0);  
		    xform2 = lcms2.cmsCreateTransform(h3, lcms2.TYPE_GRAY_16, null, lcms2.TYPE_GRAY_16, lcms2.INTENT_PERCEPTUAL, 0);    
		    
		    rc &= Compare16bitXFORM(xform1, xform2, 1);
		    lcms2.cmsDeleteTransform(xform1);
		    lcms2.cmsDeleteTransform(xform2);
		    lcms2.cmsCloseProfile(h1); lcms2.cmsCloseProfile(h2); lcms2.cmsCloseProfile(h3);
		    lcms2.cmsFreeToneCurve(c1); lcms2.cmsFreeToneCurve(c2); lcms2.cmsFreeToneCurve(c3);
		    
		    return rc;
		}
	};
	
	// Lab to Lab trivial transforms ----------------------------------------------------------------------------------------
	
	private static double MaxDE;
	
	private static int CheckOneLab(cmsHTRANSFORM xform, double L, double a, double b)
	{
	    cmsCIELab In = new cmsCIELab(), Out = new cmsCIELab();
	    double dE;
	    
	    In.L = L; In.a = a; In.b = b;
	    lcms2.cmsDoTransform(xform, In, Out, 1);
	    
	    dE = lcms2.cmsDeltaE(In, Out);
	    
	    if (dE > MaxDE)
	    {
	    	MaxDE = dE;
	    }
	    
	    if (MaxDE >  0.003)
	    {
	        Fail("dE=%f Lab1=(%f, %f, %f)\n\tLab2=(%f %f %f)", new Object[]{new Double(MaxDE), new Double(In.L), new Double(In.a), new Double(In.b),
	        		new Double(Out.L), new Double(Out.a), new Double(Out.b)});
	        lcms2.cmsDoTransform(xform, In, Out, 1);
	        return 0;
	    }
	    
	    return 1;
	}
	
	// Check several Lab, slicing at non-exact values. Precision should be 16 bits. 50x50x50 checks aprox.
	private static int CheckSeveralLab(cmsHTRANSFORM xform)
	{
	    int L, a, b;
	    
	    MaxDE = 0;
	    for (L=0; L < 65536; L += 1311)
	    {
	        for (a = 0; a < 65536; a += 1232)
	        {
	            for (b = 0; b < 65536; b += 1111)
	            {
	                if (CheckOneLab(xform, (L * 100.0) / 65535.0, (a  / 257.0) - 128, (b / 257.0) - 128) == 0)
	                {
	                	return 0;
	                }
	            }
	        }
	    }
	    return 1;
	}
	
	private static int OneTrivialLab(cmsHPROFILE hLab1, cmsHPROFILE hLab2, final String txt)
	{   
	    cmsHTRANSFORM xform;
	    int rc;
	    
	    SubTest(txt, null);
	    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hLab1, lcms2.TYPE_Lab_DBL, hLab2, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
	    lcms2.cmsCloseProfile(hLab1); lcms2.cmsCloseProfile(hLab2);
	    
	    rc = CheckSeveralLab(xform);
	    lcms2.cmsDeleteTransform(xform);
	    return rc;
	}
	
	private static final TestFn CheckFloatLabTransforms = new TestFn()
	{
		public int run()
		{
			return (OneTrivialLab(lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null), lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null),  "Lab4/Lab4") != 0 &&
	           OneTrivialLab(lcms2.cmsCreateLab2ProfileTHR(DbgThread(), null), lcms2.cmsCreateLab2ProfileTHR(DbgThread(), null),  "Lab2/Lab2") != 0 &&
	           OneTrivialLab(lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null), lcms2.cmsCreateLab2ProfileTHR(DbgThread(), null),  "Lab4/Lab2") != 0 &&
	           OneTrivialLab(lcms2.cmsCreateLab2ProfileTHR(DbgThread(), null), lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null),  "Lab2/Lab4") != 0) ? 1 : 0;
		}
	};
	
	private static final TestFn CheckEncodedLabTransforms = new TestFn()
	{
		public int run()
		{
			cmsHTRANSFORM xform;
		    short[] In = new short[3];
		    cmsCIELab Lab = new cmsCIELab();
		    cmsCIELab White = new cmsCIELab();
		    White.L = 100;
		    cmsHPROFILE hLab1 = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    cmsHPROFILE hLab2 = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hLab1, lcms2.TYPE_Lab_16, hLab2, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hLab1); lcms2.cmsCloseProfile(hLab2);
		    
		    In[0] = (short)0xFFFF;
		    In[1] = (short)0x8080;
		    In[2] = (short)0x8080;
		    
		    lcms2.cmsDoTransform(xform, In, Lab, 1);
		    
		    if (lcms2.cmsDeltaE(Lab, White) > 0.0001)
		    {
		    	return 0;
		    }
		    lcms2.cmsDeleteTransform(xform);
		    
		    hLab1 = lcms2.cmsCreateLab2ProfileTHR(DbgThread(), null);
		    hLab2 = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hLab1, lcms2.TYPE_LabV2_16, hLab2, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hLab1); lcms2.cmsCloseProfile(hLab2);
		    
		    In[0] = (short)0xFF00;
		    In[1] = (short)0x8000;
		    In[2] = (short)0x8000;
		    
		    lcms2.cmsDoTransform(xform, In, Lab, 1);
		    
		    if (lcms2.cmsDeltaE(Lab, White) > 0.0001)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsDeleteTransform(xform);

		    hLab2 = lcms2.cmsCreateLab2ProfileTHR(DbgThread(), null);
		    hLab1 = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hLab1, lcms2.TYPE_Lab_DBL, hLab2, lcms2.TYPE_LabV2_16, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hLab1); lcms2.cmsCloseProfile(hLab2);
		    
		    Lab.L = 100;
		    Lab.a = 0;
		    Lab.b = 0;
		    
		    lcms2.cmsDoTransform(xform, Lab, In, 1);
		    if (In[0] != (short)0xFF00 ||
		        In[1] != (short)0x8000 ||
		        In[2] != (short)0x8000)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    
		    hLab1 = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    hLab2 = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hLab1, lcms2.TYPE_Lab_DBL, hLab2, lcms2.TYPE_Lab_16, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hLab1); lcms2.cmsCloseProfile(hLab2);
		    
		    Lab.L = 100;
		    Lab.a = 0;
		    Lab.b = 0;
		    
		    lcms2.cmsDoTransform(xform, Lab, In, 1);
		    
		    if (In[0] != (short)0xFFFF ||
		        In[1] != (short)0x8080 ||
		        In[2] != (short)0x8080)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    
		    return 1;
		}
	};
	
	private static final TestFn CheckStoredIdentities = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hLab, hLink, h4, h2;
		    cmsHTRANSFORM xform;
		    int rc = 1;
		    
		    hLab  = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hLab, lcms2.TYPE_Lab_8, hLab, lcms2.TYPE_Lab_8, 0, 0);
		    
		    hLink = lcms2.cmsTransform2DeviceLink(xform, 3.4, 0);
		    lcms2.cmsSaveProfileToFile(hLink, FILE_PREFIX + "abstractv2.icc");
		    lcms2.cmsCloseProfile(hLink);
		    
		    hLink = lcms2.cmsTransform2DeviceLink(xform, 4.2, 0);
		    lcms2.cmsSaveProfileToFile(hLink, FILE_PREFIX + "abstractv4.icc");
		    lcms2.cmsCloseProfile(hLink);
		    
		    lcms2.cmsDeleteTransform(xform);
		    lcms2.cmsCloseProfile(hLab);
		    
		    h4 = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "abstractv4.icc", "r");
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), h4, lcms2.TYPE_Lab_DBL, h4, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    
		    SubTest("V4", null);
		    rc &= CheckSeveralLab(xform);
		    
		    lcms2.cmsDeleteTransform(xform);
		    lcms2.cmsCloseProfile(h4);
		    if (rc == 0)
		    {
		    	fileOperation("abstractv2.icc", remove);
			    fileOperation("abstractv4.icc", remove);
			    return rc;
		    }
		    
		    SubTest("V2", null);
		    h2 = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "abstractv2.icc", "r");
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), h2, lcms2.TYPE_Lab_DBL, h2, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    rc &= CheckSeveralLab(xform);
		    lcms2.cmsDeleteTransform(xform);
		    lcms2.cmsCloseProfile(h2);
		    if (rc == 0)
		    {
		    	fileOperation("abstractv2.icc", remove);
			    fileOperation("abstractv4.icc", remove);
			    return rc;
		    }
		    
		    SubTest("V2 -> V4", null);
		    h2 = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "abstractv2.icc", "r");
		    h4 = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "abstractv4.icc", "r");
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), h4, lcms2.TYPE_Lab_DBL, h2, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    rc &= CheckSeveralLab(xform);
		    lcms2.cmsDeleteTransform(xform);
		    lcms2.cmsCloseProfile(h2);
		    lcms2.cmsCloseProfile(h4);
		    
		    SubTest("V4 -> V2", null);
		    h2 = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "abstractv2.icc", "r");
		    h4 = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "abstractv4.icc", "r");
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), h2, lcms2.TYPE_Lab_DBL, h4, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    rc &= CheckSeveralLab(xform);
		    lcms2.cmsDeleteTransform(xform);
		    lcms2.cmsCloseProfile(h2);
		    lcms2.cmsCloseProfile(h4);
		    
		    fileOperation("abstractv2.icc", remove);
		    fileOperation("abstractv4.icc", remove);
		    return rc;
		}
	};
	
	// Check a simple xform from a matrix profile to itself. Test floating point accuracy.
	private static final TestFn CheckMatrixShaperXFORMFloat = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hAbove, hSRGB;
		    cmsHTRANSFORM xform;
		    int rc1, rc2;
		    
		    hAbove = Create_AboveRGB();
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hAbove, lcms2.TYPE_RGB_FLT, hAbove, lcms2.TYPE_RGB_FLT, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hAbove);
		    rc1 = CheckFloatlinearXFORM(xform, 3);
		    lcms2.cmsDeleteTransform(xform);
		    
		    hSRGB = lcms2.cmsCreate_sRGBProfileTHR(DbgThread());
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hSRGB, lcms2.TYPE_RGB_FLT, hSRGB, lcms2.TYPE_RGB_FLT, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hSRGB);
		    rc2 = CheckFloatlinearXFORM(xform, 3);
		    lcms2.cmsDeleteTransform(xform);
		    
		    return (rc1 != 0 && rc2 != 0) ? 1 : 0;
		}
	};
	
	// Check a simple xform from a matrix profile to itself. Test 16 bits accuracy.
	private static final TestFn CheckMatrixShaperXFORM16 = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hAbove, hSRGB;
		    cmsHTRANSFORM xform;
		    int rc1, rc2;
		    
		    hAbove = Create_AboveRGB();
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hAbove, lcms2.TYPE_RGB_16, hAbove, lcms2.TYPE_RGB_16, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hAbove);
		    rc1 = Check16linearXFORM(xform, 3);
		    lcms2.cmsDeleteTransform(xform);
		    
		    hSRGB = lcms2.cmsCreate_sRGBProfileTHR(DbgThread());
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hSRGB, lcms2.TYPE_RGB_16, hSRGB, lcms2.TYPE_RGB_16, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hSRGB);
		    rc2 = Check16linearXFORM(xform, 3);
		    lcms2.cmsDeleteTransform(xform);
		    
		    return (rc1 != 0 && rc2 != 0) ? 1 : 0;
		}
	};
	
	// Check a simple xform from a matrix profile to itself. Test 8 bits accuracy.
	private static final TestFn CheckMatrixShaperXFORM8 = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hAbove, hSRGB;
		    cmsHTRANSFORM xform;
		    int rc1, rc2;
		    
		    hAbove = Create_AboveRGB();
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hAbove, lcms2.TYPE_RGB_8, hAbove, lcms2.TYPE_RGB_8, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hAbove);
		    rc1 = Check8linearXFORM(xform, 3);
		    lcms2.cmsDeleteTransform(xform);
		    
		    hSRGB = lcms2.cmsCreate_sRGBProfileTHR(DbgThread());
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hSRGB, lcms2.TYPE_RGB_8, hSRGB, lcms2.TYPE_RGB_8, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hSRGB);
		    rc2 = Check8linearXFORM(xform, 3);
		    lcms2.cmsDeleteTransform(xform);
		    
		    return (rc1 != 0 && rc2 != 0) ? 1 : 0;
		}
	};
	
	// TODO: Check LUT based to LUT based transforms for CMYK
	
	// -----------------------------------------------------------------------------------------------------------------
	
	// Check known values going from sRGB to XYZ
	private static int CheckOneRGB_f(cmsHTRANSFORM xform, int R, int G, int B, double X, double Y, double Z, double err)
	{
	    float[] RGB = new float[3];
	    double[] Out = new double[3];
	    
	    RGB[0] = (R / 255.0f);
	    RGB[1] = (G / 255.0f);
	    RGB[2] = (B / 255.0f);
	    
	    lcms2.cmsDoTransform(xform, RGB, Out, 1);
	    
	    return (IsGoodVal("X", X , Out[0], err) &&
	           IsGoodVal("Y", Y , Out[1], err) &&
	           IsGoodVal("Z", Z , Out[2], err)) ? 1 : 0;
	}
	
	private static final TestFn Chack_sRGB_Float = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hsRGB, hXYZ, hLab;
		    cmsHTRANSFORM xform1, xform2;
		    int rc;
		    
		    hsRGB = lcms2.cmsCreate_sRGBProfileTHR(DbgThread());
		    hXYZ  = lcms2.cmsCreateXYZProfileTHR(DbgThread());
		    hLab  = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    
		    xform1 = lcms2.cmsCreateTransformTHR(DbgThread(), hsRGB, lcms2.TYPE_RGB_FLT, hXYZ, lcms2.TYPE_XYZ_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    
		    xform2 = lcms2.cmsCreateTransformTHR(DbgThread(), hsRGB, lcms2.TYPE_RGB_FLT, hLab, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hsRGB);
		    lcms2.cmsCloseProfile(hXYZ);
		    lcms2.cmsCloseProfile(hLab);
		    
		    MaxErr = 0;
		    
		    // Xform 1 goes from 8 bits to XYZ,
		    rc  = CheckOneRGB_f(xform1, 1, 1, 1,        0.0002926, 0.00030352, 0.00025037, 0.0001);
		    rc  &= CheckOneRGB_f(xform1, 127, 127, 127, 0.2046329, 0.212230,   0.175069,   0.0001);
		    rc  &= CheckOneRGB_f(xform1, 12, 13, 15,    0.0038364, 0.0039928,  0.00385212, 0.0001);
		    rc  &= CheckOneRGB_f(xform1, 128, 0, 0,     0.0940846, 0.0480030,  0.00300543, 0.0001);
		    rc  &= CheckOneRGB_f(xform1, 190, 25, 210,  0.3203491, 0.1605240,  0.46817115, 0.0001);
		    
		    // Xform 2 goes from 8 bits to Lab, we allow 0.01 error max
		    rc  &= CheckOneRGB_f(xform2, 1, 1, 1,       0.2741748, 0, 0,                  0.01);
		    rc  &= CheckOneRGB_f(xform2, 127, 127, 127, 53.192776, 0, 0,                  0.01);
		    rc  &= CheckOneRGB_f(xform2, 190, 25, 210,  47.043171, 74.564576, -56.89373,  0.01);
		    rc  &= CheckOneRGB_f(xform2, 128, 0, 0,     26.158100, 48.474477, 39.425916,  0.01);
		    
		    lcms2.cmsDeleteTransform(xform1);
		    lcms2.cmsDeleteTransform(xform2);
		    return rc;
		}
	};
	
	// ---------------------------------------------------

	private static boolean GetProfileRGBPrimaries(cmsHPROFILE hProfile, cmsCIEXYZTRIPLE result, int intent)
	{
	    cmsHPROFILE hXYZ;
	    cmsHTRANSFORM hTransform;
	    double[] rgb = {1., 0., 0.,
	    0., 1., 0.,
	    0., 0., 1.};
	    
	    hXYZ = lcms2.cmsCreateXYZProfile();
	    if (hXYZ == null)
	    {
	    	return false;
	    }
	    
	    hTransform = lcms2.cmsCreateTransform(hProfile, lcms2.TYPE_RGB_DBL, hXYZ, lcms2.TYPE_XYZ_DBL, intent, lcms2.cmsFLAGS_NOCACHE | lcms2.cmsFLAGS_NOOPTIMIZE);
	    lcms2.cmsCloseProfile(hXYZ);
	    if (hTransform == null)
	    {
	    	return false;
	    }
	    
	    lcms2.cmsDoTransform(hTransform, rgb, result, 3);
	    lcms2.cmsDeleteTransform(hTransform);
	    return true;
	}
	
	private static final TestFn CheckRGBPrimaries = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hsRGB;
		    cmsCIEXYZTRIPLE tripXYZ = new cmsCIEXYZTRIPLE();
		    cmsCIExyYTRIPLE tripxyY = new cmsCIExyYTRIPLE();
		    boolean result;
		    
		    hsRGB = lcms2.cmsCreate_sRGBProfileTHR(DbgThread());
		    if (hsRGB == null)
		    {
		    	return 0;
		    }
		    
		    result = GetProfileRGBPrimaries(hsRGB, tripXYZ, lcms2.INTENT_ABSOLUTE_COLORIMETRIC);
		    
		    lcms2.cmsCloseProfile(hsRGB);
		    if (!result)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsXYZ2xyY(tripxyY.Red, tripXYZ.Red);
		    lcms2.cmsXYZ2xyY(tripxyY.Green, tripXYZ.Green);
		    lcms2.cmsXYZ2xyY(tripxyY.Blue, tripXYZ.Blue);
		    
		    /* valus were taken from
		    http://en.wikipedia.org/wiki/RGB_color_spaces#Specifications */
		    
		    if (!IsGoodFixed15_16("xRed", tripxyY.Red.x, 0.64) ||
		        !IsGoodFixed15_16("yRed", tripxyY.Red.y, 0.33) ||
		        !IsGoodFixed15_16("xGreen", tripxyY.Green.x, 0.30) || 
		        !IsGoodFixed15_16("yGreen", tripxyY.Green.y, 0.60) ||
		        !IsGoodFixed15_16("xBlue", tripxyY.Blue.x, 0.15) || 
		        !IsGoodFixed15_16("yBlue", tripxyY.Blue.y, 0.06))
		    {
		    	Fail("One or more primaries are wrong.", null);
		    	return 0;
		    }
		    
		    return 1;
		}
	};
	
	// -----------------------------------------------------------------------------------------------------------------
	
	// This function will check CMYK -> CMYK transforms. It uses FOGRA29 and SWOP ICC profiles
	
	private static int CheckCMYK(int Intent, final String Profile1, final String Profile2)
	{
	    cmsHPROFILE hSWOP  = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), Profile1, "r");
	    cmsHPROFILE hFOGRA = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), Profile2, "r");
	    cmsHTRANSFORM xform, swop_lab, fogra_lab;
	    float[] CMYK1 = new float[4], CMYK2 = new float[4];
	    cmsCIELab Lab1 = new cmsCIELab(), Lab2 = new cmsCIELab();
	    cmsHPROFILE hLab;
	    double DeltaL, Max;
	    int i;
	    
	    hLab = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
	    
	    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hSWOP, lcms2.TYPE_CMYK_FLT, hFOGRA, lcms2.TYPE_CMYK_FLT, Intent, 0);
	    
	    swop_lab = lcms2.cmsCreateTransformTHR(DbgThread(), hSWOP, lcms2.TYPE_CMYK_FLT, hLab, lcms2.TYPE_Lab_DBL, Intent, 0);
	    fogra_lab = lcms2.cmsCreateTransformTHR(DbgThread(), hFOGRA, lcms2.TYPE_CMYK_FLT, hLab, lcms2.TYPE_Lab_DBL, Intent, 0);
	    
	    Max = 0;
	    for (i=0; i <= 100; i++)
	    {
	        CMYK1[0] = 10;
	        CMYK1[1] = 20;
	        CMYK1[2] = 30;
	        CMYK1[3] = (float) i;
	        
	        lcms2.cmsDoTransform(swop_lab, CMYK1, Lab1, 1);
	        lcms2.cmsDoTransform(xform, CMYK1, CMYK2, 1);
	        lcms2.cmsDoTransform(fogra_lab, CMYK2, Lab2, 1);
	        
	        DeltaL = Math.abs(Lab1.L - Lab2.L);
	        
	        if (DeltaL > Max)
	        {
	        	Max = DeltaL;
	        }
	    }
	    
	    lcms2.cmsDeleteTransform(xform);
	    
	    if (Max > 3.0)
	    {
	    	return 0;
	    }
	    
	    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hFOGRA, lcms2.TYPE_CMYK_FLT, hSWOP, lcms2.TYPE_CMYK_FLT, Intent, 0);
	    
	    Max = 0;
	    
	    for (i=0; i <= 100; i++)
	    {
	        CMYK1[0] = 10;
	        CMYK1[1] = 20;
	        CMYK1[2] = 30;
	        CMYK1[3] = (float) i;
	        
	        lcms2.cmsDoTransform(fogra_lab, CMYK1, Lab1, 1);
	        lcms2.cmsDoTransform(xform, CMYK1, CMYK2, 1);
	        lcms2.cmsDoTransform(swop_lab, CMYK2, Lab2, 1);
	        
	        DeltaL = Math.abs(Lab1.L - Lab2.L);
	        
	        if (DeltaL > Max)
	        {
	        	Max = DeltaL;
	        }
	    }
	    
	    lcms2.cmsCloseProfile(hSWOP);
	    lcms2.cmsCloseProfile(hFOGRA);
	    lcms2.cmsCloseProfile(hLab);
	    
	    lcms2.cmsDeleteTransform(xform);
	    lcms2.cmsDeleteTransform(swop_lab);
	    lcms2.cmsDeleteTransform(fogra_lab);
	    
	    return Max < 3.0 ? 1 : 0;
	}
	
	private static final TestFn CheckCMYKRoundtrip = new TestFn()
	{
		public int run()
		{
			return CheckCMYK(lcms2.INTENT_RELATIVE_COLORIMETRIC, FILE_PREFIX + "USWebCoatedSWOP.icc", FILE_PREFIX + "USWebCoatedSWOP.icc");
		}
	};
	
	private static final TestFn CheckCMYKPerceptual = new TestFn()
	{
		public int run()
		{
			return CheckCMYK(lcms2.INTENT_PERCEPTUAL, FILE_PREFIX + "USWebCoatedSWOP.icc", FILE_PREFIX + "UncoatedFOGRA29.icc");
		}
	};
	
	private static final TestFn CheckCMYKRelCol = new TestFn()
	{
		public int run()
		{
			return CheckCMYK(lcms2.INTENT_RELATIVE_COLORIMETRIC, FILE_PREFIX + "USWebCoatedSWOP.icc", FILE_PREFIX + "UncoatedFOGRA29.icc");
		}
	};
	
	private static final TestFn CheckKOnlyBlackPreserving = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hSWOP  = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "USWebCoatedSWOP.icc", "r");
		    cmsHPROFILE hFOGRA = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "UncoatedFOGRA29.icc", "r");
		    cmsHTRANSFORM xform, swop_lab, fogra_lab;
		    float[] CMYK1 = new float[4], CMYK2 = new float[4];
		    cmsCIELab Lab1 = new cmsCIELab(), Lab2 = new cmsCIELab();
		    cmsHPROFILE hLab;
		    double DeltaL, Max;
		    int i;
		    
		    hLab = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hSWOP, lcms2.TYPE_CMYK_FLT, hFOGRA, lcms2.TYPE_CMYK_FLT, lcms2.INTENT_PRESERVE_K_ONLY_PERCEPTUAL, 0);
		    
		    swop_lab = lcms2.cmsCreateTransformTHR(DbgThread(), hSWOP, lcms2.TYPE_CMYK_FLT, hLab, lcms2.TYPE_Lab_DBL, lcms2.INTENT_PERCEPTUAL, 0);
		    fogra_lab = lcms2.cmsCreateTransformTHR(DbgThread(), hFOGRA, lcms2.TYPE_CMYK_FLT, hLab, lcms2.TYPE_Lab_DBL, lcms2.INTENT_PERCEPTUAL, 0);
		    
		    Max = 0;
		    
		    for (i=0; i <= 100; i++)
		    {
		        CMYK1[0] = 0;
		        CMYK1[1] = 0;
		        CMYK1[2] = 0;
		        CMYK1[3] = (float) i;
		        
		        // SWOP CMYK to Lab1
		        lcms2.cmsDoTransform(swop_lab, CMYK1, Lab1, 1);
		        
		        // SWOP To FOGRA using black preservation
		        lcms2.cmsDoTransform(xform, CMYK1, CMYK2, 1);
		        
		        // Obtained FOGRA CMYK to Lab2
		        lcms2.cmsDoTransform(fogra_lab, CMYK2, Lab2, 1);
		        
		        // We care only on L*
		        DeltaL = Math.abs(Lab1.L - Lab2.L);
		        
		        if (DeltaL > Max)
		        {
		        	Max = DeltaL;
		        }
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    
		    // dL should be below 3.0
		    if (Max > 3.0)
		    {
		    	return 0;
		    }
		    
		    // Same, but FOGRA to SWOP
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hFOGRA, lcms2.TYPE_CMYK_FLT, hSWOP, lcms2.TYPE_CMYK_FLT, lcms2.INTENT_PRESERVE_K_ONLY_PERCEPTUAL, 0);
		    
		    Max = 0;
		    
		    for (i=0; i <= 100; i++)
		    {
		        CMYK1[0] = 0;
		        CMYK1[1] = 0;
		        CMYK1[2] = 0;
		        CMYK1[3] = (float) i;
		        
		        lcms2.cmsDoTransform(fogra_lab, CMYK1, Lab1, 1);
		        lcms2.cmsDoTransform(xform, CMYK1, CMYK2, 1);
		        lcms2.cmsDoTransform(swop_lab, CMYK2, Lab2, 1);
		        
		        DeltaL = Math.abs(Lab1.L - Lab2.L);
		        
		        if (DeltaL > Max)
		        {
		        	Max = DeltaL;
		        }
		    }
		    
		    lcms2.cmsCloseProfile(hSWOP);
		    lcms2.cmsCloseProfile(hFOGRA);
		    lcms2.cmsCloseProfile(hLab);
		    
		    lcms2.cmsDeleteTransform(xform);
		    lcms2.cmsDeleteTransform(swop_lab);
		    lcms2.cmsDeleteTransform(fogra_lab);
		    
		    return Max < 3.0 ? 1 : 0;
		}
	};
	
	private static final TestFn CheckKPlaneBlackPreserving = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hSWOP  = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "USWebCoatedSWOP.icc", "r");
		    cmsHPROFILE hFOGRA = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "UncoatedFOGRA29.icc", "r");
		    cmsHTRANSFORM xform, swop_lab, fogra_lab;
		    float[] CMYK1 = new float[4], CMYK2 = new float[4];
		    cmsCIELab Lab1 = new cmsCIELab(), Lab2 = new cmsCIELab();
		    cmsHPROFILE hLab;
		    double DeltaE, Max;
		    int i;
		    
		    hLab = lcms2.cmsCreateLab4ProfileTHR(DbgThread(), null);
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hSWOP, lcms2.TYPE_CMYK_FLT, hFOGRA, lcms2.TYPE_CMYK_FLT, lcms2.INTENT_PERCEPTUAL, 0);
		    
		    swop_lab = lcms2.cmsCreateTransformTHR(DbgThread(), hSWOP, lcms2.TYPE_CMYK_FLT, hLab, lcms2.TYPE_Lab_DBL, lcms2.INTENT_PERCEPTUAL, 0);
		    fogra_lab = lcms2.cmsCreateTransformTHR(DbgThread(), hFOGRA, lcms2.TYPE_CMYK_FLT, hLab, lcms2.TYPE_Lab_DBL, lcms2.INTENT_PERCEPTUAL, 0);
		    
		    Max = 0;
		    
		    for (i=0; i <= 100; i++)
		    {
		        CMYK1[0] = 0;
		        CMYK1[1] = 0;
		        CMYK1[2] = 0;
		        CMYK1[3] = (float) i;
		        
		        lcms2.cmsDoTransform(swop_lab, CMYK1, Lab1, 1);
		        lcms2.cmsDoTransform(xform, CMYK1, CMYK2, 1);
		        lcms2.cmsDoTransform(fogra_lab, CMYK2, Lab2, 1);
		        
		        DeltaE = lcms2.cmsDeltaE(Lab1, Lab2);
		        
		        if (DeltaE > Max)
		        {
		        	Max = DeltaE;
		        }
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    
		    xform = lcms2.cmsCreateTransformTHR(DbgThread(), hFOGRA, lcms2.TYPE_CMYK_FLT, hSWOP, lcms2.TYPE_CMYK_FLT, lcms2.INTENT_PRESERVE_K_PLANE_PERCEPTUAL, 0);
		    
		    for (i=0; i <= 100; i++)
		    {
		        CMYK1[0] = 30;
		        CMYK1[1] = 20;
		        CMYK1[2] = 10;
		        CMYK1[3] = (float) i;
		        
		        lcms2.cmsDoTransform(fogra_lab, CMYK1, Lab1, 1);
		        lcms2.cmsDoTransform(xform, CMYK1, CMYK2, 1);
		        lcms2.cmsDoTransform(swop_lab, CMYK2, Lab2, 1);
		        
		        DeltaE = lcms2.cmsDeltaE(Lab1, Lab2);
		        
		        if (DeltaE > Max)
		        {
		        	Max = DeltaE;
		        }
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    
		    lcms2.cmsCloseProfile(hSWOP);
		    lcms2.cmsCloseProfile(hFOGRA);
		    lcms2.cmsCloseProfile(hLab);
		    
		    lcms2.cmsDeleteTransform(swop_lab);
		    lcms2.cmsDeleteTransform(fogra_lab);
		    
		    return Max < 30.0 ? 1 : 0;
		}
	};
	
	// ------------------------------------------------------------------------------------------------------
	
	private static final TestFn CheckProofingXFORMFloat = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hAbove;
		    cmsHTRANSFORM xform;
		    int rc;
		    
		    hAbove = Create_AboveRGB();
		    xform = lcms2.cmsCreateProofingTransformTHR(DbgThread(), hAbove, lcms2.TYPE_RGB_FLT, hAbove, lcms2.TYPE_RGB_FLT, hAbove, 
		    		lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.cmsFLAGS_SOFTPROOFING);
		    lcms2.cmsCloseProfile(hAbove);
		    rc = CheckFloatlinearXFORM(xform, 3);
		    lcms2.cmsDeleteTransform(xform);
		    return rc;
		}
	};
	
	private static final TestFn CheckProofingXFORM16 = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hAbove;
		    cmsHTRANSFORM xform;
		    int rc;
		    
		    hAbove = Create_AboveRGB();
		    xform = lcms2.cmsCreateProofingTransformTHR(DbgThread(), hAbove, lcms2.TYPE_RGB_16, hAbove, lcms2.TYPE_RGB_16, hAbove, 
		    		lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.cmsFLAGS_SOFTPROOFING|lcms2.cmsFLAGS_NOCACHE);
		    lcms2.cmsCloseProfile(hAbove);
		    rc = Check16linearXFORM(xform, 3);
		    lcms2.cmsDeleteTransform(xform);
		    return rc;
		}
	};
	
	private static final TestFn CheckGamutCheck = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hSRGB, hAbove;
	        cmsHTRANSFORM xform;
	        int rc;
	        short[] Alarm = { (short)0xDEAD, (short)0xBABE, (short)0xFACE };
	        
	        // Set alarm codes to fancy values so we could check the out of gamut condition
	        lcms2.cmsSetAlarmCodes(Alarm);
	        
	        // Create the profiles
	        hSRGB  = lcms2.cmsCreate_sRGBProfileTHR(DbgThread());
	        hAbove = Create_AboveRGB();
	        
	        if (hSRGB == null || hAbove == null)
	        {
	        	return 0; // Failed
	        }
	        
	        SubTest("Gamut check on floating point", null);
	        
	        // Create a gamut checker in the same space. No value should be out of gamut
	        xform = lcms2.cmsCreateProofingTransformTHR(DbgThread(), hAbove, lcms2.TYPE_RGB_FLT, hAbove, lcms2.TYPE_RGB_FLT, hAbove, 
	        		lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.cmsFLAGS_GAMUTCHECK);
	        
	        if (CheckFloatlinearXFORM(xform, 3) == 0)
	        {
	        	lcms2.cmsCloseProfile(hSRGB);
	        	lcms2.cmsCloseProfile(hAbove);
	        	lcms2.cmsDeleteTransform(xform);
	            Fail("Gamut check on same profile failed", null);
	            return 0;
	        }
	        
	        lcms2.cmsDeleteTransform(xform);
	        
	        SubTest("Gamut check on 16 bits", null);
	        
	        xform = lcms2.cmsCreateProofingTransformTHR(DbgThread(), hAbove, lcms2.TYPE_RGB_16, hAbove, lcms2.TYPE_RGB_16, hAbove, 
	        		lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.cmsFLAGS_GAMUTCHECK);
	        
	        lcms2.cmsCloseProfile(hSRGB);
	        lcms2.cmsCloseProfile(hAbove);
	        
	        rc = Check16linearXFORM(xform, 3);
	        
	        lcms2.cmsDeleteTransform(xform);
	        
	        return rc;
		}
	};
	
	// -------------------------------------------------------------------------------------------------------------------
	
	private static final TestFn CheckBlackPoint = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hProfile;
		    cmsCIEXYZ Black = new cmsCIEXYZ();
		    cmsCIELab Lab = new cmsCIELab();
		    
		    hProfile  = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "r");  
		    lcms2.cmsDetectBlackPoint(Black, hProfile, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hProfile);
		    
		    hProfile = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "USWebCoatedSWOP.icc", "r");
		    lcms2.cmsDetectBlackPoint(Black, hProfile, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsXYZ2Lab(null, Lab, Black);
		    lcms2.cmsCloseProfile(hProfile);
		    
		    hProfile = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "lcms2cmyk.icc", "r");
		    lcms2.cmsDetectBlackPoint(Black, hProfile, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsXYZ2Lab(null, Lab, Black);
		    lcms2.cmsCloseProfile(hProfile);
		    
		    hProfile = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "UncoatedFOGRA29.icc", "r");
		    lcms2.cmsDetectBlackPoint(Black, hProfile, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsXYZ2Lab(null, Lab, Black);
		    lcms2.cmsCloseProfile(hProfile);
		    
		    hProfile = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "USWebCoatedSWOP.icc", "r");
		    lcms2.cmsDetectBlackPoint(Black, hProfile, lcms2.INTENT_PERCEPTUAL, 0);
		    lcms2.cmsXYZ2Lab(null, Lab, Black);
		    lcms2.cmsCloseProfile(hProfile);
		    
		    return 1;
		}
	};
	
	private static int CheckOneTAC(double InkLimit)
	{
	    cmsHPROFILE h;
	    double d;
	    
	    h = CreateFakeCMYK(InkLimit, true);
	    lcms2.cmsSaveProfileToFile(h, FILE_PREFIX + "lcmstac.icc");
	    lcms2.cmsCloseProfile(h);
	    
	    h = lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "lcmstac.icc", "r");
	    d = lcms2.cmsDetectTAC(h);
	    lcms2.cmsCloseProfile(h);
	    
	    fileOperation("lcmstac.icc", remove);
	    
	    if (Math.abs(d - InkLimit) > 5)
	    {
	    	return 0;
	    }
	    
	    return 1;
	}
	
	private static final TestFn CheckTAC = new TestFn()
	{
		public int run()
		{
			if (CheckOneTAC(180) == 0)
			{
				return 0;
			}
		    if (CheckOneTAC(220) == 0)
			{
				return 0;
			}
		    if (CheckOneTAC(286) == 0)
			{
				return 0;
			}
		    if (CheckOneTAC(310) == 0)
			{
				return 0;
			}
		    if (CheckOneTAC(330) == 0)
			{
				return 0;
			}
		    
		    return 1;
		}
	};
	
	// -------------------------------------------------------------------------------------------------------
	
	private static final int NPOINTS_IT8 = 10; // (17*17*17*17)
	
	private static final TestFn CheckCGATS = new TestFn()
	{
		public int run()
		{
			cmsHANDLE it8;
		    int i;
		    
		    it8 = lcms2.cmsIT8Alloc(DbgThread());
		    if (it8 == null)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsIT8SetSheetType(it8, "LCMS/TESTING");
		    lcms2.cmsIT8SetPropertyStr(it8, "ORIGINATOR",   "1 2 3 4");
		    lcms2.cmsIT8SetPropertyUncooked(it8, "DESCRIPTOR",   "1234");
		    lcms2.cmsIT8SetPropertyStr(it8, "MANUFACTURER", "3");
		    lcms2.cmsIT8SetPropertyDbl(it8, "CREATED",      4);
		    lcms2.cmsIT8SetPropertyDbl(it8, "SERIAL",       5);
		    lcms2.cmsIT8SetPropertyHex(it8, "MATERIAL",     0x123);
		    
		    lcms2.cmsIT8SetPropertyDbl(it8, "NUMBER_OF_SETS", NPOINTS_IT8);
		    lcms2.cmsIT8SetPropertyDbl(it8, "NUMBER_OF_FIELDS", 4);
		    
		    lcms2.cmsIT8SetDataFormat(it8, 0, "SAMPLE_ID");
		    lcms2.cmsIT8SetDataFormat(it8, 1, "RGB_R");
		    lcms2.cmsIT8SetDataFormat(it8, 2, "RGB_G");
		    lcms2.cmsIT8SetDataFormat(it8, 3, "RGB_B");
		    
		    for (i=0; i < NPOINTS_IT8; i++)
		    {
		    	StringBuffer Patch = new StringBuffer(20);
		    	
		    	Utility.sprintf(Patch, "P%d", new Object[]{new Integer(i)});
		    	
		    	lcms2.cmsIT8SetDataRowCol(it8, i, 0, Patch.toString());
		    	lcms2.cmsIT8SetDataRowColDbl(it8, i, 1, i);
		    	lcms2.cmsIT8SetDataRowColDbl(it8, i, 2, i);
		    	lcms2.cmsIT8SetDataRowColDbl(it8, i, 3, i);
		    }
		    
		    lcms2.cmsIT8SaveToFile(it8, FILE_PREFIX + "TEST.IT8");
		    lcms2.cmsIT8Free(it8);
		    
		    it8 = lcms2.cmsIT8LoadFromFile(DbgThread(), FILE_PREFIX + "TEST.IT8");
		    lcms2.cmsIT8SaveToFile(it8, FILE_PREFIX + "TEST.IT8");
		    lcms2.cmsIT8Free(it8);
		    
		    it8 = lcms2.cmsIT8LoadFromFile(DbgThread(), FILE_PREFIX + "TEST.IT8");
		    
		    if (lcms2.cmsIT8GetPropertyDbl(it8, "DESCRIPTOR") != 1234)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsIT8SetPropertyDbl(it8, "DESCRIPTOR", 5678);
		    
		    if (lcms2.cmsIT8GetPropertyDbl(it8, "DESCRIPTOR") != 5678)
		    {
		        return 0;
		    }
		    
		    if (lcms2.cmsIT8GetDataDbl(it8, "P3", "RGB_G") != 3)
		    {
		        return 0;
		    }
		    
		    lcms2.cmsIT8Free(it8);
		    
		    fileOperation("TEST.IT8", remove);
		    return 1;
		}
	};
	
	// Create CSA/CRD
	
	private static void GenerateCSA(final String cInProf, final String FileName)
	{
	    cmsHPROFILE hProfile;   
	    int n;
	    byte[] Buffer;
	    cmsContext BuffThread = DbgThread();
	    Stream o;
	    
	    if (cInProf == null)
	    {
	    	hProfile = lcms2.cmsCreateLab4Profile(null);
	    }
	    else
	    {
	    	hProfile = lcms2.cmsOpenProfileFromFile(cInProf, "r");
	    }
	    
	    n = lcms2.cmsGetPostScriptCSA(DbgThread(), hProfile, 0, 0, null, 0);
	    if (n == 0)
	    {
	    	return;
	    }
	    
	    Buffer = new byte[n + 1];
	    lcms2.cmsGetPostScriptCSA(DbgThread(), hProfile, 0, 0, Buffer, n);
	    Buffer[n] = 0;
	    
	    if (FileName != null)
	    {
	        o = Stream.fopen(FILE_PREFIX + FileName, "w");
	        o.write(Buffer, 0, n, 1);
	        o.close();
	    }
	    
	    Buffer = null;
	    lcms2.cmsCloseProfile(hProfile);
	    fileOperation(FileName, remove);
	}
	
	private static void GenerateCRD(final String cOutProf, final String FileName)
	{
	    cmsHPROFILE hProfile;
	    int n;
	    byte[] Buffer;
	    int dwFlags = 0;
	    cmsContext BuffThread = DbgThread();
	    
	    if (cOutProf == null)
	    {
	    	hProfile = lcms2.cmsCreateLab4Profile(null);
	    }
	    else
	    {
	    	hProfile = lcms2.cmsOpenProfileFromFile(cOutProf, "r");
	    }
	    
	    n = lcms2.cmsGetPostScriptCRD(DbgThread(), hProfile, 0, dwFlags, null, 0);
	    if (n == 0)
	    {
	    	return;
	    }
	    
	    Buffer = new byte[n + 1];
	    lcms2.cmsGetPostScriptCRD(DbgThread(), hProfile, 0, dwFlags, Buffer, n);
	    Buffer[n] = 0;
	    
	    if (FileName != null)
	    {
	        Stream o = Stream.fopen(FILE_PREFIX + FileName, "w");
	        o.write(Buffer, 0, n, 1);
	        o.close();
	    }
	    
	    Buffer = null;
	    lcms2.cmsCloseProfile(hProfile);
	    fileOperation(FileName, remove);
	}
	
	private static final TestFn CheckPostScript = new TestFn()
	{
		public int run()
		{
			GenerateCSA(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "sRGB_CSA.ps");
		    GenerateCSA(FILE_PREFIX + "aRGBlcms2.icc", "aRGB_CSA.ps");
		    GenerateCSA(FILE_PREFIX + "sRGB_v4_ICC_preference.icc", "sRGBV4_CSA.ps");
		    GenerateCSA(FILE_PREFIX + "USWebCoatedSWOP.icc", "SWOP_CSA.ps");
		    GenerateCSA(null, "Lab_CSA.ps");
		    GenerateCSA(FILE_PREFIX + "graylcms2.icc", "gray_CSA.ps");
		    
		    GenerateCRD(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "sRGB_CRD.ps");
		    GenerateCRD(FILE_PREFIX + "aRGBlcms2.icc", "aRGB_CRD.ps");
		    GenerateCRD(null, "Lab_CRD.ps");
		    GenerateCRD(FILE_PREFIX + "USWebCoatedSWOP.icc", "SWOP_CRD.ps");
		    GenerateCRD(FILE_PREFIX + "sRGB_v4_ICC_preference.icc", "sRGBV4_CRD.ps");
		    GenerateCRD(FILE_PREFIX + "graylcms2.icc", "gray_CRD.ps");
		    
		    return 1;
		}
	};
	
	private static int CheckGray(cmsHTRANSFORM xform, Byte g, double L)
	{
	    cmsCIELab Lab = new cmsCIELab();
	    
	    lcms2.cmsDoTransform(xform, g, Lab, 1);
	    
	    if (!IsGoodVal("a axis on gray", 0, Lab.a, 0.001)) return 0;
	    if (!IsGoodVal("b axis on gray", 0, Lab.b, 0.001)) return 0;
	    
	    return IsGoodVal("Gray value", L, Lab.L, 0.01) ? 1 : 0;
	}
	
	private static final TestFn CheckInputGray = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hGray = Create_Gray22();
		    cmsHPROFILE hLab  = lcms2.cmsCreateLab4Profile(null);
		    cmsHTRANSFORM xform;
		    
		    if (hGray == null || hLab == null)
		    {
		    	return 0;
		    }
		    
		    xform = lcms2.cmsCreateTransform(hGray, lcms2.TYPE_GRAY_8, hLab, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0); 
		    lcms2.cmsCloseProfile(hGray); lcms2.cmsCloseProfile(hLab);
		    
		    if (CheckGray(xform, new Byte((byte)0), 0) == 0)
		    {
		    	return 0;
		    }
		    if (CheckGray(xform, new Byte((byte)125), 52.768) == 0)
		    {
		    	return 0;
		    }
		    if (CheckGray(xform, new Byte((byte)200), 81.069) == 0)
		    {
		    	return 0;
		    }
		    if (CheckGray(xform, new Byte((byte)255), 100.0) == 0)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    return 1;
		}
	};
	
	private static final TestFn CheckLabInputGray = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hGray = Create_GrayLab();
		    cmsHPROFILE hLab  = lcms2.cmsCreateLab4Profile(null);
		    cmsHTRANSFORM xform;
		    
		    if (hGray == null || hLab == null)
		    {
		    	return 0;
		    }
		    
		    xform = lcms2.cmsCreateTransform(hGray, lcms2.TYPE_GRAY_8, hLab, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hGray); lcms2.cmsCloseProfile(hLab);
		    
		    if (CheckGray(xform, new Byte((byte)0), 0) == 0)
		    {
		    	return 0;
		    }
		    if (CheckGray(xform, new Byte((byte)125), 49.019) == 0)
		    {
		    	return 0;
		    }
		    if (CheckGray(xform, new Byte((byte)200), 78.431) == 0)
		    {
		    	return 0;
		    }
		    if (CheckGray(xform, new Byte((byte)255), 100.0) == 0)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    return 1;
		}
	};
	
	private static int CheckOutGray(cmsHTRANSFORM xform, double L, byte g)
	{
	    cmsCIELab Lab = new cmsCIELab();
	    byte[] g_out = new byte[1];
	    
	    Lab.L = L;
	    Lab.a = 0;
	    Lab.b = 0;
	    
	    lcms2.cmsDoTransform(xform, Lab, g_out, 1);
	    
	    return IsGoodVal("Gray value", g, g_out[0], 0.01) ? 1 : 0;
	}
	
	private static final TestFn CheckOutputGray = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hGray = Create_Gray22();
		    cmsHPROFILE hLab  = lcms2.cmsCreateLab4Profile(null);
		    cmsHTRANSFORM xform;
		    
		    if (hGray == null || hLab == null)
		    {
		    	return 0;
		    }
		    
		    xform = lcms2.cmsCreateTransform( hLab, lcms2.TYPE_Lab_DBL, hGray, lcms2.TYPE_GRAY_8, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0); 
		    lcms2.cmsCloseProfile(hGray); lcms2.cmsCloseProfile(hLab);
		    
		    if (CheckOutGray(xform, 0, (byte)0) == 0)
		    {
		    	return 0;
		    }
		    if (CheckOutGray(xform, 100, (byte)255) == 0)
		    {
		    	return 0;
		    }
		    
		    if (CheckOutGray(xform, 20, (byte)52) == 0)
		    {
		    	return 0;
		    }
		    if (CheckOutGray(xform, 50, (byte)118) == 0)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    return 1;
		}
	};
	
	private static final TestFn CheckLabOutputGray = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE hGray = Create_GrayLab();
		    cmsHPROFILE hLab  = lcms2.cmsCreateLab4Profile(null);
		    cmsHTRANSFORM xform;
		    int i;
		    
		    if (hGray == null || hLab == null)
		    {
		    	return 0;
		    }
		    
		    xform = lcms2.cmsCreateTransform( hLab, lcms2.TYPE_Lab_DBL, hGray, lcms2.TYPE_GRAY_8, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
		    lcms2.cmsCloseProfile(hGray); lcms2.cmsCloseProfile(hLab);
		    
		    if (CheckOutGray(xform, 0, (byte)0) == 0)
	        {
	        	return 0;
	        }
		    if (CheckOutGray(xform, 100, (byte)255) == 0)
	        {
	        	return 0;
	        }
		    
		    for (i=0; i < 100; i++)
		    {
		        byte g;
		        
		        g = (byte)Math.floor(i * 255.0 / 100.0 + 0.5);
		        
		        if (CheckOutGray(xform, i, g) == 0)
		        {
		        	return 0;
		        }
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    return 1;
		}
	};
	
	private static final TestFn CheckV4gamma = new TestFn()
	{
		public int run()
		{
			cmsHPROFILE h;
		    short[] Lin = {0, (short)0xffff};
		    cmsToneCurve g = lcms2.cmsBuildTabulatedToneCurve16(DbgThread(), 2, Lin);
		    
		    h = lcms2.cmsOpenProfileFromFileTHR(DbgThread(), FILE_PREFIX + "v4gamma.icc", "w");
		    if (h == null)
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsSetProfileVersion(h, 4.2);
		    
		    if (!lcms2.cmsWriteTag(h, lcms2.cmsSigGrayTRCTag, g))
		    {
		    	return 0;
		    }
		    lcms2.cmsCloseProfile(h);
		    
		    lcms2.cmsFreeToneCurve(g);
		    fileOperation("v4gamma.icc", remove);
		    return 1;
		}
	};
	
	//boolean cmsGBDdumpVRML(cmsHANDLE hGBD, final String fname);
	
	// Gamut descriptor routines
	private static final TestFn CheckGBD = new TestFn()
	{
		public int run()
		{
			cmsCIELab Lab = new cmsCIELab();
		    cmsHANDLE  h;
		    int L, a, b;
		    int r1, g1, b1;
		    cmsHPROFILE hLab, hsRGB;
		    cmsHTRANSFORM xform;
		    
		    h = lcms2.cmsGBDAlloc(DbgThread());
		    if (h == null)
		    {
		    	return 0;
		    }
		    
		    // Fill all Lab gamut as valid
		    SubTest("Filling RAW gamut", null);
		    
		    for (L=0; L <= 100; L += 10)
		    {
		        for (a = -128; a <= 128; a += 5)
		        {
		            for (b = -128; b <= 128; b += 5)
		            {
		                Lab.L = L;
		                Lab.a = a;
		                Lab.b = b;
		                if (!lcms2.cmsGDBAddPoint(h, Lab))
		                {
		                	return 0;
		                }
		            }
		        }
		    }
		    
		    // Complete boundaries
		    SubTest("computing Lab gamut", null);
		    if (!lcms2.cmsGDBCompute(h, 0))
		    {
		    	return 0;
		    }
		    
		    // All points should be inside gamut
		    SubTest("checking Lab gamut", null);
		    for (L=10; L <= 90; L += 25)
		    {
		        for (a = -120; a <= 120; a += 25)
		        {
		            for (b = -120; b <= 120; b += 25)
		            {
		                Lab.L = L;
		                Lab.a = a;
		                Lab.b = b;
		                if (!lcms2.cmsGDBCheckPoint(h, Lab))
		                {
		                    return 0;
		                }
		            }
		        }
		    }
		    lcms2.cmsGBDFree(h);
		    
		    // Now for sRGB
		    SubTest("checking sRGB gamut", null);
		    h = lcms2.cmsGBDAlloc(DbgThread());
		    hsRGB = lcms2.cmsCreate_sRGBProfile();
		    hLab  = lcms2.cmsCreateLab4Profile(null);
		    
		    xform = lcms2.cmsCreateTransform(hsRGB, lcms2.TYPE_RGB_8, hLab, lcms2.TYPE_Lab_DBL, lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.cmsFLAGS_NOCACHE);
		    lcms2.cmsCloseProfile(hsRGB); lcms2.cmsCloseProfile(hLab);
		    
		    for (r1=0; r1 < 256; r1 += 5)
		    {
		        for (g1=0; g1 < 256; g1 += 5)
		        {
		            for (b1=0; b1 < 256; b1 += 5)
		            {
		                byte[] rgb = new byte[3];  
		                
		                rgb[0] = (byte) r1;
		                rgb[1] = (byte) g1; 
		                rgb[2] = (byte) b1;
		                
		                lcms2.cmsDoTransform(xform, rgb, Lab, 1);
		                
		                // if (fabs(Lab.b) < 20 && Lab.a > 0) continue;
		                
		                if (!lcms2.cmsGDBAddPoint(h, Lab))
		                {
		                	lcms2.cmsGBDFree(h);
		                    return 0;
		                }
		            }
		        }
		    }
		    
		    if (!lcms2.cmsGDBCompute(h, 0))
		    {
		    	return 0;
		    }
		    // cmsGBDdumpVRML(h, FILE_PREFIX + "lab.wrl");
		    
		    for (r1=10; r1 < 200; r1 += 10)
		    {
		        for (g1=10; g1 < 200; g1 += 10)
		        {
		            for (b1=10; b1 < 200; b1 += 10)
		            {
		                byte[] rgb = new byte[3];  

		                rgb[0] = (byte) r1;
		                rgb[1] = (byte) g1; 
		                rgb[2] = (byte) b1;

		                lcms2.cmsDoTransform(xform, rgb, Lab, 1);
		                if (!lcms2.cmsGDBCheckPoint(h, Lab))
		                {
		                	lcms2.cmsDeleteTransform(xform);
		                	lcms2.cmsGBDFree(h);
		                    return 0;
		                }
		            }
		        }
		    }
		    
		    lcms2.cmsDeleteTransform(xform);
		    lcms2.cmsGBDFree(h);
		    
		    SubTest("checking LCh chroma ring", null);
		    h = lcms2.cmsGBDAlloc(DbgThread());
		    
		    for (r1=0; r1 < 360; r1++)
		    {
		        cmsCIELCh LCh = new cmsCIELCh();
		        
		        LCh.L = 70;
		        LCh.C = 60;
		        LCh.h = r1;
		        
		        lcms2.cmsLCh2Lab(Lab, LCh);
		        if (!lcms2.cmsGDBAddPoint(h, Lab))
		        {
		        	lcms2.cmsGBDFree(h);
		        	return 0;
		        }
		    }
		    
		    if (!lcms2.cmsGDBCompute(h, 0))
		    {
		    	return 0;
		    }
		    
		    lcms2.cmsGBDFree(h);
		    
		    return 1;
		}
	};
	
	// --------------------------------------------------------------------------------------------------
	// P E R F O R M A N C E   C H E C K S
	// --------------------------------------------------------------------------------------------------
	
	private static class Scanline_rgb1 {byte r, g, b, a;static final int SIZE = 4;}
	private static class Scanline_rgb2 {short r, g, b, a;static final int SIZE = Scanline_rgb1.SIZE * 2;}
	private static class Scanline_rgb8 {byte r, g, b;static final int SIZE = 3;}
	private static class Scanline_rgb0 {short r, g, b;static final int SIZE = Scanline_rgb8.SIZE * 2;}
	
	private static void TitlePerformance(final String Txt)
	{
	    Utility.fprintf(print, "%-45s: ", new Object[]{Txt});
	    print.flush();
	}
	
	private static void PrintPerformance(int Bytes, int SizeOfPixel, double diff)
	{
		double seconds  = diff / 1000.0;
		double mpix_sec = Bytes / (1024.0*1024.0*seconds*SizeOfPixel);
		
	    Utility.fprintf(print, "%g MPixel/sec.\n", new Object[]{new Double(mpix_sec)});
	    print.flush();
	}
	
	private static void SpeedTest16bits(final String Title, cmsHPROFILE hlcmsProfileIn, cmsHPROFILE hlcmsProfileOut, int Intent)
	{
	    int r, g, b, j;
	    long atime;
	    double diff;
	    cmsHTRANSFORM hlcmsxform;
	    Scanline_rgb0[] In;
	    int Mb;
	    
	    if (hlcmsProfileIn == null || hlcmsProfileOut == null)
	    {
	    	Die("Unable to open profiles");
	    }
	    
	    hlcmsxform  = lcms2.cmsCreateTransformTHR(DbgThread(), hlcmsProfileIn, lcms2.TYPE_RGB_16, hlcmsProfileOut, lcms2.TYPE_RGB_16, Intent, lcms2.cmsFLAGS_NOCACHE);
	    lcms2.cmsCloseProfile(hlcmsProfileIn);
	    lcms2.cmsCloseProfile(hlcmsProfileOut);
	    
	    Mb = 256*256*256*Scanline_rgb0.SIZE;
	    In = new Scanline_rgb0[256*256*256];
	    
	    j = 0;
	    for (r=0; r < 256; r++)
	    {
	        for (g=0; g < 256; g++)
	        {
	            for (b=0; b < 256; b++)
	            {
	            	In[j].r = (short) ((r << 8) | r);
	            	In[j].g = (short) ((g << 8) | g);
	            	In[j].b = (short) ((b << 8) | b);
	            	
	            	j++;
	            }
	        }
	    }
	    
	    TitlePerformance(Title);
	    
	    atime = System.currentTimeMillis();
	    
	    lcms2.cmsDoTransform(hlcmsxform, In, In, 256*256*256);
	    
	    diff = System.currentTimeMillis() - atime;
	    
	    In = null;
	    
	    PrintPerformance(Mb, Scanline_rgb0.SIZE, diff);
	    lcms2.cmsDeleteTransform(hlcmsxform);
	}
	
	private static void SpeedTest16bitsCMYK(final String Title, cmsHPROFILE hlcmsProfileIn, cmsHPROFILE hlcmsProfileOut)
	{
	    int r, g, b, j;
	    long atime;
	    double diff;
	    cmsHTRANSFORM hlcmsxform;
	    Scanline_rgb2[] In;
	    int Mb;
	    
	    if (hlcmsProfileIn == null || hlcmsProfileOut == null)
	    {
	    	Die("Unable to open profiles");
	    }
	    
	    hlcmsxform  = lcms2.cmsCreateTransformTHR(DbgThread(), hlcmsProfileIn, lcms2.TYPE_CMYK_16, hlcmsProfileOut, lcms2.TYPE_CMYK_16, lcms2.INTENT_PERCEPTUAL, lcms2.cmsFLAGS_NOCACHE);
	    lcms2.cmsCloseProfile(hlcmsProfileIn);
	    lcms2.cmsCloseProfile(hlcmsProfileOut);
	    
	    Mb = 256*256*256*Scanline_rgb2.SIZE;
	    In = new Scanline_rgb2[256*256*256];
	    
	    j = 0;
	    for (r=0; r < 256; r++)
	    {
	        for (g=0; g < 256; g++)
	        {
	            for (b=0; b < 256; b++)
	            {
	            	In[j].r = (short) ((r << 8) | r);
	            	In[j].g = (short) ((g << 8) | g);
	            	In[j].b = (short) ((b << 8) | b);
	            	In[j].a = 0;
	            	
	            	j++;
	            }
	        }
	    }
	    
	    TitlePerformance(Title);
	    
	    atime = System.currentTimeMillis();
	    
	    lcms2.cmsDoTransform(hlcmsxform, In, In, 256*256*256);
	    
	    diff = System.currentTimeMillis() - atime;
	    
	    In = null;
	    
	    PrintPerformance(Mb, Scanline_rgb2.SIZE, diff);
	    
	    lcms2.cmsDeleteTransform(hlcmsxform);
	}
	
	private static void SpeedTest8bits(final String Title, cmsHPROFILE hlcmsProfileIn, cmsHPROFILE hlcmsProfileOut, int Intent)
	{
	    int r, g, b, j;
	    long atime;
	    double diff;
	    cmsHTRANSFORM hlcmsxform;
	    Scanline_rgb8[] In;
	    int Mb;
	    
	    if (hlcmsProfileIn == null || hlcmsProfileOut == null)
	    {
	    	Die("Unable to open profiles");
	    }
	    
	    hlcmsxform  = lcms2.cmsCreateTransformTHR(DbgThread(), hlcmsProfileIn, lcms2.TYPE_RGB_8, hlcmsProfileOut, lcms2.TYPE_RGB_8, Intent, lcms2.cmsFLAGS_NOCACHE);
	    lcms2.cmsCloseProfile(hlcmsProfileIn);
	    lcms2.cmsCloseProfile(hlcmsProfileOut);
	    
	    Mb = 256*256*256*Scanline_rgb8.SIZE;
	    In = new Scanline_rgb8[256*256*256];
	    
	    j = 0;
	    for (r=0; r < 256; r++)
	    {
	        for (g=0; g < 256; g++)
	        {
	            for (b=0; b < 256; b++)
	            {
	            	In[j].r = (byte) r;
	                In[j].g = (byte) g;
	                In[j].b = (byte) b;
	            	
	            	j++;
	            }
	        }
	    }
	    
	    TitlePerformance(Title);
	    
	    atime = System.currentTimeMillis();
	    
	    lcms2.cmsDoTransform(hlcmsxform, In, In, 256*256*256);
	    
	    diff = System.currentTimeMillis() - atime;
	    
	    In = null;
	    
	    PrintPerformance(Mb, Scanline_rgb8.SIZE, diff);
	    lcms2.cmsDeleteTransform(hlcmsxform);
	}
	
	private static void SpeedTest8bitsCMYK(final String Title, cmsHPROFILE hlcmsProfileIn, cmsHPROFILE hlcmsProfileOut)
	{
	    int r, g, b, j;
	    long atime;
	    double diff;
	    cmsHTRANSFORM hlcmsxform;
	    Scanline_rgb2[] In;
	    int Mb;
	    
	    if (hlcmsProfileIn == null || hlcmsProfileOut == null)
	    {
	    	Die("Unable to open profiles");
	    }
	    
	    hlcmsxform  = lcms2.cmsCreateTransformTHR(DbgThread(), hlcmsProfileIn, lcms2.TYPE_CMYK_8, hlcmsProfileOut, lcms2.TYPE_CMYK_8, lcms2.INTENT_PERCEPTUAL, lcms2.cmsFLAGS_NOCACHE);
	    lcms2.cmsCloseProfile(hlcmsProfileIn);
	    lcms2.cmsCloseProfile(hlcmsProfileOut);
	    
	    Mb = 256*256*256*Scanline_rgb2.SIZE;
	    In = new Scanline_rgb2[256*256*256];
	    
	    j = 0;
	    for (r=0; r < 256; r++)
	    {
	        for (g=0; g < 256; g++)
	        {
	            for (b=0; b < 256; b++)
	            {
	            	In[j].r = (byte) r;
	                In[j].g = (byte) g;
	                In[j].b = (byte) b;
	                In[j].a = (byte) 0;
	            	
	            	j++;
	            }
	        }
	    }
	    
	    TitlePerformance(Title);
	    
	    atime = System.currentTimeMillis();
	    
	    lcms2.cmsDoTransform(hlcmsxform, In, In, 256*256*256);
	    
	    diff = System.currentTimeMillis() - atime;
	    
	    In = null;
	    
	    PrintPerformance(Mb, Scanline_rgb2.SIZE, diff);
	    
	    lcms2.cmsDeleteTransform(hlcmsxform);
	}
	
	private static void SpeedTest8bitsGray(final String Title, cmsHPROFILE hlcmsProfileIn, cmsHPROFILE hlcmsProfileOut, int Intent)
	{
	    int r, g, b, j;
	    long atime;
	    double diff;
	    cmsHTRANSFORM hlcmsxform;
	    VirtualPointer In;
	    int Mb;
	    
	    if (hlcmsProfileIn == null || hlcmsProfileOut == null)
	    {
	    	Die("Unable to open profiles");
	    }
	    
	    hlcmsxform  = lcms2.cmsCreateTransformTHR(DbgThread(), hlcmsProfileIn, lcms2.TYPE_GRAY_8, hlcmsProfileOut, lcms2.TYPE_GRAY_8, Intent, lcms2.cmsFLAGS_NOCACHE);
	    lcms2.cmsCloseProfile(hlcmsProfileIn);
	    lcms2.cmsCloseProfile(hlcmsProfileOut);
	    Mb = 256*256*256;
	    
	    In = new VirtualPointer(Mb);
	    
	    j = 0;
	    for (r=0; r < 256; r++)
	    {
	        for (g=0; g < 256; g++)
	        {
	            for (b=0; b < 256; b++)
	            {
	            	In.writeRaw((byte) r, j);
	            	
	            	j++;
	            }
	        }
	    }
	    
	    TitlePerformance(Title);
	    
	    atime = System.currentTimeMillis();
	    
	    lcms2.cmsDoTransform(hlcmsxform, In, In, 256*256*256);
	    
	    diff = System.currentTimeMillis() - atime;
	    In.free();
	    
	    PrintPerformance(Mb, 8, diff);
	    lcms2.cmsDeleteTransform(hlcmsxform);
	}
	
	private static cmsHPROFILE CreateCurves()
	{
	    cmsToneCurve Gamma = lcms2.cmsBuildGamma(DbgThread(), 1.1);
	    cmsToneCurve[] Transfer = new cmsToneCurve[3];
	    cmsHPROFILE h;
	    
	    Transfer[0] = Transfer[1] = Transfer[2] = Gamma;
	    h = lcms2.cmsCreateLinearizationDeviceLink(lcms2.cmsSigRgbData, Transfer);
	    
	    lcms2.cmsFreeToneCurve(Gamma);
	    
	    return h;
	}
	
	public void SpeedTest()
	{
		Utility.fprintf(print, "\n\nP E R F O R M A N C E   T E S T S\n", null);
		Utility.fprintf(print,     "=================================\n\n", null);
		print.flush();
	    
	    SpeedTest16bits("16 bits on CLUT profiles", 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "r"),
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGBSpac.icm", "r"), lcms2.INTENT_PERCEPTUAL);
	    
	    SpeedTest8bits("8 bits on CLUT profiles", 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "r"),
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGBSpac.icm", "r"),
	    		lcms2.INTENT_PERCEPTUAL);
	    
	    SpeedTest8bits("8 bits on Matrix-Shaper profiles", 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "r"), 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "aRGBlcms2.icc", "r"),
	    		lcms2.INTENT_PERCEPTUAL);
	    
	    SpeedTest8bits("8 bits on SAME Matrix-Shaper profiles",
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "r"),
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "r"),
	    		lcms2.INTENT_PERCEPTUAL);
	    
	    SpeedTest8bits("8 bits on Matrix-Shaper profiles (AbsCol)", 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "r"),
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "aRGBlcms2.icc", "r"),
	    		lcms2.INTENT_ABSOLUTE_COLORIMETRIC);  
	    
	    SpeedTest16bits("16 bits on Matrix-Shaper profiles", 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "r"),
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "aRGBlcms2.icc", "r"),
	    		lcms2.INTENT_PERCEPTUAL);
	    
	    SpeedTest16bits("16 bits on SAME Matrix-Shaper profiles", 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "aRGBlcms2.icc", "r"),
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "aRGBlcms2.icc", "r"),
	    		lcms2.INTENT_PERCEPTUAL);
	    
	    SpeedTest16bits("16 bits on Matrix-Shaper profiles (AbsCol)", 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "sRGB_Color_Space_Profile.icm", "r"),
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "aRGBlcms2.icc", "r"),
	    		lcms2.INTENT_ABSOLUTE_COLORIMETRIC);
	    
	    SpeedTest8bits("8 bits on curves",
	    		CreateCurves(),
	    		CreateCurves(),
	    		lcms2.INTENT_PERCEPTUAL);
	    
	    SpeedTest16bits("16 bits on curves",
	    		CreateCurves(),
	    		CreateCurves(),
	    		lcms2.INTENT_PERCEPTUAL);
	    
	    SpeedTest8bitsCMYK("8 bits on CMYK profiles", 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "USWebCoatedSWOP.icc", "r"),
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "UncoatedFOGRA29.icc", "r"));
	    
	    SpeedTest16bitsCMYK("16 bits on CMYK profiles", 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "USWebCoatedSWOP.icc", "r"),
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "UncoatedFOGRA29.icc", "r"));
	    
	    SpeedTest8bitsGray("8 bits on gray-to-gray",
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "graylcms2.icc", "r"), 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "glablcms2.icc", "r"), lcms2.INTENT_RELATIVE_COLORIMETRIC);
	    
	    SpeedTest8bitsGray("8 bits on SAME gray-to-gray",
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "graylcms2.icc", "r"), 
	    		lcms2.cmsOpenProfileFromFile(FILE_PREFIX + "graylcms2.icc", "r"), lcms2.INTENT_PERCEPTUAL);
	}
	
	// -----------------------------------------------------------------------------------------------------
	
	// Print the supported intents
	private static void PrintSupportedIntents()
	{
	    int n, i;
	    int[] Codes = new int[200];
	    String[] Descriptions = new String[200];
	    
	    n = lcms2.cmsGetSupportedIntents(200, Codes, Descriptions);
	    
	    Utility.fprintf(print, "Supported intents:\n", null);
	    for (i=0; i < n; i++)
	    {
	    	Utility.fprintf(print, "\t%d - %s\n", new Object[]{new Integer(Codes[i]), Descriptions[i]});
	    }
	    Utility.fprintf(print, "\n", null);
	}
	
	// ZOO checks ------------------------------------------------------------------------------------------------------------
	
	private static final String ZOOfolder = "file:///SDCard/BlackBerry/documents/colormaps/";
	private static final String ZOOwrite = ZOOfolder + "write/";
	private static final String ZOORawWrite = ZOOfolder + "rawwrite/";
	
	// Read all tags on a profile given by its handle
	private static void ReadAllTags(cmsHPROFILE h)
	{
	    int i, n;
	    int sig;
	    
	    n = lcms2.cmsGetTagCount(h);
	    for (i=0; i < n; i++)
	    {
	    	sig = lcms2.cmsGetTagSignature(h, i);
	        if (lcms2.cmsReadTag(h, sig) == null)
	        {
	        	return;
	        }
	    }
	}
	
	// Read all tags on a profile given by its handle
	private static void ReadAllRAWTags(cmsHPROFILE h)
	{
	    int i, n;
	    int sig;
	    int len;
	    
	    n = lcms2.cmsGetTagCount(h);
	    for (i=0; i < n; i++)
	    {
	        sig = lcms2.cmsGetTagSignature(h, i);
	        len = lcms2.cmsReadRawTag(h, sig, null, 0);
	    }
	}
	
	private static void PrintInfo(cmsHPROFILE h, int Info)
	{
	    StringBuffer text;
	    int len;
	    cmsContext id = DbgThread();
	    
	    len = lcms2.cmsGetProfileInfo(h, Info, "en", "US", null, 0);
	    if (len == 0)
	    {
	    	return;
	    }
	    
	    text = new StringBuffer(len);
	    lcms2.cmsGetProfileInfo(h, Info, "en", "US", text, len);
	    
	    Utility.fprintf(print, "%s\n", new Object[]{text});
	}
	
	private static void PrintAllInfos(cmsHPROFILE h)
	{
	     PrintInfo(h, lcms2.cmsInfoDescription);
	     PrintInfo(h, lcms2.cmsInfoManufacturer);
	     PrintInfo(h, lcms2.cmsInfoModel);
	     PrintInfo(h, lcms2.cmsInfoCopyright);
	     Utility.fprintf(print, "\n\n", null);
	}
	
	private static void ReadAllLUTS(cmsHPROFILE h)
	{
	    cmsPipeline a;
	    cmsCIEXYZ Black = new cmsCIEXYZ();
	    
	    a = lcms2_internal._cmsReadInputLUT(h, lcms2.INTENT_PERCEPTUAL);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    a = lcms2_internal._cmsReadInputLUT(h, lcms2.INTENT_RELATIVE_COLORIMETRIC);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    a = lcms2_internal._cmsReadInputLUT(h, lcms2.INTENT_SATURATION);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    a = lcms2_internal._cmsReadInputLUT(h, lcms2.INTENT_ABSOLUTE_COLORIMETRIC);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    
	    a = lcms2_internal._cmsReadOutputLUT(h, lcms2.INTENT_PERCEPTUAL);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    a = lcms2_internal._cmsReadOutputLUT(h, lcms2.INTENT_RELATIVE_COLORIMETRIC);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    a = lcms2_internal._cmsReadOutputLUT(h, lcms2.INTENT_SATURATION);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    a = lcms2_internal._cmsReadOutputLUT(h, lcms2.INTENT_ABSOLUTE_COLORIMETRIC);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    
	    a = lcms2_internal._cmsReadDevicelinkLUT(h, lcms2.INTENT_PERCEPTUAL);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    a = lcms2_internal._cmsReadDevicelinkLUT(h, lcms2.INTENT_RELATIVE_COLORIMETRIC);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    a = lcms2_internal._cmsReadDevicelinkLUT(h, lcms2.INTENT_SATURATION);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    a = lcms2_internal._cmsReadDevicelinkLUT(h, lcms2.INTENT_ABSOLUTE_COLORIMETRIC);
	    if (a != null)
	    {
	    	lcms2.cmsPipelineFree(a);
	    }
	    
	    
	    lcms2.cmsDetectBlackPoint(Black, h, lcms2.INTENT_PERCEPTUAL, 0);
	    lcms2.cmsDetectBlackPoint(Black, h, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
	    lcms2.cmsDetectBlackPoint(Black, h, lcms2.INTENT_SATURATION, 0);
	    lcms2.cmsDetectBlackPoint(Black, h, lcms2.INTENT_ABSOLUTE_COLORIMETRIC, 0);
	    lcms2.cmsDetectTAC(h);
	}
	
	// Check one specimen in the ZOO
	
	private static int CheckSingleSpecimen(final String Profile)
	{
	    char[] BuffSrc = new char[256];
	    char[] BuffDst = new char[256];
	    cmsHPROFILE h;
	    
	    Utility.sprintf(BuffSrc, "%s%s", new Object[]{ZOOfolder, Profile});
	    Utility.sprintf(BuffDst, "%s%s", new Object[]{ZOOwrite,  Profile});
	    
	    h = lcms2.cmsOpenProfileFromFile(Utility.cstringCreation(BuffSrc), "r");
	    if (h == null)
	    {
	    	return 0;
	    }
	    
	    Utility.fprintf(print, "%s\n", new Object[]{Profile});
	    PrintAllInfos(h);
	    ReadAllTags(h);
	    // ReadAllRAWTags(h);
	    ReadAllLUTS(h);
	    
	    lcms2.cmsSaveProfileToFile(h, Utility.cstringCreation(BuffDst));
	    lcms2.cmsCloseProfile(h);
	    
	    h = lcms2.cmsOpenProfileFromFile(Utility.cstringCreation(BuffDst), "r");
	    if (h == null)
	    {
	    	return 0;
	    }
	    ReadAllTags(h);
	    
	    lcms2.cmsCloseProfile(h);
	    
	    return 1;
	}
	
	private static int CheckRAWSpecimen(final String Profile)
	{
	    char[] BuffSrc = new char[256];
	    char[] BuffDst = new char[256];
	    cmsHPROFILE h;
	    
	    Utility.sprintf(BuffSrc, "%s%s", new Object[]{ZOOfolder, Profile});
	    Utility.sprintf(BuffDst, "%s%s", new Object[]{ZOORawWrite,  Profile});
	    
	    h = lcms2.cmsOpenProfileFromFile(Utility.cstringCreation(BuffSrc), "r");
	    if (h == null)
	    {
	    	return 0;
	    }
	       
	    ReadAllTags(h);
	    ReadAllRAWTags(h);
	    lcms2.cmsSaveProfileToFile(h, Utility.cstringCreation(BuffDst));
	    lcms2.cmsCloseProfile(h);
	    
	    h = lcms2.cmsOpenProfileFromFile(Utility.cstringCreation(BuffDst), "r");
	    if (h == null)
	    {
	    	return 0;
	    }
	    ReadAllTags(h);
	    lcms2.cmsCloseProfile(h);
	    
	    return 1;
	}
	
	private static void CheckProfileZOO()
	{
		FileConnection c_file = null;
		Enumeration en = null;
		
		lcms2.cmsSetLogErrorHandler(null);
		
		try
		{
			c_file = (FileConnection)Connector.open(ZOOfolder, Connector.READ_WRITE);
			if(c_file.exists())
			{
				if(c_file.isDirectory())
				{
					en = c_file.list();
				}
			}
		}
		catch(IOException io)
		{
		}
		finally
		{
			if(c_file != null)
			{
				try
				{
					c_file.close();
					c_file = null;
				}
				catch(IOException io)
				{
				}
			}
		}
		
		if(en == null || !en.hasMoreElements())
		{
			Utility.fprintf(print, "No files in current directory", null);
		}
		else
		{
			try
			{
				do
				{
					if(c_file != null)
					{
						c_file.close();
					}
					c_file = (FileConnection)Connector.open(ZOOfolder + (String)en.nextElement(), Connector.READ_WRITE);
					
					Utility.fprintf(print, "%s\n", new Object[]{c_file.getName()});
					CheckSingleSpecimen( c_file.getName());
	                CheckRAWSpecimen( c_file.getName());
	                
	                if (TotalMemory > 0)
	                {
	                	Utility.fprintf(print, "Ok, but %s are left!\n", new Object[]{MemStr(TotalMemory)});
	                }
	                else
	                {
	                	Utility.fprintf(print, "Ok.\n", null);
	                }
				} while(en.hasMoreElements());
			}
			catch(IOException io)
			{
			}
			finally
			{
				if(c_file != null)
				{
					try
					{
						c_file.close();
					}
					catch(IOException io)
					{
					}
				}
			}
		}
		
		lcms2.cmsSetLogErrorHandler(FatalErrorQuit);
	}
	
	private static int CheckProfile(final String FileName)
	{
	    cmsHPROFILE h = lcms2.cmsOpenProfileFromFile(FileName, "r");
	    if (h == null)
	    {
	    	return 0;
	    }
	    
	    // Do some teste....
	    
	    lcms2.cmsCloseProfile(h);
	    
	    return 1;
	}
	
	// ---------------------------------------------------------------------------------------
	
	static
	{
		DebugMemHandler = new cmsPluginMemHandler();
		DebugMemHandler.Magic = lcms2_plugin.cmsPluginMagicNumber;
		DebugMemHandler.ExpectedVersion = 2000;
		DebugMemHandler.Type = lcms2_plugin.cmsPluginMemHandlerSig;
		DebugMemHandler.Next = null;
		DebugMemHandler.MallocPtr = DebugMalloc;
		DebugMemHandler.FreePtr = DebugFree;
		DebugMemHandler.ReallocPtr = DebugRealloc;
	}
	
	public final class TestScreen extends MainScreen implements FieldChangeListener
	{
		private boolean Exhaustive, DoSpeedTests;
		private ObjectListField list;
		private ListOut listOut;
		private PrintStream print;
		private boolean running;
		
		public TestScreen()
		{
			Exhaustive = false;
			DoSpeedTests = false;
			running = false;
			
			setTitle(new LabelField("Little CMS Test"));
			VerticalFieldManager vert = new VerticalFieldManager();
			add(vert);
			
			HorizontalFieldManager horz = new HorizontalFieldManager(HorizontalFieldManager.FIELD_HCENTER);
			VerticalFieldManager vert2 = new VerticalFieldManager();
			horz.add(vert2);
			ButtonField button = new ButtonField("Exhaustive Test [OFF]", ButtonField.CONSUME_CLICK);
			button.setChangeListener(this);
			vert2.add(button);
			button = new ButtonField("Do Speed Tests [OFF]", ButtonField.CONSUME_CLICK);
			button.setChangeListener(this);
			vert2.add(button);
			button = new ButtonField("Run", ButtonField.CONSUME_CLICK);
			button.setChangeListener(this);
			vert2.add(button);
			vert.add(horz);
			
			list = new ObjectListField(ObjectListField.ELLIPSIS | ObjectListField.FOCUSABLE)
			{
				protected boolean keyChar( char character, int status, int time ) 
			    {
			        if( character == Characters.ENTER ) {
			            clickButton();
			            return true;
			        }
			        return super.keyChar( character, status, time );
			    }
			    
			    protected boolean navigationClick( int status, int time ) 
			    {
			        clickButton();
			        return true;
			    }
			    
			    protected boolean trackwheelClick( int status, int time )
			    {        
			        clickButton();
			        return true;
			    }
			    
//#ifndef BlackBerrySDK4.1.0 | BlackBerrySDK4.0.0
			        protected boolean invokeAction( int action ) 
			        {
			            switch( action ) {
			                case ACTION_INVOKE: {
			                    clickButton(); 
			                    return true;
			                }
			            }
			            return super.invokeAction( action );
			        }
//#endif        
			         
			    /**
			     * A public way to click this button
			     */
			    public void clickButton() 
			    {
			    	//this.getChangeListener().fieldChanged(this, 0);
			        fieldChangeNotify( 0 );
			    }
			       
//#ifndef BlackBerrySDK4.6.1 | BlackBerrySDK4.6.0 | BlackBerrySDK4.5.0 | BlackBerrySDK4.2.1 | BlackBerrySDK4.2.0
			    protected boolean touchEvent( TouchEvent message )
			    {
			        int x = message.getX( 1 );
			        int y = message.getY( 1 );
			        if( x < 0 || y < 0 || x > getExtent().width || y > getExtent().height ) {
			            // Outside the field
			            return false;
			        }
			        switch( message.getEvent() ) {
			       
			            case TouchEvent.UNCLICK:
			                clickButton();
			                return true;
			        }
			        return super.touchEvent( message );
			    }
//#endif
			};
			list.setChangeListener(this);
			listOut = new ListOut(list);
			TestApp.print = print = new PrintStream(listOut);
			print.println("Little CMS 2 Testing App");
			vert.add(list);
		}
		
		public void runTest()
		{
			Utility.fprintf(print, "LittleCMS %2.2f test bed %s %s\n\n", new Object[]{new Double(lcms2.LCMS_VERSION / 1000.0), 
					new SimpleDateFormat("MMM dd yyyy").format(Calendar.getInstance(), new StringBuffer(), null), 
					new SimpleDateFormat("HH':'mm':'ss").format(Calendar.getInstance(), new StringBuffer(), null)});
			
			if(Exhaustive)
			{
				Utility.fprintf(print, "Running exhaustive tests (will take a while...)\n\n", null);
			}
			
			//TODO: Add some safety so that if the app exits prematurely it cleans up everything
			
			Utility.fprintf(print, "Installing debug memory plug-in ... ", null);
			lcms2.cmsPlugin(DebugMemHandler);
		    Utility.fprintf(print, "done.\n", null);
		    
		    Utility.fprintf(print, "Installing error logger ... ", null);
		    lcms2.cmsSetLogErrorHandler(FatalErrorQuit);
		    Utility.fprintf(print, "done.\n", null);
		    
//#ifdef CMS_IS_WINDOWS_
		     // CheckProfileZOO();
//#endif
		    
		    PrintSupportedIntents();
		    
		    CheckRGBPrimaries.run();
			
		    // Create utility profiles
		    Check("Creation of test profiles", CreateTestProfiles);
		    
		    //Check("Base types", CheckBaseTypes); //Base types don't change and are hard-coded
		    Check("endianess", CheckEndianess);
		    Check("quick floor", CheckQuickFloor);
		    Check("quick floor word", CheckQuickFloorWord);
		    Check("Fixed point 15.16 representation", CheckFixedPoint15_16);
		    Check("Fixed point 8.8 representation", CheckFixedPoint8_8);
		    
		    // Forward 1D interpolation
		    Check("1D interpolation in 2pt tables", Check1DLERP2);
		    Check("1D interpolation in 3pt tables", Check1DLERP3);
		    Check("1D interpolation in 4pt tables", Check1DLERP4);
		    Check("1D interpolation in 6pt tables", Check1DLERP6);
		    Check("1D interpolation in 18pt tables", Check1DLERP18);
		    Check("1D interpolation in descending 2pt tables", Check1DLERP2Down);
		    Check("1D interpolation in descending 3pt tables", Check1DLERP3Down);
		    Check("1D interpolation in descending 6pt tables", Check1DLERP6Down);
		    Check("1D interpolation in descending 18pt tables", Check1DLERP18Down);
		    
		    if (Exhaustive)
		    {
		        Check("1D interpolation in n tables", ExhaustiveCheck1DLERP);
		        Check("1D interpolation in descending tables", ExhaustiveCheck1DLERPDown);
		    }
		    
		    // Forward 3D interpolation
		    Check("3D interpolation Tetrahedral (float) ", Check3DinterpolationFloatTetrahedral);
		    Check("3D interpolation Trilinear (float) ", Check3DinterpolationFloatTrilinear);
		    Check("3D interpolation Tetrahedral (16) ", Check3DinterpolationTetrahedral16);
		    Check("3D interpolation Trilinear (16) ", Check3DinterpolationTrilinear16); //XXX
		    
		    if (Exhaustive)
		    {
		        Check("Exhaustive 3D interpolation Tetrahedral (float) ", ExaustiveCheck3DinterpolationFloatTetrahedral);
		        Check("Exhaustive 3D interpolation Trilinear  (float) ", ExaustiveCheck3DinterpolationFloatTrilinear);
		        Check("Exhaustive 3D interpolation Tetrahedral (16) ", ExhaustiveCheck3DinterpolationTetrahedral16);
		        Check("Exhaustive 3D interpolation Trilinear (16) ", ExhaustiveCheck3DinterpolationTrilinear16);
		    }
		    
		    Check("Reverse interpolation 3 -> 3", CheckReverseInterpolation3x3);
		    Check("Reverse interpolation 4 -> 3", CheckReverseInterpolation4x3);
		    
		    // High dimensionality interpolation
		    Check("3D interpolation", Check3Dinterp);
		    Check("3D interpolation with granularity", Check3DinterpGranular);
		    Check("4D interpolation", Check4Dinterp);
		    Check("4D interpolation with granularity", Check4DinterpGranular);
		    Check("5D interpolation with granularity", Check5DinterpGranular);
		    Check("6D interpolation with granularity", Check6DinterpGranular);
		    Check("7D interpolation with granularity", Check7DinterpGranular);
		    
		    // Encoding of colorspaces
		    Check("Lab to LCh and back (float only) ", CheckLab2LCh);
		    Check("Lab to XYZ and back (float only) ", CheckLab2XYZ);
		    Check("Lab to xyY and back (float only) ", CheckLab2xyY);
		    Check("Lab V2 encoding", CheckLabV2encoding);
		    Check("Lab V4 encoding", CheckLabV4encoding);
		    
		    // BlackBody
		    Check("Blackbody radiator", CheckTemp2CHRM);
		    
		    // Tone curves
		    Check("Linear gamma curves (16 bits)", CheckGammaCreation16);
		    Check("Linear gamma curves (float)", CheckGammaCreationFlt);
		    
		    Check("Curve 1.8 (float)", CheckGamma18);
		    Check("Curve 2.2 (float)", CheckGamma22);
		    Check("Curve 3.0 (float)", CheckGamma30);
		    
		    Check("Curve 1.8 (table)", CheckGamma18Table);
		    Check("Curve 2.2 (table)", CheckGamma22Table);
		    Check("Curve 3.0 (table)", CheckGamma30Table);
		    
		    Check("Curve 1.8 (word table)", CheckGamma18TableWord);
		    Check("Curve 2.2 (word table)", CheckGamma22TableWord);
		    Check("Curve 3.0 (word table)", CheckGamma30TableWord);
		    
		    Check("Parametric curves", CheckParametricToneCurves);
		    
		    Check("Join curves", CheckJointCurves);
		    Check("Join curves descending", CheckJointCurvesDescending);
		    Check("Join curves degenerated", CheckReverseDegenerated);
		    Check("Join curves sRGB (Float)", CheckJointFloatCurves_sRGB);
		    Check("Join curves sRGB (16 bits)", CheckJoint16Curves_sRGB);
		    Check("Join curves sigmoidal", CheckJointCurvesSShaped);
		    
		    // LUT basics
		    Check("LUT creation & dup", CheckLUTcreation);
		    Check("1 Stage LUT ", Check1StageLUT);
		    Check("2 Stage LUT ", Check2StageLUT);
		    Check("2 Stage LUT (16 bits)", Check2Stage16LUT);
		    Check("3 Stage LUT ", Check3StageLUT);
		    Check("3 Stage LUT (16 bits)", Check3Stage16LUT);
		    Check("4 Stage LUT ", Check4StageLUT);
		    Check("4 Stage LUT (16 bits)", Check4Stage16LUT);
		    Check("5 Stage LUT ", Check5StageLUT);
		    Check("5 Stage LUT (16 bits) ", Check5Stage16LUT);
		    Check("6 Stage LUT ", Check6StageLUT);
		    Check("6 Stage LUT (16 bits) ", Check6Stage16LUT);
		    
		    // LUT operation
		    Check("Lab to Lab LUT (float only) ", CheckLab2LabLUT);
		    Check("XYZ to XYZ LUT (float only) ", CheckXYZ2XYZLUT);
		    Check("Lab to Lab MAT LUT (float only) ", CheckLab2LabMatLUT);
		    Check("Named Color LUT", CheckNamedColorLUT);
		    Check("Usual formatters", CheckFormatters16);
		    Check("Floating point formatters", CheckFormattersFloat);
		    
		    // MLU
		    Check("Multilocalized Unicode", CheckMLU);
		    
		    // Named color
		    Check("Named color lists", CheckNamedColorList);
		    
		    // Profile I/O (this one is huge!)
		    Check("Profile creation", CheckProfileCreation);
		    
		    // Error reporting
		    Check("Error reporting on bad profiles", CheckErrReportingOnBadProfiles);
		    Check("Error reporting on bad transforms", CheckErrReportingOnBadTransforms);
		    
		    // Transforms
		    Check("Curves only transforms", CheckCurvesOnlyTransforms);
		    Check("Float Lab->Lab transforms", CheckFloatLabTransforms);
		    Check("Encoded Lab->Lab transforms", CheckEncodedLabTransforms);
		    Check("Stored identities", CheckStoredIdentities);
		    
		    Check("Matrix-shaper transform (float)",   CheckMatrixShaperXFORMFloat);
		    Check("Matrix-shaper transform (16 bits)", CheckMatrixShaperXFORM16);   
		    Check("Matrix-shaper transform (8 bits)",  CheckMatrixShaperXFORM8);
		    
		    Check("Primaries of sRGB", CheckRGBPrimaries);
		    
		    // Known values
		    Check("Known values across matrix-shaper", Chack_sRGB_Float);
		    Check("Gray input profile", CheckInputGray);
		    Check("Gray Lab input profile", CheckLabInputGray);
		    Check("Gray output profile", CheckOutputGray);
		    Check("Gray Lab output profile", CheckLabOutputGray);
		    
		    Check("Matrix-shaper proofing transform (float)",   CheckProofingXFORMFloat);
		    Check("Matrix-shaper proofing transform (16 bits)",  CheckProofingXFORM16);
		    
		    Check("Gamut check", CheckGamutCheck);
		    
		    Check("CMYK roundtrip on perceptual transform",   CheckCMYKRoundtrip);
		    
		    Check("CMYK perceptual transform",   CheckCMYKPerceptual);
		    // Check("CMYK rel.col. transform",   CheckCMYKRelCol);
		    
		    Check("Black ink only preservation", CheckKOnlyBlackPreserving);
		    Check("Black plane preservation", CheckKPlaneBlackPreserving);
		    
		    Check("Deciding curve types", CheckV4gamma);
		    
		    Check("Black point detection", CheckBlackPoint);
		    Check("TAC detection", CheckTAC);
		    
		    Check("CGATS parser", CheckCGATS);
		    Check("PostScript generator", CheckPostScript);
		    Check("Segment maxima GBD", CheckGBD);
		    
		    if (DoSpeedTests)
		    {
		    	SpeedTest();
		    }
		    
		    DebugMemPrintTotals();
		    
		    lcms2.cmsUnregisterPlugins();
		    
		    // Cleanup
		    RemoveTestProfiles();
		    
		    Utility.fprintf(print, "Total number of failures: %d\n", new Object[]{new Integer(TotalFail)});
		}
		
		public void fieldChanged(Field field, int context)
		{
			if(field instanceof ButtonField)
			{
				ButtonField button = (ButtonField)field;
				String label = button.getLabel();
				if(label.equals("Run"))
				{
					if(!running)
					{
						running = true;
						//list.setSize(1, 0);
						//list.setFocus();
						new Thread(new Runnable()
						{
							public void run()
							{
								runTest();
								running = false;
							}
						}).start();
					}
				}
				else if(label.startsWith("Do Speed Tests"))
				{
					if(!running)
					{
						if(label.indexOf("[ON]") >= 0)
						{
							button.setLabel("Do Speed Tests [OFF]");
							DoSpeedTests = false;
						}
						else
						{
							if(Dialog.ask(Dialog.D_YES_NO, 
									"Your phone might not have enough memory, it will not tell you if it crashed. Are you sure you want to continue? Needs at least 512mb of memory.") 
									== Dialog.YES)
							{
								button.setLabel("Do Speed Tests [ON]");
								DoSpeedTests = true;
							}
						}
					}
				}
				else
				{
					if(!running)
					{
						button.setLabel(label.indexOf("[ON]") >= 0 ? "Exhaustive Test [OFF]" : "Exhaustive Test [ON]");
						Exhaustive = !Exhaustive;
					}
				}
			}
			else if(field instanceof ObjectListField)
			{
				if(context == 0)
				{
					ObjectListField list = (ObjectListField)field;
					String value = list.get(list, list.getSelectedIndex()).toString();
					if(value.trim().length() != 0)
					{
						if(list.getFont().getAdvance(value) > list.getWidth())
						{
							Dialog.inform(value.trim()); //Trim the value first because tabs and extra spaces are not needed.
						}
					}
				}
			}
		}
		
		public boolean onClose()
		{
			running = false;
			print.close();
			super.close();
			return true;
		}
		
		protected void makeMenu(Menu menu, int instance)
		{
			MenuItem item = new MenuItem("To top", 0, 0)
			{
				public void run()
				{
					list.setSelectedIndex(0);
					list.setFocus();
				}
			};
			menu.add(item);
			super.makeMenu(menu, instance);
		}
		
		public boolean onMenu(int instance)
		{
			if(instance == 0x40000000) //0x40000000 is menu key
			{
				return super.onMenu(instance);
			}
			return false;
		}
	}
}
