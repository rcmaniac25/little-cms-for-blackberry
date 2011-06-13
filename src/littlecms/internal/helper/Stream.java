//#preprocessor

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
//@Author Vinnie Simonetti
package littlecms.internal.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
import net.rim.device.api.io.FileNotFoundException;
import net.rim.device.api.io.Seekable;
//#endif

/**
 * Generic data stream for files and the likes. Functions based off Standard C functions.
 */
public abstract class Stream
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
	implements Seekable
//#endif
{
	public static final int SEEK_SET = 0;
	public static final int SEEK_CUR = 1;
	public static final int SEEK_END = 2;
	
	public abstract long getPosition();
	
	public void setPosition(long pos)
	{
		seek(pos, SEEK_SET);
	}
	
	/**
	 * Seek through the stream.
	 * @param offset The offset to seek to, based on origin.
	 * @param origin The origin to seek from, either {@link #SEEK_SET}, {@link #SEEK_CUR}, or {@link #SEEK_END}
	 * @return If zero is returned then the seek operation completed successfully, else an error occurred.
	 */
	public abstract int seek(long offset, int origin);
	
	/**
	 * @param buffer The location data should be read to.
	 * @param offset The offset in the buffer to where data should start being written to.
	 * @param size The size of an item to read
	 * @param count The number of items of <i>size</i> length to read.
	 * @return Number of items read in
	 */
	public abstract long read(byte[] buffer, int offset, int size, int count);
	
	/**
	 * @param buffer The location data should be read from.
	 * @param offset The offset in the buffer to where data should start being read from.
	 * @param size The size of an item to write
	 * @param count The number of items of <i>size</i> length to write.
	 * @return Number of items written
	 */
	public abstract long write(byte[] buffer, int offset, int size, int count);
	
	/**
	 * Close the stream.
	 * @return If zero is returned then the operation completed successfully, else an error occurred.
	 */
	public abstract int close();
	
	public final int readByte()
	{
		long pos = this.getPosition();
		byte[] data = new byte[1];
		if(this.read(data, 0, 1, 1) != 0)
		{
			return data[0] & 0xFF;
		}
		this.seek(pos, SEEK_SET);
		return -1;
	}
	
	/**
	 * Determine if the end-of-file has been reached.
	 * @return <code>true</code> if end-of-file has been reached, <code>false</code> if otherwise.
	 */
	public final boolean eof()
	{
		long pos = this.getPosition(); //Remember the initial position
		if(this.seek(0, SEEK_END) == 0)
		{
			//Successfully went to the end of the Stream.
			boolean end = pos >= this.getPosition(); //See if the position is the same as or greater then original position
			if(this.seek(pos, SEEK_SET) == 0)
			{
				//Successfully went to the original position of the Stream.
				return end;
			}
		}
		return true; //O no, a seek operation failed. Treat this like a end of file.
	}
	
	public static Stream fopen(final String filename, final String mode)
	{
		FileStream fs = new FileStream(filename, mode, 0);
		if(!fs.valid)
		{
			//Not a valid file
			fs.close();
			fs = null;
		}
		return fs;
	}
	
	static class FileStream extends Stream
	{
		public boolean valid;
		private FileConnection file;
		private InputStream in;
		private OutputStream out;
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
		private Seekable inSeek;
		private Seekable outSeek;
//#else
		private long pos;
//#endif
		
		public FileStream(final String filename, final String mode, long pos)
		{
			int iMode = 0;
			int len = mode.length();
			boolean startAtBeginning = true;
		END_BAD:
			for(int i = 0; i < len; i++)
			{
				//Full parameter checks are not really done since this function (though it can be accessed publicly) is only sent 'w' and 'r' by LCMS
				switch(mode.charAt(i))
				{
					case 'r':
						iMode |= Connector.READ;
						break;
					case 'w':
						iMode |= Connector.WRITE;
						break;
					case 'b':
						//Just in case extra (valid but not needed) parameters are entered
						break;
					case 'a':
						startAtBeginning = false;
						break;
					case '+':
						iMode |= Connector.READ_WRITE;
						break;
					default:
						iMode = 0;
						break END_BAD;
				}
			}
			if(iMode == 0 || !filename.startsWith("file"))
			{
				//No valid parameter found
				return;
			}
			if(startAtBeginning)
			{
				pos = 0;
			}
			try
			{
				file = (FileConnection)Connector.open(filename, iMode == Connector.READ ? iMode : Connector.READ_WRITE);
				if((iMode & Connector.READ) != 0)
				{
					in = file.openInputStream();
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
					if(in.markSupported())
					{
						in.mark(Integer.MAX_VALUE); //Set mark so reading doesn't need to occur
					}
					in.skip(pos);
//#endif
				}
				if((iMode & Connector.WRITE) != 0)
				{
					if(file.exists())
					{
						//Make sure that none of the previous data exists in the file once processed.
						file.truncate(0);
					}
					else
					{
						//Create the file automatically since in write mode
						file.create();
					}
					out = file.openOutputStream(pos);
				}
				valid = true;
			}
			catch (IOException e) //IO errors
			{
			}
			catch (IllegalArgumentException e) //Bad file name
			{
			}
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
			if(!valid)
			{
				//No need to crash
				return;
			}
			if((iMode & Connector.READ) != 0)
			{
				inSeek = (Seekable)in;
				if(pos != 0)
				{
					try
					{
						inSeek.setPosition(pos);
					}
					catch (IOException e)
					{
						//Couldn't use Seekable, do it old fashioned way
						try
						{
							in = file.openInputStream();
							inSeek = null;
							if(in.markSupported())
							{
								in.mark(Integer.MAX_VALUE); //Set mark so reading doesn't need to occur
							}
							in.skip(pos);
						}
						catch (IOException e2)
						{
						}
					}
				}
			}
			if((iMode & Connector.WRITE) != 0)
			{
				outSeek = (Seekable)out;
			}
//#else
			this.pos = pos;
//#endif
		}
		
		public String getFilename()
		{
			return file.getURL();
		}
		
		public int getMode()
		{
			int mode = 0;
			if(in != null)
			{
				mode |= Connector.READ;
			}
			if(out != null)
			{
				mode |= Connector.WRITE;
			}
			return mode;
		}
		
		public long write(byte[] buffer, int offset, int size, int count)
		{
			if(out == null)
			{
				return 0;
			}
			count = Math.min(buffer.length / size, count) * size;
			int written = 0;
			try
			{
				out.write(buffer, offset, count);
				out.flush();
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
				pos += count;
//#endif
				if(in != null)
				{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
					if(inSeek != null)
					{
						inSeek.setPosition(inSeek.getPosition() + count);
					}
					else
//#endif
					in.skip(count);
				}
				written = count;
			}
			catch(IOException ioe)
			{
				written = 0;
			}
			return written / size;
		}
		
		public int seek(long offset, int origin)
		{
			long absPos = getPosition();
			switch(origin)
			{
				case SEEK_SET:
					absPos = offset;
					break;
				case SEEK_CUR:
					absPos += offset;
					break;
				case SEEK_END:
					try
					{
						absPos = file.fileSize() + offset;
					}
					catch(IOException ioe)
					{
					}
					break;
			}
			if(absPos == getPosition())
			{
				//Nothing changed, return
				return 0;
			}
			try
			{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
				if(out != null)
				{
					outSeek.setPosition(absPos);
				}
//#else
				if(out != null)
				{
					out.flush();
					out.close();
					out = file.openOutputStream(absPos);
				}
				pos = absPos;
//#endif
				if(in != null)
				{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
					if(inSeek != null)
					{
						inSeek.setPosition(absPos);
					}
					else
					{
//#endif
					long cpos = getPosition();
					if(cpos < absPos)
					{
						in.skip(absPos - cpos);
					}
					else
					{
						if(in.markSupported())
						{
							in.reset();
							in.mark(Integer.MAX_VALUE); //Precaution
						}
						else
						{
							in.close();
							in = file.openInputStream();
						}
						in.skip(absPos);
					}
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
					}
//#endif
				}
			}
			catch(IOException ioe)
			{
			}
			return getPosition() == absPos ? 0 : 1;
		}
		
		public long read(byte[] buffer, int offset, int size, int count)
		{
			if(in == null)
			{
				return 0;
			}
			count = Math.min(buffer.length / size, count) * size;
			int read;
			try
			{
				read = in.read(buffer, offset, count);
				if(read > -1)
				{
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
					pos += read;
//#endif
					if(out != null)
					{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
						outSeek.setPosition(outSeek.getPosition() + read);
//#else
						out.flush();
						out.close();
						out = file.openOutputStream(pos);
//#endif
					}
				}
				else
				{
					read = 0;
				}
			}
			catch(IOException ioe)
			{
				read = 0;
			}
			return read / size;
		}
		
		public long getPosition()
		{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
			try
			{
				if(in != null)
				{
					return inSeek.getPosition();
				}
				else
				{
					return outSeek.getPosition();
				}
			}
			catch(IOException ioe)
			{
				return -1L;
			}
//#else
			return this.pos;
//#endif
		}
		
		public int close()
		{
			if(file != null)
			{
				try
				{
					if(in != null)
					{
						in.close();
					}
					if(out != null)
					{
						out.flush();
						out.close();
					}
					file.close();
				}
				catch (IOException e)
				{
					return -1;
				}
			}
			return 0;
		}
	}
}
