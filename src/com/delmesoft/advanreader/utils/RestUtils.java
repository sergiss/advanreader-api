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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestUtils {

	private final StringBuilder sb = new StringBuilder();

	private int connectTimeout;

	public String sendGet(URL url) throws IOException {
	
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
		try {
			return getData(conn.getInputStream());
		} catch (Exception e) {
			return getData(conn.getErrorStream());
		}
	}

	public String sendPut(URL url, String postData) throws IOException {

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setConnectTimeout(connectTimeout);
		conn.setRequestMethod("PUT");
		conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		// Send post request
		conn.setDoOutput(true);

		try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
			dos.writeBytes(postData);
		}

		try {
			return getData(conn.getInputStream());
		} catch (Exception e) {
			return getData(conn.getErrorStream());
		}

	}

	private synchronized String getData(InputStream is) throws IOException {
		if (is != null) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
				String line;
				while ((line = in.readLine()) != null) {
					sb.append(line);
				}
				return sb.toString();
			} finally {
				sb.setLength(0);
			}
		}
		return null;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	
}
