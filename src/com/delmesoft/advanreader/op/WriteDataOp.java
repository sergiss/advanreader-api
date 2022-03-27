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
package com.delmesoft.advanreader.op;

import com.delmesoft.advanreader.AdvanReader;
import com.delmesoft.advanreader.Device;
import com.delmesoft.advanreader.Read;
import com.delmesoft.advanreader.ReaderListener;

public class WriteDataOp extends AdvanOp {
	
	private String data;
	private String accessPassword;
	private int memoryBank;
	private short wordPointer;
	
	public String getData() {
		return data;
	}
	
	public void setData(String data) {
		this.data = data;
	}

	public String getAccessPassword() {
		return accessPassword;
	}

	public void setAccessPassword(String accessPassword) {
		this.accessPassword = accessPassword;
	}
	
	public void setMemoryBank(int memoryBank) {
		this.memoryBank = memoryBank;
	}

	public void setWordPointer(short wordPointer) {
		this.wordPointer = wordPointer;
	}

	public int getMemoryBank() {
		return memoryBank;
	}

	public short getWordPointer() {
		return wordPointer;
	}

	@Override
	public void perform(Read read, AdvanReader reader, ReaderListener readerListener) {
		try {
			Device device = reader.getDevice();
			int port = reader.getSettings().getPort();
			reader.writeData(device, port, epc, data, memoryBank, wordPointer, accessPassword);
			readerListener.onWrite(read, 0, "writeData"); // OK
		} catch (Exception e) {
			readerListener.onWrite(read, -1, e.getMessage());
		}
	}

}
