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

import littlecms.internal.helper.Utility;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsCIExyY;
import littlecms.internal.lcms2.cmsCIExyYTRIPLE;
import littlecms.internal.lcms2_plugin.cmsMAT3;
import littlecms.internal.lcms2_plugin.cmsVEC3;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmswtpnt
{
	private static final long D50_XYZ_UID = 0x989B7FD873DB3186L;
	private static final long D50_xyY_UID = 0xCEA6CBC509325696L;
	
	// D50 - Widely used
	public static cmsCIEXYZ cmsD50_XYZ()
	{
		Object obj;
		if((obj = Utility.singletonStorageGet(D50_XYZ_UID)) != null)
		{
			return (cmsCIEXYZ)obj;
		}
		cmsCIEXYZ temp = new cmsCIEXYZ(lcms2.cmsD50X, lcms2.cmsD50Y, lcms2.cmsD50Z);
		Utility.singletonStorageSet(D50_XYZ_UID, temp);
		return temp;
	}
	
	public static cmsCIExyY cmsD50_xyY()
	{
		Object obj;
		if((obj = Utility.singletonStorageGet(D50_xyY_UID)) != null)
		{
			return (cmsCIExyY)obj;
		}
		
		cmsCIExyY D50xyY = new cmsCIExyY();
		
		cmspcs.cmsXYZ2xyY(D50xyY, cmsD50_XYZ());
		Utility.singletonStorageSet(D50_xyY_UID, D50xyY);
		
		return D50xyY;
	}
	
	// Obtains WhitePoint from Temperature
	public static boolean cmsWhitePointFromTemp(cmsCIExyY WhitePoint, double TempK)
	{
		double x, y;
		double T, T2, T3;
		// double M1, M2;
		
		lcms2_internal._cmsAssert(WhitePoint != null, "WhitePoint != null");
		
		T = TempK;
		T2 = T*T;            // Square
		T3 = T2*T;           // Cube
		
		// For correlated color temperature (T) between 4000K and 7000K:
		
		if (T >= 4000. && T <= 7000.)
		{
			x = -4.6070*(1E9/T3) + 2.9678*(1E6/T2) + 0.09911*(1E3/T) + 0.244063;
		}
		else
		{
			// or for correlated color temperature (T) between 7000K and 25000K:
			
			if (T > 7000.0 && T <= 25000.0)
			{
				x = -2.0064*(1E9/T3) + 1.9018*(1E6/T2) + 0.24748*(1E3/T) + 0.237040;
			}
			else
			{
				cmserr.cmsSignalError(null, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSWTPNT_BAD_WHITEPOINT_TEMP), null);
				return false;
			}
		}
		
		// Obtain y(x)
		
		y = -3.000*(x*x) + 2.870*x - 0.275;
		
		// wave factors (not used, but here for futures extensions)
		
		// M1 = (-1.3515 - 1.7703*x + 5.9114 *y)/(0.0241 + 0.2562*x - 0.7341*y);
		// M2 = (0.0300 - 31.4424*x + 30.0717*y)/(0.0241 + 0.2562*x - 0.7341*y);
		
		WhitePoint.x = x;
		WhitePoint.y = y;
		WhitePoint.Y = 1.0;
		
		return true;
	}
	
	private static final class ISOTEMPERATURE
	{
		public double mirek;  // temp (in microreciprocal kelvin)
		public double ut;     // u coord of intersection w/ blackbody locus
		public double vt;     // v coord of intersection w/ blackbody locus
		public double tt;     // slope of ISOTEMPERATURE. line
		
		public ISOTEMPERATURE(double mirek, double ut, double vt, double tt)
		{
			this.mirek = mirek;
			this.ut = ut;
			this.vt = vt;
			this.tt = tt;
		}
	}
	
	private static final ISOTEMPERATURE[] isotempdata = {
	//  {Mirek, Ut,       Vt,      Tt      } 
	    new ISOTEMPERATURE(0,     0.18006,  0.26352,  -0.24341),
	    new ISOTEMPERATURE(10,    0.18066,  0.26589,  -0.25479),
	    new ISOTEMPERATURE(20,    0.18133,  0.26846,  -0.26876),
	    new ISOTEMPERATURE(30,    0.18208,  0.27119,  -0.28539),
	    new ISOTEMPERATURE(40,    0.18293,  0.27407,  -0.30470),
	    new ISOTEMPERATURE(50,    0.18388,  0.27709,  -0.32675),
	    new ISOTEMPERATURE(60,    0.18494,  0.28021,  -0.35156),
	    new ISOTEMPERATURE(70,    0.18611,  0.28342,  -0.37915),
	    new ISOTEMPERATURE(80,    0.18740,  0.28668,  -0.40955),
	    new ISOTEMPERATURE(90,    0.18880,  0.28997,  -0.44278),
	    new ISOTEMPERATURE(100,   0.19032,  0.29326,  -0.47888),
	    new ISOTEMPERATURE(125,   0.19462,  0.30141,  -0.58204),
	    new ISOTEMPERATURE(150,   0.19962,  0.30921,  -0.70471),
	    new ISOTEMPERATURE(175,   0.20525,  0.31647,  -0.84901),
	    new ISOTEMPERATURE(200,   0.21142,  0.32312,  -1.0182 ),
	    new ISOTEMPERATURE(225,   0.21807,  0.32909,  -1.2168 ),
	    new ISOTEMPERATURE(250,   0.22511,  0.33439,  -1.4512 ),
	    new ISOTEMPERATURE(275,   0.23247,  0.33904,  -1.7298 ),
	    new ISOTEMPERATURE(300,   0.24010,  0.34308,  -2.0637 ),
	    new ISOTEMPERATURE(325,   0.24702,  0.34655,  -2.4681 ),
	    new ISOTEMPERATURE(350,   0.25591,  0.34951,  -2.9641 ),
	    new ISOTEMPERATURE(375,   0.26400,  0.35200,  -3.5814 ),
	    new ISOTEMPERATURE(400,   0.27218,  0.35407,  -4.3633 ),
	    new ISOTEMPERATURE(425,   0.28039,  0.35577,  -5.3762 ),
	    new ISOTEMPERATURE(450,   0.28863,  0.35714,  -6.7262 ),
	    new ISOTEMPERATURE(475,   0.29685,  0.35823,  -8.5955 ),
	    new ISOTEMPERATURE(500,   0.30505,  0.35907,  -11.324 ),
	    new ISOTEMPERATURE(525,   0.31320,  0.35968,  -15.628 ),
	    new ISOTEMPERATURE(550,   0.32129,  0.36011,  -23.325 ),
	    new ISOTEMPERATURE(575,   0.32931,  0.36038,  -40.770 ),
	    new ISOTEMPERATURE(600,   0.33724,  0.36051,  -116.45 )
	};

	private static final int NISO = 31;
	
	// Robertson's method
	public static boolean cmsTempFromWhitePoint(double[] TempK, final cmsCIExyY WhitePoint)
	{
		int j;
		double us,vs;
		double uj,vj,tj,di,dj,mi,mj;
		double xs, ys;
		
		lcms2_internal._cmsAssert(WhitePoint != null, "WhitePoint != null");
		lcms2_internal._cmsAssert(TempK != null, "TempK != null");
	    
		di = mi = 0;
		xs = WhitePoint.x;
		ys = WhitePoint.y;
		
		// convert (x,y) to CIE 1960 (u,WhitePoint) 
		
		us = (2*xs) / (-xs + 6*ys + 1.5);
		vs = (3*ys) / (-xs + 6*ys + 1.5);
		
		for (j = 0; j < NISO; j++)
		{
			uj = isotempdata[j].ut;
			vj = isotempdata[j].vt;
			tj = isotempdata[j].tt;
			mj = isotempdata[j].mirek;
			
			dj = ((vs - vj) - tj * (us - uj)) / Math.sqrt(1.0 + tj * tj);
			
			if ((j != 0) && (di/dj < 0.0))
			{
				// Found a match
				TempK[0] = 1000000.0 / (mi + (di / (di - dj)) * (mj - mi));
				return true;
			}
			
			di = dj;
			mi = mj;
		}
		
		// Not found
		return false;
	}
	
	// Compute chromatic adaptation matrix using Chad as cone matrix 
	
	private static boolean ComputeChromaticAdaptation(cmsMAT3 Conversion, final cmsCIEXYZ SourceWhitePoint, final cmsCIEXYZ DestWhitePoint, final cmsMAT3 Chad)
	{
	    cmsMAT3 Chad_Inv = new cmsMAT3();
	    cmsVEC3 ConeSourceXYZ = new cmsVEC3(), ConeSourceRGB = new cmsVEC3();
	    cmsVEC3 ConeDestXYZ = new cmsVEC3(), ConeDestRGB = new cmsVEC3();
	    cmsMAT3 Cone = new cmsMAT3(), Tmp;
	    
	    Tmp = Chad;
	    if (!cmsmtrx._cmsMAT3inverse(Tmp, Chad_Inv))
	    {
	    	return false;
	    }
	    
	    cmsmtrx._cmsVEC3init(ConeSourceXYZ, SourceWhitePoint.X, SourceWhitePoint.Y, SourceWhitePoint.Z);
	    
	    cmsmtrx._cmsVEC3init(ConeDestXYZ, DestWhitePoint.X, DestWhitePoint.Y, DestWhitePoint.Z);
	    
	    cmsmtrx._cmsMAT3eval(ConeSourceRGB, Chad, ConeSourceXYZ);
	    cmsmtrx._cmsMAT3eval(ConeDestRGB, Chad, ConeDestXYZ);
	    
	    // Build matrix
	    cmsmtrx._cmsVEC3init(Cone.v[0], ConeDestRGB.n[0]/ConeSourceRGB.n[0],    0.0,  0.0);
	    cmsmtrx._cmsVEC3init(Cone.v[1], 0.0,   ConeDestRGB.n[1]/ConeSourceRGB.n[1],   0.0);
	    cmsmtrx._cmsVEC3init(Cone.v[2], 0.0,   0.0,   ConeDestRGB.n[2]/ConeSourceRGB.n[2]);
	    
	    // Normalize
	    cmsmtrx._cmsMAT3per(Tmp, Cone, Chad);
	    cmsmtrx._cmsMAT3per(Conversion, Chad_Inv, Tmp);
	    
		return true;
	}
	
	// Returns the final chrmatic adaptation from illuminant FromIll to Illuminant ToIll
	// The cone matrix can be specified in ConeMatrix. If NULL, Bradford is assumed
	public static boolean _cmsAdaptationMatrix(cmsMAT3 r, cmsMAT3 ConeMatrix, final cmsCIEXYZ FromIll, final cmsCIEXYZ ToIll)
	{
		cmsMAT3 LamRigg = new cmsMAT3(); // Bradford matrix
		cmsmtrx._cmsMAT3set(LamRigg, new double[]{
				 0.8951,  0.2664, -0.1614,
				-0.7502,  1.7135,  0.0367,
				 0.0389, -0.0685,  1.0296
		}, 0);
		
		if (ConeMatrix == null)
		{
			ConeMatrix = LamRigg;
		}
		
		return ComputeChromaticAdaptation(r, FromIll, ToIll, ConeMatrix);	
	}
	
	// Same as anterior, but assuming D50 destination. White point is given in xyY
	private static boolean _cmsAdaptMatrixToD50(cmsMAT3 r, final cmsCIExyY SourceWhitePt)
	{
		cmsCIEXYZ Dn = new cmsCIEXYZ();
		cmsMAT3 Bradford = new cmsMAT3();
		cmsMAT3 Tmp;
		
		cmspcs.cmsxyY2XYZ(Dn, SourceWhitePt);
		
		if (!_cmsAdaptationMatrix(Bradford, null, Dn, cmsD50_XYZ()))
		{
			return false;
		}
		
		//Tmp = r; //This causes incorrect values because it's getting and setting from the same memory
		Tmp = new cmsMAT3();
		double[] tempVals = new double[9];
		cmsmtrx._cmsMAT3get(r, tempVals, 0);
		cmsmtrx._cmsMAT3set(Tmp, tempVals, 0);
		cmsmtrx._cmsMAT3per(r, Bradford, Tmp);
		
		return true;
	}
	
	// Build a White point, primary chromas transfer matrix from RGB to CIE XYZ
	// This is just an approximation, I am not handling all the non-linear
	// aspects of the RGB to XYZ process, and assuming that the gamma correction
	// has transitive property in the transformation chain.
	//
	// the alghoritm:
	//
	//	            - First I build the absolute conversion matrix using
	//	              primaries in XYZ. This matrix is next inverted
	//	            - Then I eval the source white point across this matrix
	//	              obtaining the coefficients of the transformation
	//	            - Then, I apply these coefficients to the original matrix
	//
	public static boolean _cmsBuildRGB2XYZtransferMatrix(cmsMAT3 r, final cmsCIExyY WhitePt, final cmsCIExyYTRIPLE Primrs)
	{
		cmsVEC3 WhitePoint = new cmsVEC3(), Coef = new cmsVEC3();
		cmsMAT3 Result = new cmsMAT3(), Primaries = new cmsMAT3();
		double xn, yn;
		double xr, yr;
		double xg, yg;
		double xb, yb;
		
		xn = WhitePt.x;
		yn = WhitePt.y;
		xr = Primrs.Red.x;
		yr = Primrs.Red.y;
		xg = Primrs.Green.x;
		yg = Primrs.Green.y;
		xb = Primrs.Blue.x;
		yb = Primrs.Blue.y;
		
		// Build Primaries matrix
		cmsmtrx._cmsVEC3init(Primaries.v[0], xr,        xg,         xb);
		cmsmtrx._cmsVEC3init(Primaries.v[1], yr,        yg,         yb);
		cmsmtrx._cmsVEC3init(Primaries.v[2], (1-xr-yr), (1-xg-yg),  (1-xb-yb));
		
		// Result = Primaries ^ (-1) inverse matrix
		if (!cmsmtrx._cmsMAT3inverse(Primaries, Result))
		{
			return false;
		}
		
		cmsmtrx._cmsVEC3init(WhitePoint, xn/yn, 1.0, (1.0-xn-yn)/yn);
		
		// Across inverse primaries ...
		cmsmtrx._cmsMAT3eval(Coef, Result, WhitePoint);
		
		// Give us the Coefs, then I build transformation matrix
		cmsmtrx._cmsVEC3init(r.v[0], Coef.n[lcms2_plugin.VX]*xr,          Coef.n[lcms2_plugin.VY]*xg,          Coef.n[lcms2_plugin.VZ]*xb);
		cmsmtrx._cmsVEC3init(r.v[1], Coef.n[lcms2_plugin.VX]*yr,          Coef.n[lcms2_plugin.VY]*yg,          Coef.n[lcms2_plugin.VZ]*yb);
		cmsmtrx._cmsVEC3init(r.v[2], Coef.n[lcms2_plugin.VX]*(1.0-xr-yr), Coef.n[lcms2_plugin.VY]*(1.0-xg-yg), Coef.n[lcms2_plugin.VZ]*(1.0-xb-yb));
		
		return _cmsAdaptMatrixToD50(r, WhitePt);
	}
	
	// Adapts a color to a given illuminant. Original color is expected to have
	// a SourceWhitePt white point. 
	public static boolean cmsAdaptToIlluminant(cmsCIEXYZ Result, final cmsCIEXYZ SourceWhitePt, final cmsCIEXYZ Illuminant, final cmsCIEXYZ Value)
	{
		cmsMAT3 Bradford = new cmsMAT3();
		cmsVEC3 In = new cmsVEC3(), Out = new cmsVEC3();
		
		lcms2_internal._cmsAssert(Result != null, "Result != null");
		lcms2_internal._cmsAssert(SourceWhitePt != null, "SourceWhitePt != null");
		lcms2_internal._cmsAssert(Illuminant != null, "Illuminant != null");
		lcms2_internal._cmsAssert(Value != null, "Value != null");
		
		if (!_cmsAdaptationMatrix(Bradford, null, SourceWhitePt, Illuminant))
		{
			return false;
		}
		
		cmsmtrx._cmsVEC3init(In, Value.X, Value.Y, Value.Z);
		cmsmtrx._cmsMAT3eval(Out, Bradford, In);
		
		Result.X = Out.n[0];
		Result.Y = Out.n[1];
		Result.Z = Out.n[2];
		
		return true;
	}
}
