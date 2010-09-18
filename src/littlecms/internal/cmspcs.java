//#preprocessor

//---------------------------------------------------------------------------------
//
//  Little Color Management System
//  Copyright (c) 1998-2010 Marti Maria Saguer
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

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.MathUtilities;
import littlecms.internal.helper.Utility;
import littlecms.internal.lcms2.cmsCIELCh;
import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsCIExyY;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmspcs
{
	//  inter PCS conversions XYZ <-> CIE L* a* b*
	/*


	       CIE 15:2004 CIELab is defined as:

	       L* = 116*f(Y/Yn) - 16                     0 <= L* <= 100
	       a* = 500*[f(X/Xn) - f(Y/Yn)]
	       b* = 200*[f(Y/Yn) - f(Z/Zn)]

	       and

	              f(t) = t^(1/3)                     1 >= t >  (24/116)^3
	                     (841/108)*t + (16/116)      0 <= t <= (24/116)^3


	       Reverse transform is:

	       X = Xn*[a* / 500 + (L* + 16) / 116] ^ 3   if (X/Xn) > (24/116)
	         = Xn*(a* / 500 + L* / 116) / 7.787      if (X/Xn) <= (24/116)



	       PCS in Lab2 is encoded as:

	              8 bit Lab PCS:

	                     L*      0..100 into a 0..ff byte.
	                     a*      t + 128 range is -128.0  +127.0
	                     b*

	             16 bit Lab PCS:

	                     L*     0..100  into a 0..ff00 word.
	                     a*     t + 128  range is  -128.0  +127.9961
	                     b*



	Interchange Space   Component     Actual Range        Encoded Range
	CIE XYZ             X             0 -> 1.99997        0x0000 -> 0xffff
	CIE XYZ             Y             0 -> 1.99997        0x0000 -> 0xffff
	CIE XYZ             Z             0 -> 1.99997        0x0000 -> 0xffff

	Version 2,3
	-----------

	CIELAB (16 bit)     L*            0 -> 100.0          0x0000 -> 0xff00
	CIELAB (16 bit)     a*            -128.0 -> +127.996  0x0000 -> 0x8000 -> 0xffff
	CIELAB (16 bit)     b*            -128.0 -> +127.996  0x0000 -> 0x8000 -> 0xffff


	Version 4
	---------

	CIELAB (16 bit)     L*            0 -> 100.0          0x0000 -> 0xffff
	CIELAB (16 bit)     a*            -128.0 -> +127      0x0000 -> 0x8080 -> 0xffff
	CIELAB (16 bit)     b*            -128.0 -> +127      0x0000 -> 0x8080 -> 0xffff

	*/
	
	// Conversions
	public static void cmsXYZ2xyY(cmsCIExyY Dest, final cmsCIEXYZ Source)
	{
	    double ISum;
	    
	    ISum = 1.0 /(Source.X + Source.Y + Source.Z);
	    
	    Dest.x = (Source.X) * ISum;
	    Dest.y = (Source.Y) * ISum;
	    Dest.Y = Source.Y;
	}
	
	public static void cmsxyY2XYZ(cmsCIEXYZ Dest, final cmsCIExyY Source)
	{
	    Dest.X = (Source.x / Source.y) * Source.Y;
	    Dest.Y = Source.Y;
	    Dest.Z = ((1 - Source.x - Source.y) / Source.y) * Source.Y;
	}
	
	private static double f(double t)
	{
	    final double Limit = (24.0/116.0) * (24.0/116.0) * (24.0/116.0);
	    
	    if (t <= Limit)
	    {
	    	return (841.0/108.0) * t + (16.0/116.0);
	    }
	    else
	    {
//#ifndef BlackBerrySDK4.5.0
	    	return MathUtilities.pow(t, 1.0/3.0);
//#else
	    	return Utility.pow(t, 1.0/3.0);
//#endif
	    }
	}
	
	private static double f_1(double t)
	{
	    final double Limit = (24.0/116.0);
	    
	    if (t <= Limit)
	    {
	        return (108.0/841.0) * (t - (16.0/116.0));
	    }
	    
	    return t * t * t;
	}
	
	// Standard XYZ to Lab. it can handle negative XZY numbers in some cases
	public static void cmsXYZ2Lab(cmsCIEXYZ WhitePoint, cmsCIELab Lab, final cmsCIEXYZ xyz)
	{
	    double fx, fy, fz;
	    
	    if (WhitePoint == null)
	    {
	    	WhitePoint = cmswtpnt.cmsD50_XYZ();
	    }
	    
	    fx = f(xyz.X / WhitePoint.X);
	    fy = f(xyz.Y / WhitePoint.Y);
	    fz = f(xyz.Z / WhitePoint.Z);
	    
	    Lab.L = 116.0*fy - 16.0;
	    Lab.a = 500.0*(fx - fy);
	    Lab.b = 200.0*(fy - fz);
	}
	
	// Standard XYZ to Lab. It can return negative XYZ in some cases
	public static void cmsLab2XYZ(cmsCIEXYZ WhitePoint, cmsCIEXYZ xyz, final cmsCIELab Lab)
	{
	    double x, y, z;

	    if (WhitePoint == null)
	    {
	    	WhitePoint = cmswtpnt.cmsD50_XYZ();
	    }
	    
	    y = (Lab.L + 16.0) / 116.0;
	    x = y + 0.002 * Lab.a;
	    z = y - 0.005 * Lab.b;
	    
	    xyz.X = f_1(x) * WhitePoint.X;
	    xyz.Y = f_1(y) * WhitePoint.Y;
	    xyz.Z = f_1(z) * WhitePoint.Z;
	}
	
	private static double L2float2(short v)
	{
		return v / 652.800;
	}
	
	// the a/b part
	private static double ab2float2(short v)
	{   
		return (v / 256.0) - 128.0;
	}
	
	private static short L2Fix2(double L)
	{
		return lcms2_internal._cmsQuickSaturateWord(L *  652.8);
	}
	
	private static short ab2Fix2(double ab)
	{
		return lcms2_internal._cmsQuickSaturateWord((ab + 128.0) * 256.0);
	}
	
	private static double L2float4(short v)
	{
	    return v / 655.35;
	}

	// the a/b part
	private static double ab2float4(short v)
	{
	    return (v / 257.0) - 128.0;
	}
	
	public static void cmsLabEncoded2FloatV2(cmsCIELab Lab, final short[] wLab)
	{
        Lab.L = L2float2(wLab[0]);
        Lab.a = ab2float2(wLab[1]);
        Lab.b = ab2float2(wLab[2]);
	}
	
	public static void cmsLabEncoded2Float(cmsCIELab Lab, final short[] wLab)
	{
		Lab.L = L2float4(wLab[0]);
		Lab.a = ab2float4(wLab[1]);
		Lab.b = ab2float4(wLab[2]);
	}
	
	private static double Clamp_L_doubleV2(double L)
	{
	    final double L_max = (0xFFFF * 100.0) / 0xFF00;

	    if (L < 0)
	    {
	    	L = 0;
	    }
	    if (L > L_max)
	    {
	    	L = L_max;
	    }
	    
	    return L;
	}
	
	private static double Clamp_ab_doubleV2(double ab)
	{
	    if (ab < lcms2_internal.MIN_ENCODEABLE_ab2)
	    {
	    	ab = lcms2_internal.MIN_ENCODEABLE_ab2;
	    }
	    if (ab > lcms2_internal.MAX_ENCODEABLE_ab2)
	    {
	    	ab = lcms2_internal.MAX_ENCODEABLE_ab2;
	    }
	    
	    return ab;
	}
	
	public static void cmsFloat2LabEncodedV2(short[] wLab, final cmsCIELab fLab)
	{
	    cmsCIELab Lab = new cmsCIELab();
	    
	    Lab.L = Clamp_L_doubleV2(fLab.L);
	    Lab.a = Clamp_ab_doubleV2(fLab.a);
	    Lab.b = Clamp_ab_doubleV2(fLab.b);
	    
	    wLab[0] = L2Fix2(Lab.L);
	    wLab[1] = ab2Fix2(Lab.a);
	    wLab[2] = ab2Fix2(Lab.b);
	}
	
	private static double Clamp_L_doubleV4(double L)
	{
	    if (L < 0)
	    {
	    	L = 0;
	    }
	    if (L > 100.0)
	    {
	    	L = 100.0;
	    }
	    
	    return L;
	}
	
	private static double Clamp_ab_doubleV4(double ab)
	{
	    if (ab < lcms2_internal.MIN_ENCODEABLE_ab4)
	    {
	    	ab = lcms2_internal.MIN_ENCODEABLE_ab4;
	    }
	    if (ab > lcms2_internal.MAX_ENCODEABLE_ab4)
	    {
	    	ab = lcms2_internal.MAX_ENCODEABLE_ab4;
	    }
	    
	    return ab;
	}
	
	private static short L2Fix4(double L)
	{
	    return lcms2_internal._cmsQuickSaturateWord(L *  655.35);
	}
	
	private static short ab2Fix4(double ab)
	{
	    return lcms2_internal._cmsQuickSaturateWord((ab + 128.0) * 257.0);
	}
	
	public static void cmsFloat2LabEncoded(short[] wLab, final cmsCIELab fLab)
	{
	    cmsCIELab Lab = new cmsCIELab();
	    
	    Lab.L = Clamp_L_doubleV4(fLab.L);
	    Lab.a = Clamp_ab_doubleV4(fLab.a);
	    Lab.b = Clamp_ab_doubleV4(fLab.b);
	    
	    wLab[0] = L2Fix4(Lab.L);
	    wLab[1] = ab2Fix4(Lab.a);
	    wLab[2] = ab2Fix4(Lab.b);
	}
	
	// Auxiliar: convert to Radians
	private static double RADIANS(double deg)
	{
	    //return (deg * lcms2_internal.M_PI) / 180.0;
		return Math.toRadians(deg); //Low-level hardware function. lcms2_internal.M_PI = Math.PI so inaccuracies don't need to be worried about.
	}
	
	// Auxiliar: atan2 but operating in degrees and returning 0 if a==b==0
	private static double atan2deg(double a, double b)
	{
		double h;
		
		if (a == 0 && b == 0)
		{
			h = 0;
		}
	    else
	    {
//#ifndef BlackBerrySDK4.5.0
	    	h = MathUtilities.atan2(a, b);
//#else
	    	h = lcms2_internal.atan2(a, b);
//#endif
	    }
		
	    h *= (180.0 / lcms2_internal.M_PI);
	    
	    while (h > 360.0)
	    {
	    	h -= 360.0;
	    }
	    
	    while ( h < 0)
	    {
	    	h += 360.0;
	    }
	    
	    return h;
	}
	
	// Auxiliar: Square
	private static double Sqr(double v)
	{
	    return v * v; 
	}
	// From cylindrical coordinates. No check is performed, then negative values are allowed
	public static void cmsLab2LCh(cmsCIELCh LCh, final cmsCIELab Lab)
	{
	    LCh.L = Lab.L;
//#ifndef BlackBerrySDK4.5.0
	    LCh.C = MathUtilities.pow(Sqr(Lab.a) + Sqr(Lab.b), 0.5);
//#else
	    LCh.C = Utility.pow(Sqr(Lab.a) + Sqr(Lab.b), 0.5);
//#endif
	    LCh.h = atan2deg(Lab.b, Lab.a);
	}
	
	// To cylindrical coordinates. No check is performed, then negative values are allowed
	public static void cmsLCh2Lab(cmsCIELab Lab, final cmsCIELCh LCh)
	{
	    double h = (LCh.h * lcms2_internal.M_PI) / 180.0;
	    
	    Lab.L = LCh.L;
	    Lab.a = LCh.C * Math.cos(h);
	    Lab.b = LCh.C * Math.sin(h);          
	}
	
	// In XYZ All 3 components are encoded using 1.15 fixed point
	private static short XYZ2Fix(double d)
	{     
	    return lcms2_internal._cmsQuickSaturateWord(d * 32768.0);
	}
	
	public static void cmsFloat2XYZEncoded(short[] XYZ, final cmsCIEXYZ fXYZ)
	{
	    cmsCIEXYZ xyz = new cmsCIEXYZ();
	    
	    xyz.X = fXYZ.X;
	    xyz.Y = fXYZ.Y;
	    xyz.Z = fXYZ.Z;
	    
	    // Clamp to encodeable values.     
	    if (xyz.Y <= 0)
	    {
	        xyz.X = 0;
	        xyz.Y = 0;
	        xyz.Z = 0;
	    }
	    
	    if (xyz.X > lcms2_internal.MAX_ENCODEABLE_XYZ)
	    {
	    	xyz.X = lcms2_internal.MAX_ENCODEABLE_XYZ;
	    }
	    
	    if (xyz.X < 0)
	    {
	    	xyz.X = 0;
	    }
	    
	    if (xyz.Y > lcms2_internal.MAX_ENCODEABLE_XYZ)
	    {
	    	xyz.Y = lcms2_internal.MAX_ENCODEABLE_XYZ;
	    }
	    
	    if (xyz.Y < 0)
	    {
	    	xyz.Y = 0;
	    }
	    
	    if (xyz.Z > lcms2_internal.MAX_ENCODEABLE_XYZ)
	    {
	    	xyz.Z = lcms2_internal.MAX_ENCODEABLE_XYZ;
	    }
	    
	    if (xyz.Z < 0)
	    {
	    	xyz.Z = 0;
	    }
	    
	    XYZ[0] = XYZ2Fix(xyz.X);
	    XYZ[1] = XYZ2Fix(xyz.Y);
	    XYZ[2] = XYZ2Fix(xyz.Z);        
	}
	
	//  To convert from Fixed 1.15 point to cmsFloat64Number
	private static double XYZ2float(short v)
	{
	    int fix32;
	    
	    // From 1.15 to 15.16
	    fix32 = v << 1;
	    
	    // From fixed 15.16 to cmsFloat64Number
	    return cmsplugin._cms15Fixed16toDouble(fix32);
	}
	
	public static void cmsXYZEncoded2Float(cmsCIEXYZ fXYZ, final short[] XYZ)
	{
	    fXYZ.X = XYZ2float(XYZ[0]);
	    fXYZ.Y = XYZ2float(XYZ[1]);
	    fXYZ.Z = XYZ2float(XYZ[2]);
	}
	
	// Returns dE on two Lab values
	public static double cmsDeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2)
	{
		double dL, da, db;
		
	    dL = Math.abs(Lab1.L - Lab2.L);
	    da = Math.abs(Lab1.a - Lab2.a);
	    db = Math.abs(Lab1.b - Lab2.b);
	    
//#ifndef BlackBerrySDK4.5.0
	    return MathUtilities.pow(Sqr(dL) + Sqr(da) + Sqr(db), 0.5);
//#else
	    return Utility.pow(Sqr(dL) + Sqr(da) + Sqr(db), 0.5);
//#endif
	}
	
	// Return the CIE94 Delta E 
	public static double cmsCIE94DeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2)
	{
	    cmsCIELCh LCh1 = new cmsCIELCh(), LCh2 = new cmsCIELCh();
	    double dE, dL, dC, dh, dhsq;
	    double c12, sc, sh;
	    
	    dL = Math.abs(Lab1.L - Lab2.L);
	    
	    cmsLab2LCh(LCh1, Lab1);
	    cmsLab2LCh(LCh2, Lab2);
	    
	    dC  = Math.abs(LCh1.C - LCh2.C);
	    dE  = cmsDeltaE(Lab1, Lab2);
	    
	    dhsq = Sqr(dE) - Sqr(dL) - Sqr(dC);
	    if (dhsq < 0)
	    {
	    	dh = 0;
	    }
	    else
	    {
//#ifndef BlackBerrySDK4.5.0
	    	dh = MathUtilities.pow(dhsq, 0.5);
//#else
	    	dh = Utility.pow(dhsq, 0.5);
//#endif
	    }
	    
	    c12 = Math.sqrt(LCh1.C * LCh2.C);
	    
	    sc = 1.0 + (0.048 * c12);
	    sh = 1.0 + (0.014 * c12);
	    
	    return Math.sqrt(Sqr(dL)  + Sqr(dC) / Sqr(sc) + Sqr(dh) / Sqr(sh));
	}
	
	// Auxiliary
	private static double ComputeLBFD(final cmsCIELab Lab)
	{
		double yt;
		
		if (Lab.L > 7.996969)
		{
			yt = (Sqr((Lab.L+16)/116)*((Lab.L+16)/116))*100;
		}
		else
		{
			yt = 100 * (Lab.L / 903.3);
		}
		
		return (54.6 * (lcms2_internal.M_LOG10E * (MathUtilities.log(yt + 1.5))) - 9.6);
	}
	
	// bfd - gets BFD(1:1) difference between Lab1, Lab2
	public static double cmsBFDdeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2)
	{
	    double lbfd1,lbfd2,AveC,Aveh,dE,deltaL,deltaC,deltah,dc,t,g,dh,rh,rc,rt,bfd;
	    cmsCIELCh LCh1 = new cmsCIELCh(), LCh2 = new cmsCIELCh();
	    
	    lbfd1 = ComputeLBFD(Lab1);
	    lbfd2 = ComputeLBFD(Lab2);
	    deltaL = lbfd2 - lbfd1;
	    
	    cmsLab2LCh(LCh1, Lab1);
	    cmsLab2LCh(LCh2, Lab2);
	    
	    deltaC = LCh2.C - LCh1.C;
	    AveC = (LCh1.C+LCh2.C)/2;
	    Aveh = (LCh1.h+LCh2.h)/2;
	    
	    dE = cmsDeltaE(Lab1, Lab2);
	    
	    if (Sqr(dE)>(Sqr(Lab2.L-Lab1.L)+Sqr(deltaC)))
	    {
	    	deltah = Math.sqrt(Sqr(dE)-Sqr(Lab2.L-Lab1.L)-Sqr(deltaC));
	    }
	    else
	    {
	    	deltah =0;
	    }
	    
	    dc   = 0.035 * AveC / (1 + 0.00365 * AveC)+0.521;
	    g    = Math.sqrt(Sqr(Sqr(AveC))/(Sqr(Sqr(AveC))+14000));
	    t    = 0.627+(0.055*Math.cos((Aveh-254)/(180/lcms2_internal.M_PI))-       
	           0.040*Math.cos((2*Aveh-136)/(180/lcms2_internal.M_PI))+
	           0.070*Math.cos((3*Aveh-31)/(180/lcms2_internal.M_PI))+
	           0.049*Math.cos((4*Aveh+114)/(180/lcms2_internal.M_PI))-
	           0.015*Math.cos((5*Aveh-103)/(180/lcms2_internal.M_PI)));
	    
	    dh    = dc*(g*t+1-g);
	    rh    = -0.260*Math.cos((Aveh-308)/(180/lcms2_internal.M_PI))-
	           0.379*Math.cos((2*Aveh-160)/(180/lcms2_internal.M_PI))-
	           0.636*Math.cos((3*Aveh+254)/(180/lcms2_internal.M_PI))+
	           0.226*Math.cos((4*Aveh+140)/(180/lcms2_internal.M_PI))-
	           0.194*Math.cos((5*Aveh+280)/(180/lcms2_internal.M_PI));
	    
	    rc = Math.sqrt((AveC*AveC*AveC*AveC*AveC*AveC)/((AveC*AveC*AveC*AveC*AveC*AveC)+70000000));
	    rt = rh*rc;
	    
	    bfd = Math.sqrt(Sqr(deltaL)+Sqr(deltaC/dc)+Sqr(deltah/dh)+(rt*(deltaC/dc)*(deltah/dh)));
	    
	    return bfd;
	}
	
	//  cmc - CMC(l:c) difference between Lab1, Lab2
	public static double cmsCMCdeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2, double l, double c)
	{
		double dE,dL,dC,dh,sl,sc,sh,t,f,cmc;
		cmsCIELCh LCh1 = new cmsCIELCh(), LCh2 = new cmsCIELCh();
		
		if (Lab1.L == 0 && Lab2.L == 0)
		{
			return 0;
		}
		
		cmsLab2LCh(LCh1, Lab1);
		cmsLab2LCh(LCh2, Lab2);
		
		dL = Lab2.L-Lab1.L;
		dC = LCh2.C-LCh1.C;
		
		dE = cmsDeltaE(Lab1, Lab2);
		
		if (Sqr(dE)>(Sqr(dL)+Sqr(dC)))
		{
			dh = Math.sqrt(Sqr(dE)-Sqr(dL)-Sqr(dC));
		}
		else
		{
			dh =0;
		}
		
		if ((LCh1.h > 164) && (LCh1.h < 345))
		{
			t = 0.56 + Math.abs(0.2 * Math.cos(((LCh1.h + 168)/(180/lcms2_internal.M_PI))));
		}
		else
		{
			t = 0.36 + Math.abs(0.4 * Math.cos(((LCh1.h + 35 )/(180/lcms2_internal.M_PI))));
		}
		
		sc  = 0.0638   * LCh1.C / (1 + 0.0131  * LCh1.C) + 0.638;
		sl  = 0.040975 * Lab1.L /(1 + 0.01765 * Lab1.L);
		
		if (Lab1.L<16)
		{
			sl = 0.511; 
		}
		
		f   = Math.sqrt((LCh1.C * LCh1.C * LCh1.C * LCh1.C)/((LCh1.C * LCh1.C * LCh1.C * LCh1.C)+1900));
		sh  = sc*(t*f+1-f);
		cmc = Math.sqrt(Sqr(dL/(l*sl))+Sqr(dC/(c*sc))+Sqr(dh/sh));
		
		return cmc;
	}
	
	// dE2000 The weightings KL, KC and KH can be modified to reflect the relative 
	// importance of lightness, chroma and hue in different industrial applications
	public static double cmsCIE2000DeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2, double Kl, double Kc, double Kh)
	{
	    double L1  = Lab1.L;
	    double a1  = Lab1.a;
	    double b1  = Lab1.b;
	    double C   = Math.sqrt( Sqr(a1) + Sqr(b1) );
	    
	    double Ls = Lab2.L;
	    double as = Lab2.a;
	    double bs = Lab2.b;
	    double Cs = Math.sqrt( Sqr(as) + Sqr(bs) );
	    
//#ifndef BlackBerrySDK4.5.0
    	double G = 0.5 * ( 1 - Math.sqrt(MathUtilities.pow((C + Cs) / 2 , 7.0) / (MathUtilities.pow((C + Cs) / 2, 7.0) + MathUtilities.pow(25.0, 7.0) ) ));
//#else
    	double G = 0.5 * ( 1 - Math.sqrt(Utility.pow((C + Cs) / 2 , 7.0) / (Utility.pow((C + Cs) / 2, 7.0) + Utility.pow(25.0, 7.0) ) ));
//#endif
	    
	    double a_p = (1 + G ) * a1;
	    double b_p = b1;
	    double C_p = Math.sqrt( Sqr(a_p) + Sqr(b_p));
	    double h_p = atan2deg(b_p, a_p);
	    
	    
	    double a_ps = (1 + G) * as;
	    double b_ps = bs;
	    double C_ps = Math.sqrt(Sqr(a_ps) + Sqr(b_ps));
	    double h_ps = atan2deg(b_ps, a_ps);
	    
	    double meanC_p =(C_p + C_ps) / 2;
	    
	    double hps_plus_hp  = h_ps + h_p;
	    double hps_minus_hp = h_ps - h_p;
	    
	    double meanh_p = Math.abs(hps_minus_hp) <= 180.000001 ? (hps_plus_hp)/2 : 
	                            (hps_plus_hp) < 360 ? (hps_plus_hp + 360)/2 : 
	                                                 (hps_plus_hp - 360)/2;
	                            
	    double delta_h = (hps_minus_hp) <= -180.000001 ?  (hps_minus_hp + 360) :
	                            (hps_minus_hp) > 180 ? (hps_minus_hp - 360) : 
	                                                    (hps_minus_hp);
	    double delta_L = (Ls - L1);
	    double delta_C = (C_ps - C_p );
	    
	    
	    double delta_H =2 * Math.sqrt(C_ps*C_p) * Math.sin(RADIANS(delta_h) / 2);
	    
	    double T = 1 - 0.17 * Math.cos(RADIANS(meanh_p-30)) 
	                 + 0.24 * Math.cos(RADIANS(2*meanh_p))  
	                 + 0.32 * Math.cos(RADIANS(3*meanh_p + 6)) 
	                 - 0.2  * Math.cos(RADIANS(4*meanh_p - 63));
	    
	    double Sl = 1 + (0.015 * Sqr((Ls + L1) /2- 50) )/ Math.sqrt(20 + Sqr( (Ls+L1)/2 - 50) );
	    
	    double Sc = 1 + 0.045 * (C_p + C_ps)/2;
	    double Sh = 1 + 0.015 * ((C_ps + C_p)/2) * T;
	    
	    double delta_ro = 30 * MathUtilities.exp( -Sqr(((meanh_p - 275 ) / 25)));
	    
//#ifndef BlackBerrySDK4.5.0
    	double Rc = 2 * Math.sqrt(( MathUtilities.pow(meanC_p, 7.0) )/( MathUtilities.pow(meanC_p, 7.0) + MathUtilities.pow(25.0, 7.0)));
//#else
    	double Rc = 2 * Math.sqrt(( Utility.pow(meanC_p, 7.0) )/( Utility.pow(meanC_p, 7.0) + Utility.pow(25.0, 7.0)));
//#endif
	    
	    double Rt = -Math.sin(2 * RADIANS(delta_ro)) * Rc;
	    
	    double deltaE00 = Math.sqrt( Sqr(delta_L /(Sl * Kl)) + 
	                            Sqr(delta_C/(Sc * Kc))  + 
	                            Sqr(delta_H/(Sh * Kh))  + 
	                            Rt*(delta_C/(Sc * Kc)) * (delta_H / (Sh * Kh)));
	    
	    return deltaE00;
	}
	
	// This function returns a number of gridpoints to be used as LUT table. It assumes same number
	// of gripdpoints in all dimensions. Flags may override the choice.
	public static int _cmsReasonableGridpointsByColorspace(int Colorspace, int dwFlags)
	{
	    int nChannels;
	    
	    // Already specified?
	    if ((dwFlags & 0x00FF0000) != 0)
	    {
	    	// Yes, grab'em
	    	return (dwFlags >> 16) & 0xFF;
	    }
	    
	    nChannels = cmsChannelsOf(Colorspace);
	    
	    // HighResPrecalc is maximum resolution
	    if ((dwFlags & lcms2.cmsFLAGS_HIGHRESPRECALC) != 0)
	    {
	        if (nChannels > 4)
	        {
	        	return 7; // 7 for Hifi
	        }
	        
	        if (nChannels == 4) // 23 for CMYK
	        {
	        	return 23;
	        }
	        
	        return 49; // 49 for RGB and others        
	    }
	    
	    // LowResPrecal is lower resolution
	    if ((dwFlags & lcms2.cmsFLAGS_LOWRESPRECALC) != 0)
	    {
	        if (nChannels > 4)
	        {
	        	return 6; // 6 for more than 4 channels
	        }
	        
	        if (nChannels == 1)
	        {
	        	return 33; // For monochrome
	        }
	        
	        return 17; // 17 for remaining
	    }
	    
	    // Default values
	    if (nChannels > 4)
	    {
	    	return 7; // 7 for Hifi
	    }
	    
	    if (nChannels == 4)
	    {
	    	return 17; // 17 for CMYK
	    }
	    
	    return 33; // 33 for RGB   
	}
	
	public static boolean _cmsEndPointsBySpace(int Space, short[] White, short[] Black, int[] nOutputs)
	{
		// Only most common spaces
		
		/* This is ignored because it would be more memory efficient to just regenerate the values for the specified items
		final short[] RGBblack  = new short[4]{ 0, 0, 0 };
		final short[] RGBwhite  = new short[4]{ (short)0xffff, (short)0xffff, (short)0xffff };
		final short[] CMYKblack = new short[]{ (short)0xffff, (short)0xffff, (short)0xffff, (short)0xffff };	// 400% of ink
		final short[] CMYKwhite = new short[]{ 0, 0, 0, 0 };
		final short[] LABblack  = new short[4]{ 0, 0x8080, 0x8080 };											// V4 Lab encoding
		final short[] LABwhite  = new short[4]{ (short)0xffff, 0x8080, 0x8080 };
		final short[] CMYblack  = new short[4]{ (short)0xffff, (short)0xffff, (short)0xffff };
		final short[] CMYwhite  = new short[4]{ 0, 0, 0 };
		final short[] Grayblack = new short[4]{ 0 };
		final short[] GrayWhite = new short[4]{ (short)0xffff };
		*/
		
		switch (Space)
		{
			case lcms2.cmsSigGrayData:
				if (White != null)
				{
					White[0] = (short)0xffff;
				}
				if (Black != null)
				{
					Black[0] = 0;
				}
				if (nOutputs != null)
				{
					nOutputs[0] = 1;
				}
				return true;
			case lcms2.cmsSigRgbData:
				if (White != null)
				{
					Arrays.fill(White, (short)0xffff, 0, 3);
				}
				if (Black != null)
				{
					Arrays.fill(Black, (short)0, 0, 3);
				}
				if (nOutputs != null)
				{
					nOutputs[0] = 3;
				}
				return true;
			case lcms2.cmsSigLabData:
				if (White != null)
				{
					White[0] = (short)0xffff;
					White[1] = (short)0x8080;
					White[2] = (short)0x8080;
				}
				if (Black != null)
				{
					Black[0] = 0;
					Black[1] = (short)0x8080;
					Black[2] = (short)0x8080;
				}
				if (nOutputs != null)
				{
					nOutputs[0] = 3;
				}
				return true;
			case lcms2.cmsSigCmykData:
				if (White != null)
				{
					Arrays.fill(White, (short)0, 0, 4);
				}
				if (Black != null)
				{
					Arrays.fill(Black, (short)0xffff, 0, 4);
				}
				if (nOutputs != null)
				{
					nOutputs[0] = 4;
				}
				return true;
			case lcms2.cmsSigCmyData:
				if (White != null)
				{
					Arrays.fill(White, (short)0, 0, 3);
				}
				if (Black != null)
				{
					Arrays.fill(Black, (short)0xffff, 0, 3);
				}
				if (nOutputs != null)
				{
					nOutputs[0] = 3;
				}
				return true;
			default:;
		}
		
		return false;
	}
	
	// Several utilities -------------------------------------------------------
	
	// Translate from our colorspace to ICC representation
	
	public static int _cmsICCcolorSpace(int OurNotation)
	{
		switch (OurNotation)
		{
			case 1:
			case lcms2.PT_GRAY:
				return lcms2.cmsSigGrayData;
				
			case 2:
			case lcms2.PT_RGB:
				return lcms2.cmsSigRgbData;
				
			case lcms2.PT_CMY:
				return lcms2.cmsSigCmyData;
			case lcms2.PT_CMYK:
				return lcms2.cmsSigCmykData;
			case lcms2.PT_YCbCr:
				return lcms2.cmsSigYCbCrData;
			case lcms2.PT_YUV:
				return lcms2.cmsSigLuvData;
			case lcms2.PT_XYZ:
				return lcms2.cmsSigXYZData;
				
			case lcms2.PT_LabV2:
			case lcms2.PT_Lab:
				return lcms2.cmsSigLabData;
				
			case lcms2.PT_YUVK:
				return lcms2.cmsSigLuvKData;
			case lcms2.PT_HSV:
				return lcms2.cmsSigHsvData;
			case lcms2.PT_HLS:
				return lcms2.cmsSigHlsData;
			case lcms2.PT_Yxy:
				return lcms2.cmsSigYxyData;
			
			case lcms2.PT_MCH1:
				return lcms2.cmsSigMCH1Data;
			case lcms2.PT_MCH2:
				return lcms2.cmsSigMCH2Data;
			case lcms2.PT_MCH3:
				return lcms2.cmsSigMCH3Data;
			case lcms2.PT_MCH4:
				return lcms2.cmsSigMCH4Data;
			case lcms2.PT_MCH5:
				return lcms2.cmsSigMCH5Data;
			case lcms2.PT_MCH6:
				return lcms2.cmsSigMCH6Data;
			case lcms2.PT_MCH7:
				return lcms2.cmsSigMCH7Data;
			case lcms2.PT_MCH8:
				return lcms2.cmsSigMCH8Data;
			
			case lcms2.PT_MCH9:
				return lcms2.cmsSigMCH9Data;
			case lcms2.PT_MCH10:
				return lcms2.cmsSigMCHAData;
			case lcms2.PT_MCH11:
				return lcms2.cmsSigMCHBData;
			case lcms2.PT_MCH12:
				return lcms2.cmsSigMCHCData;
			case lcms2.PT_MCH13:
				return lcms2.cmsSigMCHDData;
			case lcms2.PT_MCH14:
				return lcms2.cmsSigMCHEData;
			case lcms2.PT_MCH15:
				return lcms2.cmsSigMCHFData;
			
			default:
				return (-1);
		}
	}
	
	public static int _cmsLCMScolorSpace(int ProfileSpace)
	{
	    switch (ProfileSpace)
	    {
		    case lcms2.cmsSigGrayData:
		    	return lcms2.PT_GRAY;
		    case lcms2.cmsSigRgbData:
		    	return lcms2.PT_RGB;
		    case lcms2.cmsSigCmyData:
		    	return lcms2.PT_CMY;
		    case lcms2.cmsSigCmykData:
		    	return lcms2.PT_CMYK;
		    case lcms2.cmsSigYCbCrData:
		    	return lcms2.PT_YCbCr;
		    case lcms2.cmsSigLuvData:
		    	return lcms2.PT_YUV;
		    case lcms2.cmsSigXYZData:
		    	return lcms2.PT_XYZ;
		    case lcms2.cmsSigLabData:
		    	return lcms2.PT_Lab;
		    case lcms2.cmsSigLuvKData:
		    	return lcms2.PT_YUVK;
		    case lcms2.cmsSigHsvData:
		    	return lcms2.PT_HSV;
		    case lcms2.cmsSigHlsData:
		    	return lcms2.PT_HLS;
		    case lcms2.cmsSigYxyData:
		    	return lcms2.PT_Yxy;
		    
		    case lcms2.cmsSig1colorData:
		    case lcms2.cmsSigMCH1Data:
		    	return lcms2.PT_MCH1;
		    
		    case lcms2.cmsSig2colorData:
		    case lcms2.cmsSigMCH2Data:
		    	return lcms2.PT_MCH2;
		    
		    case lcms2.cmsSig3colorData:
		    case lcms2.cmsSigMCH3Data:
		    	return lcms2.PT_MCH3;
		    
		    case lcms2.cmsSig4colorData:
		    case lcms2.cmsSigMCH4Data:
		    	return lcms2.PT_MCH4;
		    
		    case lcms2.cmsSig5colorData:
		    case lcms2.cmsSigMCH5Data:
		    	return lcms2.PT_MCH5;
		    
		    case lcms2.cmsSig6colorData:
		    case lcms2.cmsSigMCH6Data:
		    	return lcms2.PT_MCH6;
		    
		    case lcms2.cmsSigMCH7Data:
		    case lcms2.cmsSig7colorData:
		    	return lcms2.PT_MCH7;
		    
		    case lcms2.cmsSigMCH8Data:
		    case lcms2.cmsSig8colorData:
		    	return lcms2.PT_MCH8;
		    
		    case lcms2.cmsSigMCH9Data:
		    case lcms2.cmsSig9colorData:
		    	return lcms2.PT_MCH9;
		    
		    case lcms2.cmsSigMCHAData:
		    case lcms2.cmsSig10colorData:
		    	return lcms2.PT_MCH10;
		    
		    case lcms2.cmsSigMCHBData:
		    case lcms2.cmsSig11colorData:
		    	return lcms2.PT_MCH11;
		    
		    case lcms2.cmsSigMCHCData:
		    case lcms2.cmsSig12colorData:
		    	return lcms2.PT_MCH12;
		    
		    case lcms2.cmsSigMCHDData:
		    case lcms2.cmsSig13colorData:
		    	return lcms2.PT_MCH13;
		    
		    case lcms2.cmsSigMCHEData:
		    case lcms2.cmsSig14colorData:
		    	return lcms2.PT_MCH14;
		    
		    case lcms2.cmsSigMCHFData:
		    case lcms2.cmsSig15colorData:
		    	return lcms2.PT_MCH15;
		    
		    default:
		    	return (-1);
	    }
	}
	
	public static int cmsChannelsOf(int ColorSpace)
	{
	    switch (ColorSpace)
	    {
		    case lcms2.cmsSigGrayData:
		    	return 1;
		    case lcms2.cmsSig2colorData:
		    	return 2;
		    case lcms2.cmsSigXYZData:
		    case lcms2.cmsSigLabData:
		    case lcms2.cmsSigLuvData:
		    case lcms2.cmsSigYCbCrData:
		    case lcms2.cmsSigYxyData:
		    case lcms2.cmsSigRgbData:   
		    case lcms2.cmsSigHsvData:
		    case lcms2.cmsSigHlsData:
		    case lcms2.cmsSigCmyData: 
		    case lcms2.cmsSig3colorData:
		    	return 3;
		    case lcms2.cmsSigLuvKData:
		    case lcms2.cmsSigCmykData:
		    case lcms2.cmsSig4colorData:
		    	return 4;
		    case lcms2.cmsSigMCH5Data:
		    case lcms2.cmsSig5colorData:
		    	return 5;
		    case lcms2.cmsSigMCH6Data:   
		    case lcms2.cmsSig6colorData:
		    	return 6;
		    case lcms2.cmsSigMCH7Data:
		    case lcms2.cmsSig7colorData:
		    	return  7;
		    case lcms2.cmsSigMCH8Data:
		    case lcms2.cmsSig8colorData:
		    	return  8;
		    case lcms2.cmsSigMCH9Data:
		    case lcms2.cmsSig9colorData:
		    	return  9;
		    case lcms2.cmsSigMCHAData:
		    case lcms2.cmsSig10colorData:
		    	return 10;
		    case lcms2.cmsSigMCHBData:
		    case lcms2.cmsSig11colorData:
		    	return 11;
		    case lcms2.cmsSigMCHCData:
		    case lcms2.cmsSig12colorData:
		    	return 12;
		    case lcms2.cmsSigMCHDData:
		    case lcms2.cmsSig13colorData:
		    	return 13;
		    case lcms2.cmsSigMCHEData:
		    case lcms2.cmsSig14colorData:
		    	return 14;
		    case lcms2.cmsSigMCHFData:
		    case lcms2.cmsSig15colorData:
		    	return 15;
		    default:
		    	return 3;
	    }
	}
}
