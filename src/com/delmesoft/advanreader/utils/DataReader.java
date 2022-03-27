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

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public abstract class DataReader {

	private Thread thread;
	
	private StringBuffer stringBuffer;

	public DataReader() {
		stringBuffer = new StringBuffer();
	}
	
	public void disconnect() {
		if(isConnected()) {
			try {
				thread.interrupt();
			} catch(Exception e) {
			} finally {
				thread = null;
			}
		}
	}

	public boolean isConnected() {
		return thread != null && !thread.isInterrupted();
	}

	public void connect(String host, int port) {

		if (!isConnected()) {

			thread = new Thread(DataReader.class.getName()) {

				@Override
				public void run() {

					try (Socket socket = new Socket(host, port); 
						InputStream inputStream = socket.getInputStream()) {

						final InputSource inputSource = new InputSource();
						final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						final Charset charset = Charset.forName("UTF-8");

						while (!isInterrupted()) {
							String xml = readXml(inputStream, charset);
							if (!xml.isEmpty()) {
								Document document = documentBuilder.parse(inputSource.setString(xml));
								handleDocument(document);
							}
						}

					} catch (Exception e) {
						if (isConnected()) {
							handleError(e);
						}
					} finally {
						disconnect();
					}

				}

			};
			thread.start();

		}

	}
	
	public abstract void handleDocument(Document document);
	
	public abstract void handleError(Exception e);

	protected String readXml(InputStream is, Charset charset) throws IOException {
		try {
			
			// *** Read Header ***
			
			String line;
			do {
				line = stringBuffer.readLine(is, charset);
			} while (line == null || !line.contains("ADVANNET"));

			// Content-Length:xxxx
			line = stringBuffer.readLine(is, charset);
			int contentLength = Integer.parseInt(line.split(":")[1].trim());
			
			// Content-Type:text/xml
			line = stringBuffer.readLine(is, charset);
			String contenType = line.split(":")[1].trim();
			if(!contenType.equals("text/xml")) {
				throw new RuntimeException("Error unsupported content type: " + contenType);
			}
			stringBuffer.readLine(is, charset); // empty line

			// *** end Read Header ***
			stringBuffer.read(is, contentLength - stringBuffer.size());
			if (stringBuffer.size() != contentLength) {
				throw new RuntimeException("Error reading xml");
			}

			return stringBuffer.toString(charset);
			
		} finally {
			stringBuffer.clear();
		}
	}

}
