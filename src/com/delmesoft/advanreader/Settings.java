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

import java.util.Arrays;

public class Settings {

	private String host;
	private int port;

	private int[] antennas;
	private double[] txPower = { 30, 30, 30, 30 };
	private double[] rxSensitivity = { -70, -70, -70, -70 };

	private int session = 1;
	private int readerModeIndex;
	private int searchModeIndex;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int[] getAntennas() {
		return antennas;
	}

	public void setAntennas(int[] antennas) {
		this.antennas = antennas;
	}

	public double[] getTxPower() {
		return txPower;
	}

	public void setTxPower(double[] txPower) {
		this.txPower = txPower;
	}

	public double[] getRxSensitivity() {
		return rxSensitivity;
	}

	public void setRxSensitivity(double[] rxSensitivity) {
		this.rxSensitivity = rxSensitivity;
	}

	public int getSession() {
		return session;
	}

	public void setSession(int session) {
		this.session = session;
	}

	public int getReaderModeIndex() {
		return readerModeIndex;
	}

	public void setReaderModeIndex(int readerModeIndex) {
		this.readerModeIndex = readerModeIndex;
	}

	public int getSearchModeIndex() {
		return searchModeIndex;
	}

	public void setSearchModeIndex(int searchModeIndex) {
		this.searchModeIndex = searchModeIndex;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Settings [host=");
		builder.append(host);
		builder.append(", port=");
		builder.append(port);
		builder.append(", antennas=");
		builder.append(Arrays.toString(antennas));
		builder.append(", txPower=");
		builder.append(Arrays.toString(txPower));
		builder.append(", rxSensitivity=");
		builder.append(Arrays.toString(rxSensitivity));
		builder.append(", session=");
		builder.append(session);
		builder.append(", readerModeIndex=");
		builder.append(readerModeIndex);
		builder.append(", searchModeIndex=");
		builder.append(searchModeIndex);
		builder.append("]");
		return builder.toString();
	}
	
}
