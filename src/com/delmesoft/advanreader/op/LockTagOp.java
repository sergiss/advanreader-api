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
import com.delmesoft.advanreader.LockOptions;
import com.delmesoft.advanreader.Read;
import com.delmesoft.advanreader.ReaderListener;

public class LockTagOp extends AdvanOp {

	private String oldAccessPassword, 
				   newAccessPassword;

	private LockOptions lockOptions;	

	public String getOldAccessPassword() {
		return oldAccessPassword;
	}

	public void setOldAccessPassword(String oldAccessPassword) {
		this.oldAccessPassword = oldAccessPassword;
	}

	public String getNewAccessPassword() {
		return newAccessPassword;
	}

	public void setNewAccessPassword(String newAccessPassword) {
		this.newAccessPassword = newAccessPassword;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public void setLockOptions(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
	}

	@Override
	public void perform(Read read, AdvanReader reader, ReaderListener readerListener) {
		
		try {
			int port = reader.getSettings().getPort();
			Device device = reader.getDevice();
			reader.lockTag(device, port, epc, oldAccessPassword, newAccessPassword, null, lockOptions);
			readerListener.onLock(read, 0, "setLock"); // OK
		} catch (Exception e) {
			readerListener.onLock(read, -1, e.getMessage());
		}
	
	}

}
