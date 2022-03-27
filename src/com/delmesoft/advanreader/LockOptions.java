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

public class LockOptions {
		
	public static final int LOCK         = 0; // Write Lock
	public static final int PERMA_LOCK   = 1; // Permanent Write Lock
	public static final int PERMA_UNLOCK = 2; // Permanent Write Unlock
	public static final int UNLOCK       = 3; // Write Unlock
	public static final int NONE         = 4; // No Action
	
	public static final LockOptions UNLOCK_EPC = valueOf(UNLOCK, NONE, NONE  , NONE  , NONE);
	public static final LockOptions UNLOCK_ALL = valueOf(UNLOCK, NONE, UNLOCK, UNLOCK, NONE);
	public static final LockOptions LOCK_ALL   = valueOf(LOCK  , NONE, LOCK  , LOCK  , NONE);
	public static final LockOptions LOCK_USER  = valueOf(NONE  , NONE, LOCK  , NONE  , NONE);
		
	private int accessPasswordLockType;
	private int epcLockType;
	private int killPasswordLockType;
	private int tidLockType;
	private int userLockType;
	
	public LockOptions() {
		accessPasswordLockType = NONE;
		epcLockType = NONE;
		killPasswordLockType = NONE;
		tidLockType = NONE;
		userLockType = NONE;
	}
		
	public LockOptions(int epcLockType, int tidLockType, int userLockType, int accessPasswordLockType, int killPasswordLockType) {
		super();
		this.epcLockType = epcLockType;
		this.tidLockType = tidLockType;
		this.userLockType = userLockType;
		this.accessPasswordLockType = accessPasswordLockType;
		this.killPasswordLockType = killPasswordLockType;		
	}

	public int getAccessPasswordLockType() {
		return accessPasswordLockType;
	}
	
	public void setAccessPasswordLockType(int accessPasswordLockType) {
		this.accessPasswordLockType = accessPasswordLockType;
	}
	
	public int getEpcLockType() {
		return epcLockType;
	}
	
	public void setEpcLockType(int epcLockType) {
		this.epcLockType = epcLockType;
	}
	
	public int getKillPasswordLockType() {
		return killPasswordLockType;
	}
	
	public void setKillPasswordLockType(int killPasswordLockType) {
		this.killPasswordLockType = killPasswordLockType;
	}
	
	public int getTidLockType() {
		return tidLockType;
	}
	
	public void setTidLockType(int tidLockType) {
		this.tidLockType = tidLockType;
	}
	
	public int getUserLockType() {
		return userLockType;
	}
	
	public void setUserLockType(int userLockType) {
		this.userLockType = userLockType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LockOptions [accessPasswordLockType=");
		builder.append(accessPasswordLockType);
		builder.append(", epcLockType=");
		builder.append(epcLockType);
		builder.append(", killPasswordLockType=");
		builder.append(killPasswordLockType);
		builder.append(", tidLockType=");
		builder.append(tidLockType);
		builder.append(", userLockType=");
		builder.append(userLockType);
		builder.append("]");
		return builder.toString();
	}
	
	public static LockOptions valueOf(int epcLockType, int tidLockType, int userLockType, int accessPasswordLockType, int killPasswordLockType) {
		return new LockOptions(epcLockType, tidLockType, userLockType, accessPasswordLockType, killPasswordLockType);
	}
	
}
