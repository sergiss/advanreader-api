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
package com.delmesoft.advanreader;

import java.util.Map;

public interface Reader {
		
	void connect() throws ReaderException;
	
	boolean isConnected();
	
	void disconnect();
	
	void readData(int bank, int address, int lenght) throws ReaderException;
	
	void readData(int bank, int address, int lenght, String accessPassword) throws ReaderException;
	
	void readData(String epc, int bank, int address, int lenght) throws ReaderException;
	
	void readData(String epc, int bank, int address, int lenght, String accessPassword) throws ReaderException;
	
	void startRead() throws ReaderException;
	
	boolean isReading();
	
	void setKillPassword(String epc, String killPassword) throws ReaderException;
	
	void setKillPassword(String epc, String accessPassword, String killPassword) throws ReaderException;
	
	void lockTag(String epc, String accessPassword, LockOptions lockOptions) throws ReaderException;
	
	void lockTag(String epc, String oldAccessPassword, String newAccessPassword, LockOptions lockOptions) throws ReaderException;
	
	void writeEpc(String srcEpc, String tgtEpc) throws ReaderException;
	
	void writeEpc(String srcEpc, String tgtEpc, String accessPassword) throws ReaderException;
	
	void writeData(String epc, String data, int memoryBank, short wordPointer) throws ReaderException;

	void writeData(String epc, String data, int memoryBank, short wordPointer, String accessPassword) throws ReaderException;
	
	void stop() throws ReaderException;
	
	int getGpoCount();
	
	void setGpo(int portNumber, boolean state) throws ReaderException;
	
	void setGpo(Map<Integer, Boolean> stateMap) throws ReaderException;
	
	void setGpo(boolean[] state) throws ReaderException;
	
	int getGpiCount();
	
	boolean isGpi(int portNumber) throws ReaderException;
	
	boolean[] getGpiState() throws ReaderException;
	
	void setSettings(Settings settings);
	
	Settings getSettings();
	
	void applySettings(Settings settings) throws ReaderException;
	
	String getSerial();
	
	String getModelName();
	
	void setReaderListener(ReaderListener readerListener);
	
	ReaderListener getReaderListener();
	
}
