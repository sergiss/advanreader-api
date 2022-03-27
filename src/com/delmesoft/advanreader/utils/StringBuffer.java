/* 
 * Copyright (c) 2022, Sergio S.- sergi.ss4@gmail.com http://sergiosoriano.com
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.delmesoft.advanreader.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class StringBuffer {
	
	private byte[] buffer = new byte[1024];
	private int index;
	
	public void storeByte(int b) {
		resize(index);
		buffer[index++] = (byte) b;
	}
	
	public String readLine(InputStream is, Charset charset) throws IOException {
		int b;
		while ((b = is.read()) != -1) { // next byte
			if (b == 13) { // carriage return (/r)				
				b = is.read(); // next byte
				String result = new String(buffer, 0, index, charset);
				index = 0;
				if (b != 10 && b != -1) { // not new line (/n) and not end of input stream
					storeByte(b); // store byte
				}
				return result; // done
			} else if (b == 10) { // new line (/n)
				int i = index;
				index = 0;
				return new String(buffer, 0, i, charset); // done
			}
			storeByte(b); // store byte
		}
		return null;
	}
	
	public void read(final InputStream is, final int length) throws IOException {
		resize(index + length);
		int count, n = 0;
		while(n < length) {
			count = is.read(buffer, index + n, length - n);
			if(count < 0) throw new EOFException();
			n += count;
		}
		index += n;
	}
	
	public void resize(int size) {
		if (size >= buffer.length) {
			byte[] tmp = new byte[size];
			System.arraycopy(buffer, 0, tmp, 0, index);
			buffer = tmp;
		}
	}
	
	public int size() {
		return index;
	}
	
	public void clear() {
		index = 0;
	}

	public String toString(Charset charset) {
		return new String(buffer, 0, index);
	}

}
