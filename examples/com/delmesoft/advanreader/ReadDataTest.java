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

import java.util.Scanner;

import org.junit.Test;

import com.delmesoft.advanreader.AdvanReader.MemoryBank;

public class ReadDataTest {

	@Test
	public void test() throws ReaderException {
		
		String host = "192.168.1.109";
		
		Settings settings = new Settings();
		settings.setPort(3161);
		settings.setHost(host);
		settings.setAntennas(new int[] { 1 });
		settings.setSession(1);
		settings.setSearchModeIndex(2); // 2 = AB

		double txPower = 10; // Power

		double[] txPowers = new double[settings.getAntennas().length];
		for (int i = 0; i < txPowers.length; ++i) {
			txPowers[i] = txPower;
		}
		settings.setTxPower(txPowers);

		double sensitivity = -70;
		double[] txSensitivities = new double[settings.getAntennas().length];
		for (int i = 0; i < txSensitivities.length; ++i) {
			txSensitivities[i] = sensitivity;
		}
		settings.setRxSensitivity(txSensitivities);
		
		Reader reader = new AdvanReader();
		reader.setSettings(settings);
		
		reader.setReaderListener(new ReaderListenerAdapter() {

			@Override
			public void onWrite(Read read, int result, String message) {}

			@Override
			public void onRead(Read read) {				
				// System.out.println("READ: " + read);				
			}

			@Override
			public void onConnectionLost() {
				System.out.println("Connection Lost");
			}

			@Override
			public void onGpi(int portNumber, boolean state) {
				System.out.println("GPI: " + portNumber + ", state: " + state);
			}

			@Override
			public void onReadData(Read read, int result, String message) {
				System.out.printf("%s (code: %d): %s\n", message, result, read);
			}
				

		});

		reader.connect();
		System.out.println(reader.getModelName());
		
		// EPC
	    // reader.readData(MemoryBank.EPC.ordinal(), 2, 6);
		
		// TID                                              
	    reader.readData(MemoryBank.TID.ordinal(), 2, 4 );
		
		// Chip Id
		// reader.readData(MemoryBank.TID.ordinal(), 0, 2);
		
		// Access Pwd
		// reader.readData(MemoryBank.RESERVED.ordinal(), 2, 2);
		
		// Kill Pwd
		// reader.readData(MemoryBank.RESERVED.ordinal(), 0, 2);
		
		System.out.println("Reading..., press enter to finish.");
		try(Scanner in = new Scanner(System.in)) {
			in.nextLine();
			reader.disconnect();
		}
		
	}

}
