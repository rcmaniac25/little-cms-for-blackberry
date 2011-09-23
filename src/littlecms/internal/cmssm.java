//#preprocessor

//---------------------------------------------------------------------------------
//
//  Little Color Management System
//  Copyright (c) 1998-2011 Marti Maria Saguer
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

//#ifndef BlackBerrySDK4.5.0
import net.rim.device.api.util.MathUtilities;
//#endif
import littlecms.internal.helper.Stream;
import littlecms.internal.helper.Utility;
import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHANDLE;
import littlecms.internal.lcms2_plugin.cmsVEC3;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmssm
{
	// Gamut boundary description by using Jan Morovic's Segment maxima method
	// Many thanks to Jan for allowing me to use his algorithm.
	
	// r = C*
	// alpha = Hab
	// theta = L*
	
	public static final int SECTORS = 16; // number of divisions in alpha and theta
	
	// Spherical coordinates
	private static class cmsSpherical
	{
		public double r;
	    public double alpha;
	    public double theta;
	}
	
	private static final int GP_EMPTY = 0;
	private static final int GP_SPECIFIED = GP_EMPTY + 1;
	private static final int GP_MODELED = GP_SPECIFIED + 1;
	
	private static class cmsGDBPoint
	{
		public int Type;
		public cmsSpherical p; // Keep also alpha & theta of maximum
		
		public cmsGDBPoint()
		{
			p = new cmsSpherical();
		}
	}
	
	private static class cmsGDB implements cmsHANDLE
	{
		public cmsContext ContextID;
		public cmsGDBPoint[][] Gamut;
		
		public cmsGDB()
		{
			Gamut = new cmsGDBPoint[SECTORS][SECTORS];
			for(int x = 0; x < SECTORS; x++)
			{
				for(int y = 0; y < SECTORS; y++)
				{
					Gamut[x][y] = new cmsGDBPoint();
				}
			}
		}
	}
	
	// A line using the parametric form
	// P = a + t*u
	private static class cmsLine
	{
		public cmsVEC3 a;
		public cmsVEC3 u;
		
		public cmsLine()
		{
			a = new cmsVEC3();
			u = new cmsVEC3();
		}
	}
	
	// A plane using the parametric form
	// Q = b + r*v + s*w
	private static class cmsPlane
	{
		public cmsVEC3 b;
		public cmsVEC3 v;
		public cmsVEC3 w;
		
		public cmsPlane()
		{
			b = new cmsVEC3();
			v = new cmsVEC3();
			w = new cmsVEC3();
		}
	}
	
	// --------------------------------------------------------------------------------------------
	
	// ATAN2() which always returns degree positive numbers
	
	private static double _cmsAtan2(double y, double x)
	{
		double a;
	    
	    // Deal with undefined case
	    if (x == 0.0 && y == 0.0)
	    {
	    	return 0;
	    }
	    
//#ifndef BlackBerrySDK4.5.0
	    a = (MathUtilities.atan2(y, x) * 180.0) / lcms2_internal.M_PI;
//#else
	    a = (Utility.atan2(y, x) * 180.0) / lcms2_internal.M_PI;
//#endif
	    
	    while (a < 0)
	    {
	        a += 360;
	    }
	    
	    return a;
	}
	
	// Convert to spherical coordinates
	private static void ToSpherical(cmsSpherical sp, final cmsVEC3 v)
	{
	    double L, a, b;
	    
	    L = v.n[lcms2_plugin.VX];
	    a = v.n[lcms2_plugin.VY];
	    b = v.n[lcms2_plugin.VZ];
	    
	    sp.r = Math.sqrt( L*L + a*a + b*b );
	    
	   if (sp.r == 0)
	   {
		   sp.alpha = sp.theta = 0;
		   return;
	   }
	   
	   sp.alpha = _cmsAtan2(a, b);
	   sp.theta = _cmsAtan2(Math.sqrt(a*a + b*b), L);  
	}
	
	// Convert to cartesian from spherical
	private static void ToCartesian(cmsVEC3 v, final cmsSpherical sp)
	{
	    double sin_alpha;
	    double cos_alpha;
	    double sin_theta;
	    double cos_theta;
	    double L, a, b;
	    
	    sin_alpha = Math.sin((lcms2_internal.M_PI * sp.alpha) * (1.0 / 180.0));
	    cos_alpha = Math.cos((lcms2_internal.M_PI * sp.alpha) * (1.0 / 180.0));
	    sin_theta = Math.sin((lcms2_internal.M_PI * sp.theta) * (1.0 / 180.0));
	    cos_theta = Math.cos((lcms2_internal.M_PI * sp.theta) * (1.0 / 180.0));
	    
	    a = sp.r * sin_theta * sin_alpha;
	    b = sp.r * sin_theta * cos_alpha; 
	    L = sp.r * cos_theta;
	    
	    v.n[lcms2_plugin.VX] = L;
	    v.n[lcms2_plugin.VY] = a;
	    v.n[lcms2_plugin.VZ] = b;
	}
	
	// Quantize sector of a spherical coordinate. Saturate 360, 180 to last sector
	// The limits are the centers of each sector, so
	private static void QuantizeToSector(final cmsSpherical sp, int[] alpha, int[] theta)
	{   
	    alpha[0] = (int)Math.floor(((sp.alpha * (SECTORS)) * (1.0 / 360.0)));
	    theta[0] = (int)Math.floor(((sp.theta * (SECTORS)) / (1.0 / 180.0)));
	    
		if (alpha[0] >= SECTORS)
		{
			alpha[0] = SECTORS-1;
		}
		if (theta[0] >= SECTORS)
		{
			theta[0] = SECTORS-1;
		}
	}
	
	// Line determined by 2 points
	private static void LineOf2Points(cmsLine line, cmsVEC3 a, cmsVEC3 b)
	{
	    cmsmtrx._cmsVEC3init(line.a, a.n[lcms2_plugin.VX], a.n[lcms2_plugin.VY], a.n[lcms2_plugin.VZ]);
	    cmsmtrx._cmsVEC3init(line.u, b.n[lcms2_plugin.VX] - a.n[lcms2_plugin.VX], 
	                            b.n[lcms2_plugin.VY] - a.n[lcms2_plugin.VY], 
	                            b.n[lcms2_plugin.VZ] - a.n[lcms2_plugin.VZ]);     
	}
	
	// Evaluate parametric line 
	private static void GetPointOfLine(cmsVEC3 p, final cmsLine line, double t)
	{
	    p.n[lcms2_plugin.VX] = line.a.n[lcms2_plugin.VX] + t * line.u.n[lcms2_plugin.VX];
	    p.n[lcms2_plugin.VY] = line.a.n[lcms2_plugin.VY] + t * line.u.n[lcms2_plugin.VY];
	    p.n[lcms2_plugin.VZ] = line.a.n[lcms2_plugin.VZ] + t * line.u.n[lcms2_plugin.VZ];     
	}
	
	/*
    Closest point in sector line1 to sector line2 (both are defined as 0 <=t <= 1)
    http://softsurfer.com/Archive/algorithm_0106/algorithm_0106.htm
    
    Copyright 2001, softSurfer (www.softsurfer.com)
    This code may be freely used and modified for any purpose
    providing that this copyright notice is included with it.
    SoftSurfer makes no warranty for this code, and cannot be held
    liable for any real or imagined damage resulting from its use.
    Users of this code must verify correctness for their application.
    
    */
	
	private static boolean ClosestLineToLine(cmsVEC3 r, final cmsLine line1, final cmsLine line2)
	{
	    double a, b, c, d, e, D;
	    double sc, sN, sD; 
	    double tc, tN, tD;
	    cmsVEC3 w0 = new cmsVEC3();
	    
	    cmsmtrx._cmsVEC3minus(w0, line1.a, line2.a);
	    
	    a  = cmsmtrx._cmsVEC3dot(line1.u, line1.u);
	    b  = cmsmtrx._cmsVEC3dot(line1.u, line2.u);
	    c  = cmsmtrx._cmsVEC3dot(line2.u, line2.u);
	    d  = cmsmtrx._cmsVEC3dot(line1.u, w0);
	    e  = cmsmtrx._cmsVEC3dot(line2.u, w0);
	    
	    D  = a*c - b * b;      // Denominator
	    sD = tD = D;           // default sD = D >= 0
	    
	    if (D < lcms2_internal.MATRIX_DET_TOLERANCE) // the lines are almost parallel
	    {
	        sN = 0.0;        // force using point P0 on segment S1
	        sD = 1.0;        // to prevent possible division by 0.0 later
	        tN = e;
	        tD = c;
	    }
	    else // get the closest points on the infinite lines
	    {
	        sN = (b*e - c*d);
	        tN = (a*e - b*d);
	        
	        if (sN < 0.0) // sc < 0 => the s=0 edge is visible
	        {
	            sN = 0.0;
	            tN = e;
	            tD = c;
	        }
	        else if (sN > sD) // sc > 1 => the s=1 edge is visible
	        {
	            sN = sD;
	            tN = e + b;
	            tD = c;
	        }
	    }
	    
	    if (tN < 0.0) // tc < 0 => the t=0 edge is visible
	    {
	        tN = 0.0;
	        // recompute sc for this edge
	        if (-d < 0.0)
	        {
	        	sN = 0.0;
	        }
	        else if (-d > a)
	        {
	        	sN = sD;
	        }
	        else
	        {
	            sN = -d;
	            sD = a;
	        }
	    }
	    else if (tN > tD) // tc > 1 => the t=1 edge is visible
	    {
	        tN = tD;
	        
	        // recompute sc for this edge
	        if ((-d + b) < 0.0)
	        {
	        	sN = 0;
	        }
	        else if ((-d + b) > a)
	        {
	        	sN = sD;
	        }
	        else
	        {
	            sN = (-d + b);
	            sD = a;
	        }
	    }
	    // finally do the division to get sc and tc
	    sc = (Math.abs(sN) < lcms2_internal.MATRIX_DET_TOLERANCE ? 0.0 : sN / sD);
	    tc = (Math.abs(tN) < lcms2_internal.MATRIX_DET_TOLERANCE ? 0.0 : tN / tD);
	    
	    GetPointOfLine(r, line1, sc);
	    return true;
	}
	
	// ------------------------------------------------------------------ Wrapper
	
	// Allocate & free structure
	public static cmsHANDLE cmsGBDAlloc(cmsContext ContextID)
	{
	    cmsGDB gbd = new cmsGDB();
	    
	    gbd.ContextID = ContextID;
	    
	    return gbd;
	}
	
	public static void cmsGBDFree(cmsHANDLE hGBD)
	{
	    cmsGDB gbd = (cmsGDB)hGBD;
	    if (hGBD != null)
	    {
	    	gbd.Gamut = null;
	    	gbd.ContextID = null;
	    	//_cmsFree(gbd.ContextID, gbd);
	    }
	}
	
	// Auxiliar to retrieve a pointer to the segmentr containing the Lab value
	private static cmsGDBPoint GetPoint(cmsGDB gbd, final cmsCIELab Lab, cmsSpherical sp)
	{
	    cmsVEC3 v = new cmsVEC3();  
	    int[] alpha = new int[1], theta = new int[1];
	    
	    // Housekeeping
	    lcms2_internal._cmsAssert(gbd != null, "gbd != null");
	    lcms2_internal._cmsAssert(Lab != null, "Lab != null");
	    lcms2_internal._cmsAssert(sp != null, "sp != null");
		
	    // Center L* by substracting half of its domain, that's 50 
	    cmsmtrx._cmsVEC3init(v, Lab.L - 50.0, Lab.a, Lab.b);
	    
	    // Convert to spherical coordinates
	    ToSpherical(sp, v);
	    
	    if (sp.r < 0 || sp.alpha < 0 || sp.theta < 0)
	    {
	    	cmserr.cmsSignalError(gbd.ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSSM_SPHERE_OUTOFRANGE), null);
	    	return null;
	    }
	    
	    // On which sector it falls?
	    QuantizeToSector(sp, alpha, theta);
	  	
	    if (alpha[0] < 0 || theta[0] < 0 || alpha[0] >= SECTORS || theta[0] >= SECTORS)
	    {
	    	cmserr.cmsSignalError(gbd.ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSSM_QUAD_OUTOFRANGE), null);
	    	return null;
	    }
	    
	    // Get pointer to the sector
	    return gbd.Gamut[theta[0]][alpha[0]];
	}
	
	// Add a point to gamut descriptor. Point to add is in Lab color space. 
	// GBD is centered on a=b=0 and L*=50
	public static boolean cmsGDBAddPoint(cmsHANDLE hGBD, final cmsCIELab Lab)
	{
	    cmsGDB gbd = (cmsGDB)hGBD;
	    cmsGDBPoint ptr;
	    cmsSpherical sp = new cmsSpherical();
	    
	    // Get pointer to the sector
	    ptr = GetPoint(gbd, Lab, sp);
	    if (ptr == null)
	    {
	    	return false;
	    }
	    
	    // If no samples at this sector, add it
	    if (ptr.Type == GP_EMPTY)
	    {
	        ptr.Type = GP_SPECIFIED;
	        ptr.p    = sp;
	    }
	    else
	    {
	        // Substitute only if radius is greater
	        if (sp.r > ptr.p.r)
	        {
	        	ptr.Type = GP_SPECIFIED;
	        	ptr.p    = sp;
	        }
	    }
	    
	    return true;
	}
	
	// Check if a given point falls inside gamut
	public static boolean cmsGDBCheckPoint(cmsHANDLE hGBD, final cmsCIELab Lab)
	{
	    cmsGDB gbd = (cmsGDB)hGBD;   
	    cmsGDBPoint ptr;
	    cmsSpherical sp = new cmsSpherical();
	    
	    // Get pointer to the sector
	    ptr = GetPoint(gbd, Lab, sp);
	    if (ptr == null)
	    {
	    	return false;
	    }
	    
	    // If no samples at this sector, return no data
	    if (ptr.Type == GP_EMPTY)
	    {
	    	return false;
	    }
	    
	    // In gamut only if radius is greater
	    
	    return (sp.r <= ptr.p.r);
	}
	
	// -----------------------------------------------------------------------------------------------------------------------
	
	// Find near sectors. The list of sectors found is returned on Close[]. 
	// The function returns the number of sectors as well.
	
	// 24   9  10  11  12
	// 23   8   1   2  13
	// 22   7   *   3  14
	// 21   6   5   4  15
	// 20  19  18  17  16
	//
	// Those are the relative movements
	// {-2,-2}, {-1, -2}, {0, -2}, {+1, -2}, {+2,  -2}, 
	// {-2,-1}, {-1, -1}, {0, -1}, {+1, -1}, {+2,  -1},
	// {-2, 0}, {-1,  0}, {0,  0}, {+1,  0}, {+2,   0},
	// {-2,+1}, {-1, +1}, {0, +1}, {+1,  +1}, {+2,  +1},
	// {-2,+2}, {-1, +2}, {0, +2}, {+1,  +2}, {+2,  +2}};
	
	private static class _spiral
	{
		public int AdvX, AdvY;
		
		public _spiral(int AdvX, int AdvY)
		{
			this.AdvX = AdvX;
			this.AdvY = AdvY;
		}
	}
	
	private static final _spiral[] Spiral;
	
	static
	{
		Spiral = new _spiral[]{
			new _spiral(0, -1), new _spiral(+1, -1), new _spiral(+1, 0), new _spiral(+1, +1), new _spiral(0, +1), new _spiral(-1, +1),
			new _spiral(-1, 0), new _spiral(-1, -1), new _spiral(-1, -2), new _spiral(0, -2), new _spiral(+1, -2), new _spiral(+2, -2),
			new _spiral(+2, -1), new _spiral(+2, 0), new _spiral(+2, +1), new _spiral(+2, +2), new _spiral(+1, +2), new _spiral(0, +2),
			new _spiral(-1, +2), new _spiral(-2, +2), new _spiral(-2, +1), new _spiral(-2, 0), new _spiral(-2, -1), new _spiral(-2, -2)
		};
	}
	
	private static final int NSTEPS = 24;
	
	private static int FindNearSectors(cmsGDB gbd, int alpha, int theta, cmsGDBPoint[] Close)
	{
	    int nSectors = 0;
	    int a, t;
	    int i; //Could be an unsigned int like original, but values are less then 24
	    cmsGDBPoint pt;
	    
	    for (i=0; i < NSTEPS; i++)
	    {
	        a = alpha + Spiral[i].AdvX;
	        t = theta + Spiral[i].AdvY;
	        
	        // Cycle at the end
	        a %= SECTORS;
	        t %= SECTORS;
	        
	        // Cycle at the begin
	        if (a < 0) a = SECTORS + a;
	        if (t < 0) t = SECTORS + t;   
	        
	        pt = gbd.Gamut[t][a];
	        
	        if (pt.Type != GP_EMPTY)
	        {
	        	Close[nSectors++] = pt;
	        }                           
	    }
	    
	    return nSectors;
	}
	
	// Interpolate a missing sector. Method identifies whatever this is top, bottom or mid
	private static boolean InterpolateMissingSector(cmsGDB gbd, int alpha, int theta)
	{   
	    cmsSpherical sp;
	    cmsVEC3 Lab;
	    cmsVEC3 Centre;
	    cmsLine ray;
	    int nCloseSectors;
	    cmsGDBPoint[] Close = new cmsGDBPoint[NSTEPS + 1];
	    cmsSpherical closel, templ;
	    cmsLine edge;
	    int k, m;
	    
	    // Is that point already specified?
	    if (gbd.Gamut[theta][alpha].Type != GP_EMPTY)
	    {
	    	return true;
	    }
	    
	    // Fill close points
	    nCloseSectors = FindNearSectors(gbd, alpha, theta, Close);
	    
	    // Find a central point on the sector
	    sp = new cmsSpherical();
	    sp.alpha = ((alpha + 0.5) * 360.0) * (1.0 / (SECTORS));
	    sp.theta = ((theta + 0.5) * 180.0) * (1.0 / (SECTORS));
	    sp.r     = 50.0; 
	    
	    // Convert to Cartesian
	    Lab = new cmsVEC3();
	    ToCartesian(Lab, sp);
	    
	    // Create a ray line from centre to this point
	    Centre = new cmsVEC3();
	    cmsmtrx._cmsVEC3init(Centre, 50.0, 0, 0);
	    ray = new cmsLine();
	    LineOf2Points(ray, Lab, Centre);
	    
	    // For all close sectors
	    closel = new cmsSpherical();
	    closel.r = 0.0;
	    closel.alpha = 0;
	    closel.theta = 0;
	    
	    edge = new cmsLine();
	    for (k=0; k < nCloseSectors; k++)
	    {
	        for(m = k+1; m < nCloseSectors; m++)
	        {
	            cmsVEC3 temp, a1, a2;
	            
	            // A line from sector to sector
	            a1 = new cmsVEC3();
	            a2 = new cmsVEC3();
	            ToCartesian(a1, Close[k].p);
	            ToCartesian(a2, Close[m].p);
	            
	            LineOf2Points(edge, a1, a2);
	            
	            // Find a line
	            temp = new cmsVEC3();
	            ClosestLineToLine(temp, ray, edge);
	            
	            // Convert to spherical
	            templ = new cmsSpherical();
	            ToSpherical(templ, temp);
	            
	            if ( templ.r > closel.r && 
	                 templ.theta >= (theta*180.0/SECTORS) && 
	                 templ.theta <= ((theta+1)*180.0/SECTORS) &&
	                 templ.alpha >= (alpha*360.0/SECTORS) &&
	                 templ.alpha <= ((alpha+1)*360.0/SECTORS)) {
	            	
	                closel = templ;
	            }
	        }
	    }
	    
	    gbd.Gamut[theta][alpha].p = closel;
	    gbd.Gamut[theta][alpha].Type = GP_MODELED;
	    
	    return true;
	}
	
	// Interpolate missing parts. The algorithm fist computes slices at
	// theta=0 and theta=Max.
	public static boolean cmsGDBCompute(cmsHANDLE hGBD, int dwFlags)
	{
	    int alpha, theta;
	    cmsGDB gbd = (cmsGDB)hGBD;
	    
	    lcms2_internal._cmsAssert(hGBD != null, "hGBD != null");
	    
	    // Interpolate black
	    for (alpha = 0; alpha < SECTORS; alpha++)
	    {
	    	if (!InterpolateMissingSector(gbd, alpha, 0))
	    	{
	    		return false;
	    	}
	    }
	    
	    // Interpolate white
	    for (alpha = 0; alpha < SECTORS; alpha++)
	    {
	    	if (!InterpolateMissingSector(gbd, alpha, SECTORS-1))
	    	{
	    		return false;
	    	}
	    }
	    
	    // Interpolate Mid
	    for (theta = 1; theta < SECTORS; theta++)
	    {
	        for (alpha = 0; alpha < SECTORS; alpha++)
	        {
	        	if (!InterpolateMissingSector(gbd, alpha, theta))
		    	{
		    		return false;
		    	}
	        }
	    }
	    
	    // Done
	    return true;
	}
	
	// --------------------------------------------------------------------------------------------------------
	
	// Great for debug, but not suitable for real use
	
//#ifdef FALSE
	
	private static boolean cmsGBDdumpVRML(cmsHANDLE hGBD, final String fname)
	{
	    Stream fp;
	    int i, j;
	    cmsGDB gbd = (cmsGDB)hGBD;
	    cmsGDBPoint pt;
	    
	    fp = Stream.fopen(fname, 'w');
	    if (fp == null)
	    {
	    	return false;
	    }
	    
	    Utility.fprintf (fp, "#VRML V2.0 utf8\n", null);
	    
	    // set the viewing orientation and distance 
	    Utility.fprintf (fp, "DEF CamTest Group {\n", null);
	    Utility.fprintf (fp, "\tchildren [\n", null); 
	    Utility.fprintf (fp, "\t\tDEF Cameras Group {\n", null); 
	    Utility.fprintf (fp, "\t\t\tchildren [\n", null); 
	    Utility.fprintf (fp, "\t\t\t\tDEF DefaultView Viewpoint {\n", null); 
	    Utility.fprintf (fp, "\t\t\t\t\tposition 0 0 340\n", null); 
	    Utility.fprintf (fp, "\t\t\t\t\torientation 0 0 1 0\n", null); 
	    Utility.fprintf (fp, "\t\t\t\t\tdescription \"default view\"\n", null); 
	    Utility.fprintf (fp, "\t\t\t\t}\n", null); 
	    Utility.fprintf (fp, "\t\t\t]\n", null); 
	    Utility.fprintf (fp, "\t\t},\n", null); 
	    Utility.fprintf (fp, "\t]\n", null); 
	    Utility.fprintf (fp, "}\n", null); 
	    
	    // Output the background stuff 
	    Utility.fprintf (fp, "Background {\n", null);
	    Utility.fprintf (fp, "\tskyColor [\n", null);
	    Utility.fprintf (fp, "\t\t.5 .5 .5\n", null);
	    Utility.fprintf (fp, "\t]\n", null);
	    Utility.fprintf (fp, "}\n", null);
	    
	    // Output the shape stuff 
	    Utility.fprintf (fp, "Transform {\n", null);
	    Utility.fprintf (fp, "\tscale .3 .3 .3\n", null);
	    Utility.fprintf (fp, "\tchildren [\n", null);
	    
	    // Draw the axes as a shape: 
	    Utility.fprintf (fp, "\t\tShape {\n", null);
	    Utility.fprintf (fp, "\t\t\tappearance Appearance {\n", null);
	    Utility.fprintf (fp, "\t\t\t\tmaterial Material {\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\tdiffuseColor 0 0.8 0\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\temissiveColor 1.0 1.0 1.0\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\tshininess 0.8\n", null);
	    Utility.fprintf (fp, "\t\t\t\t}\n", null);
	    Utility.fprintf (fp, "\t\t\t}\n", null);
	    Utility.fprintf (fp, "\t\t\tgeometry IndexedLineSet {\n", null);
	    Utility.fprintf (fp, "\t\t\t\tcoord Coordinate {\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\tpoint [\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\t0.0 0.0 0.0,\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\t%f 0.0 0.0,\n",  new Object[]{new Double(255.0)});
	    Utility.fprintf (fp, "\t\t\t\t\t0.0 %f 0.0,\n",  new Object[]{new Double(255.0)});
	    Utility.fprintf (fp, "\t\t\t\t\t0.0 0.0 %f]\n",  new Object[]{new Double(255.0)});
	    Utility.fprintf (fp, "\t\t\t\t}\n", null);
	    Utility.fprintf (fp, "\t\t\t\tcoordIndex [\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\t0, 1, -1\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\t0, 2, -1\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\t0, 3, -1]\n", null);
	    Utility.fprintf (fp, "\t\t\t}\n", null);
	    Utility.fprintf (fp, "\t\t}\n", null);
	    
	    Utility.fprintf (fp, "\t\tShape {\n", null);
	    Utility.fprintf (fp, "\t\t\tappearance Appearance {\n", null);
	    Utility.fprintf (fp, "\t\t\t\tmaterial Material {\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\tdiffuseColor 0 0.8 0\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\temissiveColor 1 1 1\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\tshininess 0.8\n", null);
	    Utility.fprintf (fp, "\t\t\t\t}\n", null);
	    Utility.fprintf (fp, "\t\t\t}\n", null);
	    Utility.fprintf (fp, "\t\t\tgeometry PointSet {\n", null);
	    
	    // fill in the points here 
	    Utility.fprintf (fp, "\t\t\t\tcoord Coordinate {\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\tpoint [\n", null);
	    
	    // We need to transverse all gamut hull.
	    for (i=0; i < SECTORS; i++)
	    {
	        for (j=0; j < SECTORS; j++)
	        {
	            cmsVEC3 v = new cmsVEC3();
	            
	            pt = gbd.Gamut[i][j];
	            ToCartesian(v, pt.p);
	            
	            Utility.fprintf (fp, "\t\t\t\t\t%g %g %g", new Object[]{new Double(v.n[0]+50), new Double(v.n[1]), new Double(v.n[2])});
	            
	            if ((j == SECTORS - 1) && (i == SECTORS - 1))
	            {
	            	Utility.fprintf (fp, "]\n", null);
	            }
	            else
	            {
	            	Utility.fprintf (fp, ",\n", null);
	            }
	        }
	    }
	    
	    Utility.fprintf (fp, "\t\t\t\t}\n", null);
	    
	    // fill in the face colors 
	    Utility.fprintf (fp, "\t\t\t\tcolor Color {\n", null);
	    Utility.fprintf (fp, "\t\t\t\t\tcolor [\n", null);
	    
	    for (i=0; i < SECTORS; i++)
	    {
	        for (j=0; j < SECTORS; j++)
	        {
	        	cmsVEC3 v = new cmsVEC3();
	        	
	            pt = gbd.Gamut[i][j];
	            
	            ToCartesian(v, pt.p);
	            
		        if (pt.Type == GP_EMPTY)
		        {
		        	Utility.fprintf (fp, "\t\t\t\t\t%g %g %g", new Object[]{new Double(0.0), new Double(0.0), new Double(0.0)});
		        }
		        else
		        {
		            if (pt.Type == GP_MODELED)
		            {
		            	Utility.fprintf (fp, "\t\t\t\t\t%g %g %g", new Object[]{new Double(1.0), new Double(.5), new Double(.5)});
		            }
					else
					{
						Utility.fprintf (fp, "\t\t\t\t\t%g %g %g", new Object[]{new Double(1.0), new Double(1.0), new Double(1.0)});
					}
		        }
		        
		        if ((j == SECTORS - 1) && (i == SECTORS - 1))
		        {
		        	Utility.fprintf (fp, "]\n", null);
		        }
		        else
		        {
		        	Utility.fprintf (fp, ",\n", null);
		        }
	        }
	    }
	    Utility.fprintf (fp, "\t\t\t}\n", null);
	    
	    Utility.fprintf (fp, "\t\t\t}\n", null);
	    Utility.fprintf (fp, "\t\t}\n", null);
	    Utility.fprintf (fp, "\t]\n", null);
	    Utility.fprintf (fp, "}\n", null);
	    
	    fp.close();
	    
	    return true;
	}
//#endif
}
