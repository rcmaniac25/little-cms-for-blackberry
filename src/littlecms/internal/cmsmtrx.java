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

import littlecms.internal.lcms2_plugin;
import littlecms.internal.lcms2_plugin.cmsMAT3;
import littlecms.internal.lcms2_plugin.cmsVEC3;
import littlecms.internal.lcms2_internal;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsmtrx
{
	public static void DSWAP(double[] val, int x, int y)
	{
		double tmp = val[x];
		val[x] = val[y];
		val[y] = tmp;
	}
	
	// Initiate a vector
	public static void _cmsVEC3set(cmsVEC3 r, final double[] value, int offset)
	{
		_cmsVEC3init(r, value[offset], value[offset + 1], value[offset + 2]);
	}
	
	public static void _cmsVEC3get(cmsVEC3 r, double[] value, int offset)
	{
		value[offset] = r.n[lcms2_plugin.VX];
		value[offset + 1] = r.n[lcms2_plugin.VY];
		value[offset + 2] = r.n[lcms2_plugin.VZ];
	}
	
	public static void _cmsVEC3init(cmsVEC3 r, double x, double y, double z)
	{
	    r.n[lcms2_plugin.VX] = x;
	    r.n[lcms2_plugin.VY] = y;
	    r.n[lcms2_plugin.VZ] = z;
	}
	
	// Vector substraction
	public static void _cmsVEC3minus(cmsVEC3 r, final cmsVEC3 a, final cmsVEC3 b)
	{
	  r.n[lcms2_plugin.VX] = a.n[lcms2_plugin.VX] - b.n[lcms2_plugin.VX];
	  r.n[lcms2_plugin.VY] = a.n[lcms2_plugin.VY] - b.n[lcms2_plugin.VY];
	  r.n[lcms2_plugin.VZ] = a.n[lcms2_plugin.VZ] - b.n[lcms2_plugin.VZ];
	}
	
	// Vector cross product
	public static void _cmsVEC3cross(cmsVEC3 r, final cmsVEC3 u, final cmsVEC3 v)
	{
	    r.n[lcms2_plugin.VX] = u.n[lcms2_plugin.VY] * v.n[lcms2_plugin.VZ] - v.n[lcms2_plugin.VY] * u.n[lcms2_plugin.VZ];
	    r.n[lcms2_plugin.VY] = u.n[lcms2_plugin.VZ] * v.n[lcms2_plugin.VX] - v.n[lcms2_plugin.VZ] * u.n[lcms2_plugin.VX];
	    r.n[lcms2_plugin.VZ] = u.n[lcms2_plugin.VX] * v.n[lcms2_plugin.VY] - v.n[lcms2_plugin.VX] * u.n[lcms2_plugin.VY];
	}
	
	// Vector dot product
	public static double _cmsVEC3dot(final cmsVEC3 u, final cmsVEC3 v)
	{
	    return u.n[lcms2_plugin.VX] * v.n[lcms2_plugin.VX] + u.n[lcms2_plugin.VY] * v.n[lcms2_plugin.VY] + u.n[lcms2_plugin.VZ] * v.n[lcms2_plugin.VZ];
	}
	
	// Euclidean length 
	public static double _cmsVEC3length(final cmsVEC3 a)
	{
	    return Math.sqrt(a.n[lcms2_plugin.VX] * a.n[lcms2_plugin.VX] +
	    				 a.n[lcms2_plugin.VY] * a.n[lcms2_plugin.VY] +
	    				 a.n[lcms2_plugin.VZ] * a.n[lcms2_plugin.VZ]);
	}

	// Euclidean distance
	public static double _cmsVEC3distance(final cmsVEC3 a, final cmsVEC3 b)
	{
	    double d1 = a.n[lcms2_plugin.VX] - b.n[lcms2_plugin.VX];
	    double d2 = a.n[lcms2_plugin.VY] - b.n[lcms2_plugin.VY];
	    double d3 = a.n[lcms2_plugin.VZ] - b.n[lcms2_plugin.VZ];
	    
	    return Math.sqrt(d1*d1 + d2*d2 + d3*d3);
	}
	
	// 3x3 Identity
	public static void _cmsMAT3set(cmsMAT3 a, final double[] value, int offset)
	{
		_cmsVEC3set(a.v[0], value, offset);
		_cmsVEC3set(a.v[1], value, offset + 3);
		_cmsVEC3set(a.v[2], value, offset + 6);
	}
	
	public static void _cmsMAT3get(cmsMAT3 a, double[] value, int offset)
	{
		_cmsVEC3get(a.v[0], value, offset);
		_cmsVEC3get(a.v[1], value, offset + 3);
		_cmsVEC3get(a.v[2], value, offset + 6);
	}
	
	public static void _cmsMAT3identity(cmsMAT3 a)
	{
	    _cmsVEC3init(a.v[0], 1.0, 0.0, 0.0);
	    _cmsVEC3init(a.v[1], 0.0, 1.0, 0.0);
	    _cmsVEC3init(a.v[2], 0.0, 0.0, 1.0);
	}
	
	private static boolean CloseEnough(double a, double b)
	{
	    return Math.abs(b - a) < (1.0 / 65535.0);
	}
	
	public static boolean _cmsMAT3isIdentity(final cmsMAT3 a)
	{
		cmsMAT3 Identity = new cmsMAT3();
		
		_cmsMAT3identity(Identity);
		
		for(int i = 0; i < 3; i++)
		{
			for(int j = 0; j < 3; j++)
			{
				if(!CloseEnough(a.v[i].n[j], Identity.v[i].n[j]))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	private static double ROWCOL(final cmsMAT3 a, final cmsMAT3 b, int i, int j)
	{
		return a.v[i].n[0]*b.v[0].n[j] + a.v[i].n[1]*b.v[1].n[j] + a.v[i].n[2]*b.v[2].n[j];
	}
	
	// Multiply two matrices
	public static void _cmsMAT3per(cmsMAT3 r, final cmsMAT3 a, final cmsMAT3 b)
	{
	    _cmsVEC3init(r.v[0], ROWCOL(a,b,0,0), ROWCOL(a,b,0,1), ROWCOL(a,b,0,2));
	    _cmsVEC3init(r.v[1], ROWCOL(a,b,1,0), ROWCOL(a,b,1,1), ROWCOL(a,b,1,2));
	    _cmsVEC3init(r.v[2], ROWCOL(a,b,2,0), ROWCOL(a,b,2,1), ROWCOL(a,b,2,2));
	}
	
	// Inverse of a matrix b = a^(-1)
	public static boolean _cmsMAT3inverse(final cmsMAT3 a, cmsMAT3 b)
	{
		double c0 =  a.v[1].n[1]*a.v[2].n[2] - a.v[1].n[2]*a.v[2].n[1];
		double c1 = -a.v[1].n[0]*a.v[2].n[2] + a.v[1].n[2]*a.v[2].n[0];
		double c2 =  a.v[1].n[0]*a.v[2].n[1] - a.v[1].n[1]*a.v[2].n[0];

		double det = a.v[0].n[0]*c0 + a.v[0].n[1]*c1 + a.v[0].n[2]*c2;
		
		if (Math.abs(det) < lcms2_internal.MATRIX_DET_TOLERANCE)
		{
			return false; //singular matrix; can't invert
		}
		det = 1.0 / det;
		
		b.v[0].n[0] = c0 * det;
		b.v[0].n[1] = (a.v[0].n[2]*a.v[2].n[1] - a.v[0].n[1]*a.v[2].n[2]) * det;
		b.v[0].n[2] = (a.v[0].n[1]*a.v[1].n[2] - a.v[0].n[2]*a.v[1].n[1]) * det;
		b.v[1].n[0] = c1 * det;
		b.v[1].n[1] = (a.v[0].n[0]*a.v[2].n[2] - a.v[0].n[2]*a.v[2].n[0]) * det;
		b.v[1].n[2] = (a.v[0].n[2]*a.v[1].n[0] - a.v[0].n[0]*a.v[1].n[2]) * det;
		b.v[2].n[0] = c2 * det;
		b.v[2].n[1] = (a.v[0].n[1]*a.v[2].n[0] - a.v[0].n[0]*a.v[2].n[1]) * det;
		b.v[2].n[2] = (a.v[0].n[0]*a.v[1].n[1] - a.v[0].n[1]*a.v[1].n[0]) * det;
		
		return true;
	}
	
	// Solve a system in the form Ax = b
	public static boolean _cmsMAT3solve(cmsVEC3 x, cmsMAT3 a, cmsVEC3 b)
	{
		cmsMAT3 a_1 = new cmsMAT3();
		
	    if (!_cmsMAT3inverse(a, a_1))
	    {
	    	return false; // Singular matrix
	    }
	    
	    _cmsMAT3eval(x, a_1, b);
	    return true;
	}
	
	// Evaluate a vector across a matrix
	public static void _cmsMAT3eval(cmsVEC3 r, final cmsMAT3 a, final cmsVEC3 v)
	{
	    r.n[lcms2_plugin.VX] = a.v[0].n[lcms2_plugin.VX]*v.n[lcms2_plugin.VX] + a.v[0].n[lcms2_plugin.VY]*v.n[lcms2_plugin.VY] + a.v[0].n[lcms2_plugin.VZ]*v.n[lcms2_plugin.VZ];
	    r.n[lcms2_plugin.VY] = a.v[1].n[lcms2_plugin.VX]*v.n[lcms2_plugin.VX] + a.v[1].n[lcms2_plugin.VY]*v.n[lcms2_plugin.VY] + a.v[1].n[lcms2_plugin.VZ]*v.n[lcms2_plugin.VZ];
	    r.n[lcms2_plugin.VZ] = a.v[2].n[lcms2_plugin.VX]*v.n[lcms2_plugin.VX] + a.v[2].n[lcms2_plugin.VY]*v.n[lcms2_plugin.VY] + a.v[2].n[lcms2_plugin.VZ]*v.n[lcms2_plugin.VZ];
	}
}
