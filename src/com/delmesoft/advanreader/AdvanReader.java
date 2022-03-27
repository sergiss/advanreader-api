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

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.delmesoft.advanreader.op.AdvanOp;
import com.delmesoft.advanreader.op.LockTagOp;
import com.delmesoft.advanreader.op.ReadDataByEpcOp;
import com.delmesoft.advanreader.op.ReadDataOp;
import com.delmesoft.advanreader.op.SetKillPwdOp;
import com.delmesoft.advanreader.op.WriteDataOp;
import com.delmesoft.advanreader.utils.DataReader;
import com.delmesoft.advanreader.utils.InputSource;
import com.delmesoft.advanreader.utils.RestUtils;
import com.delmesoft.advanreader.utils.Utils;

public class AdvanReader implements Reader {
	
	public enum MemoryBank {
		RESERVED, EPC, TID, USER;
	}

	public static final int[] TX_FREQUENCIES  = { 865700, 866300, 866900, 867500 };
	public static final String[] GEN2_SESSION = { "S0", "S1", "S2", "S3" };
	public static final String[] GEN2_TARGET  = { "A", "B", "AB", "BA" };

	public static final int TCP_PORT = 3177;

	public static final long TAG_OP_TIMEOUT  = 1000;
	public static final int RF_WRITE_RETRIES = 2;

	private final XPathExpression expressionVersion;
	private final XPathExpression expressionDevice;
	private final XPathExpression expressionReadModes;
	private final XPathExpression expressionResult;
	private final XPathExpression expressionGpiAll;
	private final XPathExpression expressionError;
	private final XPathExpression expressionTimestamp;

	private final InputSource inputSource;

	private final RestUtils restUtils;

	private final Map<String, Read> readMap;
	private final List<AdvanOp> opList;

	private ReaderListener readerListener;

	private Settings settings;

	private int gpoCount;
	private int gpiCount;

	private Device device;
	private boolean reading, singularizing;

	private long timestampStart = Long.MAX_VALUE;

	public AdvanReader() {

		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			expressionVersion   = xPath.compile("/response/msg-version/text()");
			expressionDevice    = xPath.compile("/response/data/devices/device/*[self::id or self::serial or self::family]/text()");
			expressionReadModes = xPath.compile("/response/data/entries/entry/readModes/readMode/name/text()");
			expressionResult    = xPath.compile("/response/data/result/text()");
			expressionGpiAll    = xPath.compile("/response/data/entries/entry/*[self::index or self::result]/text()");
			expressionError     = xPath.compile("/response/msg/text()");
			expressionTimestamp = xPath.compile("/response/ts/text()");
		} catch (Exception e) {
			throw new RuntimeException("Error initializing extensions", e);
		}

		inputSource = new InputSource();
		restUtils = new RestUtils();

		opList = new ArrayList<AdvanOp>();
		readMap = new HashMap<>();

	}

	private DataReader dataReader = new DataReader() {

		@Override
		public void handleError(Exception e) {
			// e.printStackTrace();
			readerListener.onConnectionLost();
		}

		@Override
		public void handleDocument(Document document) {

			Element element = document.getDocumentElement();		

			switch (element.getNodeName()) {
			case "inventory": { // INVENTORY :
				element = (Element) element.getElementsByTagName("data").item(0);	
				element = (Element) element.getElementsByTagName("inventory").item(0);
				element = (Element) element.getElementsByTagName("items").item(0);
				NodeList itemList = element.getElementsByTagName("item");
				for(int i = 0; i < itemList.getLength(); ++i) {
					element = (Element) itemList.item(i); // item
					String value = element.getElementsByTagName("ts").item(0).getTextContent();
					long timestamp = Long.valueOf(value);
					if(timestamp >= timestampStart) { // fix bug keonn :)
						element = (Element) element.getElementsByTagName("data").item(0);
						String hexEpc = ((Element) element.getElementsByTagName("hexepc").item(0)).getTextContent();

						Read read = new Read();
						read.setEpc(Utils.toUpperCase(hexEpc));
						element = (Element) element.getElementsByTagName("props").item(0);
						NodeList propList = element.getElementsByTagName("prop");
						for(int j = 0; j < propList.getLength(); ++j) {
							element = (Element) propList.item(j); // prop
							value = element.getTextContent();
							if(value.contains("RSSI")) {
								String result = getPropertyValue(value);
								read.setRssi(Double.valueOf(result));
							} else if(value.contains("ANTENNA_PORT")) {	
								String result = getPropertyValue(value);
								read.setAntennaId(Integer.valueOf(result));
							} 
						}

						handleRead(read);

					}/* else {
						System.out.println(timestampStart);
						System.out.println(timestamp);
						System.out.println("-------------");
					}*/
				}

				handleOpList();	
				break;
			}
			case "deviceEventMessage": { // EVENT :

				element = (Element) element.getElementsByTagName("event").item(0);
				String type = ((Element) element.getElementsByTagName("type").item(0)).getTextContent();
				switch(type) {
				case "GPI": {
					String line = ((Element) element.getElementsByTagName("line").item(0)).getTextContent();
					String lowToHigh = ((Element) element.getElementsByTagName("lowToHigh").item(0)).getTextContent();
					readerListener.onGpi(Integer.valueOf(line), Boolean.parseBoolean(lowToHigh));				
				}
				}
				break;			
			}
			default:
				break;
			}

		}

	};

	@Override
	public synchronized void connect() throws ReaderException {
		if(!isConnected()) {
			applySettings(settings);
			dataReader.connect(settings.getHost(), TCP_PORT);
		}
	}

	@Override
	public void applySettings(Settings settings) throws ReaderException {

		try {

			String host = this.settings.getHost();
			int port = this.settings.getPort();

			device = getDevices(host, port).iterator().next();

			_stopDevice(device, port);

			Set<String> readModes = getDeviceModes(device, port);

			if(!readModes.contains(Device.ReadMode.AUTONOMOUS.name())) {
				throw new RuntimeException("Autonomous mode unsupported");
			}

			setAntennaConfiguration(device, port, settings);

			setWritePower(device, port, 31.5);

			setParameter(device, port, "GEN2_SESSION", GEN2_SESSION[settings.getSession()]);
			setParameter(device, port, "GEN2_TARGET" , GEN2_TARGET[settings.getSearchModeIndex()]);

			gpiCount = toInt(getParameter(device, port, "DATA_GPI_NUMBER"));
			gpoCount = toInt(getParameter(device, port, "DATA_GPO_NUMBER"));

			if(reading) {
				_startDevice(device, port);
			}

			// TODO : configure gpio ...

		} catch (Exception e) {
			throw new ReaderException("Error applying settings", e);
		}

	}

	private String getPropertyValue(String value) {
		String subString = value.split(":")[1];
		int index = subString.indexOf(",");
		if(index > -1) {
			subString = subString.substring(0, index);
		}
		return subString;
	}

	private void handleRead(Read read) {
		synchronized (readMap) {
			readMap.put(read.getEpc(), read);
		}
		readerListener.onRead(read);
	}

	protected void handleOpList() {

		try {

			List<AdvanOp> opList;
			synchronized (AdvanReader.this) {
				if(this.opList.size() == 0) return;
				opList = new LinkedList<>(this.opList);
				this.opList.clear();
				singularizing = true;
			}

			_stopDevice(device, getSettings().getPort());

			try {
				Iterator<AdvanOp> it = opList.iterator();
				while (it.hasNext()) {
					AdvanOp advanOp = it.next();
					String epc = advanOp.getEpc();
					if (epc == null) {
						for (Read read : readMap.values()) {
							advanOp.perform(read, this, readerListener);
						}
					} else {
						Read read = readMap.get(advanOp.getEpc());
						if (read != null) {
							advanOp.perform(read, this, readerListener);
							it.remove();
						}
					}
				}
			} finally {
				readMap.clear();
				synchronized (AdvanReader.this) {
					if (opList.size() > 0) {
						AdvanReader.this.opList.addAll(opList);
					}
					singularizing = false;
					if (isReading()) {
						_startDevice(device, getSettings().getPort());
					}
				}
			}

		} catch (Exception ignore) {}

	}


	@Override
	public synchronized boolean isConnected() {
		return dataReader.isConnected();
	}

	@Override
	public void disconnect() {
		try {
			stop();
		} catch (Exception e) {
		} finally {
			dataReader.disconnect();
		}
	}

	protected Set<Device> getDevices(String host, int port) throws Exception {

		URL devicesURL = new URL("http", host, port, "/devices");

		String xmlFile = restUtils.sendGet(devicesURL);

		inputSource.setString(xmlFile);
		NodeList nodes = (NodeList) expressionVersion.evaluate(inputSource, XPathConstants.NODESET);
		String advanNetVersion = "";
		for (int i = 0; i < nodes.getLength(); i++) {
			advanNetVersion = nodes.item(i).getNodeValue();
		}
		Set<Device> devices = new HashSet<Device>();
		inputSource.restart();
		nodes = (NodeList) expressionDevice.evaluate(inputSource, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i += 3) {
			String id = nodes.item(i).getNodeValue();
			String serial = nodes.item(i + 1).getNodeValue();
			String family = nodes.item(i + 2).getNodeValue();
			devices.add(new Device(id, serial, family, host, advanNetVersion));
		}

		return devices;
	}

	protected Set<String> getDeviceModes(Device device, int port) throws Exception {

		URL readModesURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/deviceModes");
		String xmlFile = restUtils.sendGet(readModesURL);

		inputSource.setString(xmlFile);
		NodeList nodes = (NodeList) expressionReadModes.evaluate(inputSource, XPathConstants.NODESET);

		Set<String> results = new HashSet<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			String name = nodes.item(i).getNodeValue();
			results.add(name);
		}

		return results;		
	}

	protected String getActiveReadMode(Device device, int port) throws Exception {

		URL readModesURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/activeReadMode");
		String xmlFile = restUtils.sendGet(readModesURL);

		inputSource.setString(xmlFile);
		NodeList nodes = (NodeList) expressionResult.evaluate(inputSource, XPathConstants.NODESET);

		if (nodes.getLength() != 1)
			throw new RuntimeException("Error getting ActiveReadMode");

		return nodes.item(0).getNodeValue();
	}

	protected void setActiveDeviceMode(Device device, int port, String deviceMode) throws Exception {
		URL activeDeviceModeURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/activeDeviceMode");
		String xmlFileResponse = restUtils.sendPut(activeDeviceModeURL, deviceMode); // TODO : page 51
		/*
		 * <request><class>READMODE_AUTONOMOUS</class><name>AUTONOMOUS</name><useFastSearch>false</useFastSearch><swFilterOnly>false</swFilterOnly><filterMaskOffset>32</filterMaskOffset><filterMaskBitLength>0</filterMaskBitLength><filterMaskHex/><calibratorsRefreshPeriod>60</calibratorsRefreshPeriod><keepAllReads>false</keepAllReads><asynch>true</asynch><cleanBufferOnSynchRead>false</cleanBufferOnSynchRead><timeWindow>2500</timeWindow><onTime>600</onTime><offTime>0</offTime></request>
		 */
		if (xmlFileResponse.contains("ERROR"))
			throw new RuntimeException("Error, the device mode " + deviceMode + " was not able to be set");
	}

	protected void startDevice(Device device, int port) throws Exception {
		String xmlFileResponse = _startDevice(device, port);
		timestampStart = getTimestamp(xmlFileResponse);
	}

	String _startDevice(Device device, int port) throws Exception {
		URL startURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/start");
		String xmlFileResponse = restUtils.sendGet(startURL);
		if (xmlFileResponse.contains("ERROR"))
			throw new RuntimeException("Error, failed to start the device " + device.getId());
		return xmlFileResponse;
	}

	private long getTimestamp(String xmlFile) throws Exception {
		inputSource.setString(xmlFile);
		NodeList nodes = (NodeList) expressionTimestamp.evaluate(inputSource, XPathConstants.NODESET);
		if(nodes.getLength() > 0) {
			String result = nodes.item(0).getNodeValue();
			return Long.valueOf(result);
		}
		return -1;
	}

	protected void stopDevice(Device device, int port) throws Exception {
		timestampStart = Long.MAX_VALUE;
		_stopDevice(device, port);
	}

	void _stopDevice(Device device, int port) throws Exception {
		URL stopURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/stop");
		String xmlFileResponse = restUtils.sendGet(stopURL);

		if (xmlFileResponse.contains("ERROR"))
			throw new RuntimeException("Error, failed to stop the device " + device.getId());
	}

	protected boolean isGpi(Device device, int port, int gpiPort) throws Exception {
		URL gpiUrl = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/getGPI/" + gpiPort);
		String xml = restUtils.sendGet(gpiUrl);

		if (xml.contains("ERROR"))
			throw new RuntimeException(String.format("Error, getting gpi '%d' state", gpiPort));

		inputSource.setString(xml);
		NodeList nodes = (NodeList) expressionResult.evaluate(inputSource, XPathConstants.NODESET);

		return Boolean.parseBoolean(nodes.item(0).getNodeValue());
	}

	// In AdvanReader Series 50 and 100, the GPI operations will stop temporarily theRF operations.
	protected boolean[] getGpiAll(Device device, int port) throws Exception {
		URL gpiAllUrl = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/getGPIAll");
		String xml = restUtils.sendGet(gpiAllUrl);

		if (xml.contains("ERROR"))
			throw new RuntimeException("Error, getting GPIAll");

		inputSource.setString(xml);
		NodeList nodes = (NodeList) expressionGpiAll.evaluate(inputSource, XPathConstants.NODESET);

		Map<Integer, Boolean> results = new HashMap<>();
		for (int i = 0; i < nodes.getLength(); i += 2) {
			String index = nodes.item(i).getNodeValue();
			String state = nodes.item(i + 1).getNodeValue();
			results.put(Integer.parseInt(index), Boolean.parseBoolean(state));
		}
		boolean[] tmp = new boolean[results.size()];
		for(Entry<Integer, Boolean> entry : results.entrySet()) {
			tmp[entry.getKey()] = entry.getValue();
		}

		return tmp;	
	}

	protected void setGpo(Device device, int port, int portNumber, boolean state) throws Exception {
		URL setGpoURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/setGPO/" + portNumber + "/" + state);
		String xmlFileResponse = restUtils.sendGet(setGpoURL);

		if (xmlFileResponse.contains("ERROR"))
			throw new RuntimeException("Error, setting gpo state");
	}

	protected void setParameter(Device device, int port, String paramId, String paramValue) throws Exception {
		URL setParameterURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/reader/parameter/" + paramId);
		String xmlFileResponse = restUtils.sendPut(setParameterURL, paramValue);

		if (xmlFileResponse.contains("ERROR"))
			throw new RuntimeException(String.format("Error, setting parameter '%s' = '%s'", paramId, paramValue));
	}

	protected String getParameter(Device device, int port, String paramName) throws Exception {
		URL url = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/reader/parameter/" + paramName);
		String xmlFile = restUtils.sendGet(url);
		if (xmlFile.contains("ERROR"))
			throw new RuntimeException("Error, failed to setting antenna configuration");

		inputSource.setString(xmlFile);
		NodeList nodes = (NodeList) expressionResult.evaluate(inputSource, XPathConstants.NODESET);

		if(nodes.getLength() > 0) {
			String result = nodes.item(0).getNodeValue();
			return result;
		}
		return null;
	}

	protected int toInt(String value) {
		if(value != null) {
			return Integer.parseInt(value);
		}
		return 0;
	}

	protected void saveConfiguration(Device device, int port) throws Exception {
		URL saveConfigurationURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/confSave");
		String xmlFileResponse = restUtils.sendGet(saveConfigurationURL);
		if (xmlFileResponse.contains("ERROR"))
			throw new RuntimeException("Error, failed to setting write power");
	}

	protected void setWritePower(Device device, int port, double power) throws Exception {
		URL writePowerURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/reader/parameter/RF_WRITE_POWER/" + power);
		String xmlFileResponse = restUtils.sendGet(writePowerURL);
		if (xmlFileResponse.contains("ERROR"))
			throw new RuntimeException("Error, failed to setting write power");
	}

	protected void setAntennaConfiguration(Device device, int port, Settings settings) throws Exception {

		URL antennasURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/antennas");

		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		Document doc = docBuilder.newDocument();
		Element request = doc.createElement("request");
		doc.appendChild(request);

		Element entries = doc.createElement("entries");
		request.appendChild(entries);

		for (int i = 0; i < settings.getAntennas().length; ++i) {
			int antennaId = settings.getAntennas()[i];
			double txPower = settings.getTxPower()[i];
			double rxSensitivity = settings.getRxSensitivity()[i];
			Element antElement = getAntennaElement(antennaId, txPower, rxSensitivity, device, doc);
			entries.appendChild(antElement);
		}

		String antennaConfiguration = documentToXML(doc);
		// System.out.println(antennaConfiguration);
		String xmlFileResponse = restUtils.sendPut(antennasURL, antennaConfiguration);

		if (xmlFileResponse.contains("ERROR"))
			throw new RuntimeException("Error, failed to setting antenna configuration");

	}

	public String readDataByEpc(Device device, int port, String epc, int bank, int address, int lenght, String accessPassword) throws Exception {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document doc = docBuilder.newDocument();
		Element request = doc.createElement("request");
		doc.appendChild(request);

		Element writeDataElement = getReadDataOpElement(MemoryBank.values()[bank].name(), address, lenght, doc);
		request.appendChild(writeDataElement);

		Element paramsElement = getParamsOpElement(epc, 0, TAG_OP_TIMEOUT, RF_WRITE_RETRIES, accessPassword, doc);
		request.appendChild(paramsElement);

		String xml = documentToXML(doc);

		String xmlFileResponse = execOp(device, port, xml);
		return expressionResult.evaluate(new org.xml.sax.InputSource(new StringReader(xmlFileResponse)));
	}

	public void writeData(Device device, int port, String epc, String data, int bank, int address, String accessPassword) throws Exception {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document doc = docBuilder.newDocument();
		Element request = doc.createElement("request");
		doc.appendChild(request);

		Element writeDataElement = getWriteDataOpElement(MemoryBank.values()[bank].name(), address, data, doc);
		request.appendChild(writeDataElement);

		Element paramsElement = getParamsOpElement(epc, 0, TAG_OP_TIMEOUT, RF_WRITE_RETRIES, accessPassword, doc);
		request.appendChild(paramsElement);

		String xml = documentToXML(doc);

		execOp(device, port, xml);
	}

	public void lockTag(Device device, int port, String epc, String accessPassword, String newAccessPassword, String newKillPassword, LockOptions lockOptions) throws Exception {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		if(accessPassword == null) accessPassword = "";
		if(newAccessPassword != null || newKillPassword != null) {			
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element request = doc.createElement("request");
			doc.appendChild(request);

			Element CommissioningElement = getCommissioningOpElement(epc, accessPassword, newAccessPassword, newKillPassword, doc);
			request.appendChild(CommissioningElement);

			Element paramsElement = getParamsOpElement(epc, 0, TAG_OP_TIMEOUT, RF_WRITE_RETRIES, null, doc); // no requiere password
			request.appendChild(paramsElement);

			String xml = documentToXML(doc);

			execOp(device, port, xml);
		}

		if(lockOptions != null) {			
			if(newAccessPassword != null) // Si se ha cambiado el password
				accessPassword = newAccessPassword;

			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element request = doc.createElement("request");
			doc.appendChild(request);

			Element lockTagElement = getLockTagOpElement(accessPassword, lockOptions, doc);
			request.appendChild(lockTagElement);

			Element paramsElement = getParamsOpElement(epc, 0, TAG_OP_TIMEOUT, RF_WRITE_RETRIES, null, doc);
			request.appendChild(paramsElement);

			String xml = documentToXML(doc);

			execOp(device, port, xml);
		}

	}

	private Element getCommissioningOpElement(String epc, String accessPassword, String newAccessPassword, String newKillPassword, Document doc) {

		Element op = doc.createElement("op");
		Element opClass = doc.createElement("class");
		opClass.appendChild(doc.createTextNode("com.keonn.spec.reader.op.CommissionTagOp"));
		op.appendChild(opClass);

		Element opBank = doc.createElement("accessPwd");
		opBank.appendChild(doc.createTextNode(accessPassword));
		op.appendChild(opBank);

		if(epc != null) { // new epc
			Element opEpc = doc.createElement("epc");
			opEpc.appendChild(doc.createTextNode(epc));
			op.appendChild(opEpc);
		}

		if (newAccessPassword != null) { // new access password
			Element opNewPass = doc.createElement("newAccessPwd");
			opNewPass.appendChild(doc.createTextNode(newAccessPassword));
			op.appendChild(opNewPass);
		}

		if (newKillPassword != null) { // new kill password
			Element opLocks = doc.createElement("newKillPwd");
			opLocks.appendChild(doc.createTextNode(newKillPassword));
			op.appendChild(opLocks);
		}

		return op;
	}

	private Element getAntennaElement(int antennaId, double txPower, double rxSensitivity, Device device, Document doc) throws Exception {

		Element entry = doc.createElement("entry");

		Element antennaClass = doc.createElement("class");
		antennaClass.appendChild(doc.createTextNode("ANTENNA_DEFINITION"));
		entry.appendChild(antennaClass);

		Element cid = doc.createElement("def");
		// TODO : future 
		int mux1 = 0, mux2 = 0;
		int direction = -1;
		String location = "antenna_" + antennaId;
		int lx = 0, ly = 0, lz = 0;

		String def = String.format("%s,%d,%d,%d,%d,%s,%d,%d,%d", device.getId(), antennaId, mux1, mux2, direction, location, lx, ly, lz);
		cid.appendChild(doc.createTextNode(def));
		entry.appendChild(cid);

		Element conf = doc.createElement("conf");
		entry.appendChild(conf);

		Element confClass = doc.createElement("class");
		confClass.appendChild(doc.createTextNode("ANTENNA_CONF"));
		conf.appendChild(confClass);

		Element power = doc.createElement("power");
		if (txPower != 0) {
			power.appendChild(doc.createTextNode("" + txPower));
		}
		conf.appendChild(power);

		Element sensitivity = doc.createElement("sensitivity");
		if (rxSensitivity != 0) {
			sensitivity.appendChild(doc.createTextNode("" + rxSensitivity));
		}
		conf.appendChild(sensitivity);

		Element readTime = doc.createElement("readTime");
		conf.appendChild(readTime);

		return entry;
	}

	private static String documentToXML(Document document) throws Exception {

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		// transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		StringWriter sw = new StringWriter();
		DOMSource source = new DOMSource(document.getDocumentElement());
		transformer.transform(source, new StreamResult(sw));

		return sw.toString();
	}

	private Element getLockTagOpElement(String accessPassword, LockOptions lockOptions, Document doc) {
		Element op = doc.createElement("op");
		Element opClass = doc.createElement("class");
		opClass.appendChild(doc.createTextNode("com.keonn.spec.reader.op.LockOp"));
		op.appendChild(opClass);

		Element opBank = doc.createElement("accessPwd");
		opBank.appendChild(doc.createTextNode("0x" + accessPassword));
		op.appendChild(opBank);

		Element opOffset = doc.createElement("mask");
		opOffset.appendChild(doc.createTextNode(Integer.toString(0)));
		op.appendChild(opOffset);

		Element opData = doc.createElement("action");
		opData.appendChild(doc.createTextNode(Integer.toString(0)));
		op.appendChild(opData);

		Element opLocks = doc.createElement("locks");
		opLocks.appendChild(doc.createTextNode(toString(lockOptions)));
		op.appendChild(opLocks);

		return op;
	}

	private String toString(LockOptions lockOptions) {
		StringBuilder sb = new StringBuilder();
		getOption("ACCESS_", lockOptions.getAccessPasswordLockType(), sb);
		getOption("KILL_", lockOptions.getKillPasswordLockType(), sb);
		getOption("EPC_", lockOptions.getEpcLockType(), sb);
		getOption("TID_", lockOptions.getTidLockType(), sb);
		getOption("USER_", lockOptions.getUserLockType(), sb);
		return sb.toString();
	}

	private void getOption(String prefix, int type, StringBuilder sb) {
		switch (type) {
		case LockOptions.LOCK:
			sb.append(prefix).append("LOCK");
			break;
		case LockOptions.UNLOCK:
			sb.append(prefix).append("UNLOCK");
			break;
		case LockOptions.PERMA_LOCK:
			sb.append(prefix).append("PERMALOCK");
			break;
		case LockOptions.PERMA_UNLOCK:
			sb.append(prefix).append("PERMAUNLOCK");
			break;
		default:
			break;
		}		
	}

	private Element getReadDataOpElement(String bank, int offset, int lenght, Document doc)  throws Exception {
		Element op = doc.createElement("op");
		Element opClass = doc.createElement("class");
		opClass.appendChild(doc.createTextNode("com.keonn.spec.reader.op.ReadDataOp"));
		op.appendChild(opClass);

		Element opBank = doc.createElement("bank");
		opBank.appendChild(doc.createTextNode(bank));
		op.appendChild(opBank);

		Element opOffset = doc.createElement("offset");
		opOffset.appendChild(doc.createTextNode(Integer.toString(offset)));
		op.appendChild(opOffset);

		Element opData = doc.createElement("length");
		opData.appendChild(doc.createTextNode(Integer.toString(lenght)));
		op.appendChild(opData);

		return op;
	}

	private Element getWriteDataOpElement(String bank, int offset, String data, Document doc) throws Exception {

		Element op = doc.createElement("op");
		Element opClass = doc.createElement("class");
		opClass.appendChild(doc.createTextNode("com.keonn.spec.reader.op.WriteDataOp"));
		op.appendChild(opClass);

		Element opBank = doc.createElement("bank");
		opBank.appendChild(doc.createTextNode(bank));
		op.appendChild(opBank);

		Element opOffset = doc.createElement("offset");
		opOffset.appendChild(doc.createTextNode(Integer.toString(offset)));
		op.appendChild(opOffset);

		//		Element opLenght = doc.createElement("length");
		//		opLenght.appendChild(doc.createTextNode(Integer.toString(data.length())));
		//		op.appendChild(opLenght);

		Element opData = doc.createElement("data");
		opData.appendChild(doc.createTextNode(data));
		op.appendChild(opData);

		return op;
	}

	private Element getParamsOpElement(String epc, int antenna, long opTimeout, int retries, String accessPassword, Document doc) throws Exception {

		Element params = doc.createElement("params");
		if (epc != null && !epc.trim().isEmpty()) {
			Element param = doc.createElement("param");

			Element paramId = doc.createElement("id");
			paramId.appendChild(doc.createTextNode("GEN2_FILTER"));
			param.appendChild(paramId);

			Element paramObj = doc.createElement("obj");
			Element objClass = doc.createElement("class");
			objClass.appendChild(doc.createTextNode("com.keonn.spec.filter.SelectTagFilter"));
			paramObj.appendChild(objClass);

			Element objBank = doc.createElement("bank");
			objBank.appendChild(doc.createTextNode("EPC"));
			paramObj.appendChild(objBank);

			Element objBitPointer = doc.createElement("bitPointer");
			objBitPointer.appendChild(doc.createTextNode(Integer.toString(32)));
			paramObj.appendChild(objBitPointer);

			Element objBitLength = doc.createElement("bitLength");
			objBitLength.appendChild(doc.createTextNode(Integer.toString(epc.length() << 2)));
			paramObj.appendChild(objBitLength);

			Element objMask = doc.createElement("mask");
			objMask.appendChild(doc.createTextNode(epc));
			paramObj.appendChild(objMask);

			param.appendChild(paramObj);

			params.appendChild(param);
		}
		if (antenna > 0) {
			Element param = doc.createElement("param");

			Element paramId = doc.createElement("id");
			paramId.appendChild(doc.createTextNode("TAG_OP_ANTENNA"));
			param.appendChild(paramId);

			Element paramObj = doc.createElement("obj");
			paramObj.appendChild(doc.createTextNode(Integer.toString(antenna)));
			param.appendChild(paramObj);

			params.appendChild(param);
		}
		if(opTimeout > 0) {
			Element param = doc.createElement("param");

			Element paramId = doc.createElement("id");
			paramId.appendChild(doc.createTextNode("TAG_OP_TIMEOUT"));
			param.appendChild(paramId);

			Element paramObj = doc.createElement("obj");
			paramObj.appendChild(doc.createTextNode(Long.toString(opTimeout)));
			param.appendChild(paramObj);

			params.appendChild(param);
		}
		if(retries > 0) {
			Element param = doc.createElement("param");

			Element paramId = doc.createElement("id");
			paramId.appendChild(doc.createTextNode("RF_WRITE_RETRIES"));
			param.appendChild(paramId);

			Element paramObj = doc.createElement("obj");
			paramObj.appendChild(doc.createTextNode(Integer.toString(retries)));
			param.appendChild(paramObj);

			params.appendChild(param);
		}
		if (accessPassword != null && !accessPassword.trim().isEmpty()) {
			Element param = doc.createElement("param");

			Element paramId = doc.createElement("id");
			paramId.appendChild(doc.createTextNode("GEN2_ACCESS_PASSWORD"));
			param.appendChild(paramId);

			Element paramObj = doc.createElement("obj");
			paramObj.appendChild(doc.createTextNode(accessPassword));
			param.appendChild(paramObj);

			params.appendChild(param);
		}
		return params;
	}

	private String execOp(Device device, int port, String opData) throws Exception {
		URL execOpURL = new URL("http", device.getHost(), port, "/devices/" + device.getId() + "/execOp");
		String xmlFileResponse = restUtils.sendPut(execOpURL, opData);
		if (xmlFileResponse.contains("ERROR")) {
			throw new ReaderException(getMessageError(xmlFileResponse));
		}
		return xmlFileResponse; // response
	}

	private String getMessageError(String xmlFile) throws XPathExpressionException {
		inputSource.setString(xmlFile);
		NodeList nodes = (NodeList) expressionError.evaluate(inputSource, XPathConstants.NODESET);
		if(nodes.getLength() > 0) {
			String result = nodes.item(0).getNodeValue();
			return result;
		}
		return "";
	}

	@Override
	public void setReaderListener(ReaderListener readerListener) {
		this.readerListener = readerListener;
	}

	@Override
	public ReaderListener getReaderListener() {
		return readerListener;
	}

	public void addOp(AdvanOp op) throws ReaderException {
		synchronized (AdvanReader.this) {
			opList.add(op);
		}
		startRead();
	}

	public Device getDevice() {
		return device;
	}

	@Override
	public synchronized void startRead() throws ReaderException {
		if(!reading && !singularizing) {
			try {		
				int port = settings.getPort();
				String readMode = getActiveReadMode(device, port);
				if(!Device.ReadMode.AUTONOMOUS.name().equals(readMode)) {
					setActiveDeviceMode(device, port, "Autonomous");
				}
				startDevice(device, port);
				reading = true;
			} catch (Exception e) {
				throw new ReaderException("Error starting read", e);
			}
		}
	}

	@Override
	public synchronized boolean isReading() {
		return reading;
	}

	@Override
	public synchronized void stop() throws ReaderException {		
		if(reading) {
			try {			
				reading = false;
				singularizing = false;
				int port = settings.getPort();			
				stopDevice(device, port);			
			} catch (Exception e) {
				throw new ReaderException("Error stopping read", e);
			}		
		}
	}

	@Override
	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	@Override
	public Settings getSettings() {
		return settings;
	}

	@Override
	public void setGpo(int portNumber, boolean state) throws ReaderException {
		try {
			int port = settings.getPort();
			setGpo(device, port, portNumber, state);
		} catch (Exception e) {
			throw new ReaderException("Error setting gpo state", e);
		}
	}

	@Override
	public void setGpo(Map<Integer, Boolean> stateMap) throws ReaderException {
		try {
			int port = settings.getPort();
			for (Entry<Integer, Boolean> entry : stateMap.entrySet()) {
				setGpo(device, port, entry.getKey(), entry.getValue());
			}
		} catch (Exception e) {
			throw new ReaderException("Error setting gpo states", e);
		}
	}

	@Override
	public void setGpo(boolean[] state) throws ReaderException {
		try {
			int port = settings.getPort();
			for (int i = 0; i < state.length; ++i) {
				setGpo(device, port, i + 1, state[i]);
			}
		} catch (Exception e) {
			throw new ReaderException("Error setting gpo states", e);
		}
	}

	@Override
	public boolean isGpi(int portNumber) throws ReaderException {		
		try {
			int port = settings.getPort();
			return isGpi(device, port, portNumber);
		} catch (Exception e) {
			throw new ReaderException("Error getting gpi state", e);
		}				
	}

	@Override
	public boolean[] getGpiState() throws ReaderException {
		try {
			int port = settings.getPort();
			return getGpiAll(device, port);
		} catch (Exception e) {
			throw new ReaderException("Error getting gpi states", e);
		}
	}

	@Override
	public String getSerial() {
		return device.getSerial();
	}

	@Override
	public String getModelName() {
		return device.getFamily();
	}

	@Override
	public void readData(int bank, int address, int lenght) throws ReaderException {
		readData(bank, address, lenght, null);
	}

	@Override
	public void readData(int bank, int address, int lenght, String accessPassword) throws ReaderException {
		ReadDataOp readDataOp = new ReadDataOp();
		readDataOp.setAccessPassword(accessPassword);
		readDataOp.setBank(bank);
		readDataOp.setAddress(address);
		readDataOp.setLenght(lenght);
		addOp(readDataOp);
	}

	@Override
	public void readData(String epc, int bank, int address, int lenght) throws ReaderException {
		readData(epc, bank, address, lenght, null);
	}

	@Override
	public void readData(String epc, int bank, int address, int lenght, String accessPassword) throws ReaderException {
		ReadDataByEpcOp readDataByEpcOp = new ReadDataByEpcOp();
		readDataByEpcOp.setAccessPassword(accessPassword);
		readDataByEpcOp.setBank(bank);
		readDataByEpcOp.setAddress(address);
		readDataByEpcOp.setLenght(lenght);
		readDataByEpcOp.setEpc(epc);
		addOp(readDataByEpcOp);
	}

	@Override
	public void setKillPassword(String epc, String killPassword) throws ReaderException {
		setKillPassword(epc, null, killPassword);
	}

	@Override
	public void setKillPassword(String epc, String accessPassword, String killPassword) throws ReaderException {
		SetKillPwdOp setKillPwdOp = new SetKillPwdOp();
		setKillPwdOp.setEpc(epc);
		setKillPwdOp.setAccessPassword(accessPassword);
		setKillPwdOp.setKillPassword(killPassword);
		addOp(setKillPwdOp);
	}

	@Override
	public void lockTag(String epc, String accessPassword, LockOptions lockOptions) throws ReaderException {
		lockTag(epc, null, accessPassword, lockOptions);
	}

	@Override
	public void lockTag(String epc, String oldAccessPassword, String newAccessPassword, LockOptions lockOptions) throws ReaderException {
		LockTagOp lockTagOp = new LockTagOp();
		lockTagOp.setEpc(epc);
		lockTagOp.setOldAccessPassword(oldAccessPassword);
		lockTagOp.setNewAccessPassword(newAccessPassword);
		lockTagOp.setLockOptions(lockOptions);
		addOp(lockTagOp);
	}

	@Override
	public void writeEpc(String srcEpc, String tgtEpc) throws ReaderException {
		writeEpc(srcEpc, tgtEpc, null);
	}

	@Override
	public void writeEpc(String srcEpc, String tgtEpc, String accessPassword) throws ReaderException {
		int srcEpcLen = srcEpc.length();
		int tgtEpcLen = tgtEpc.length();
		if ((tgtEpcLen & 3) != 0 || (srcEpcLen & 3) != 0) {
			throw new ReaderException("EPCs must be a multiple of 16-bits: " + srcEpc + ", " + tgtEpc);
		}

		short wordPointer = 2;
		if(srcEpcLen != tgtEpcLen) {
			wordPointer = 1;

			short currentPC = (short) ((srcEpc.length() >> 2) << 11);
			// keep other PC bits the same.
			short newPC = (short) ((currentPC & 0x7FF) | (short) ((tgtEpc.length() >> 2) << 11));
			String newPCString = Utils.toHexString(newPC);

			tgtEpc = newPCString + tgtEpc;

		}

		writeData(srcEpc, tgtEpc, 1, wordPointer, accessPassword);
		//		ChangeEpcOp changeEpcOp = new ChangeEpcOp();		
		//		changeEpcOp.setEpc(srcEpc);
		//		changeEpcOp.setNewEpc(tgtEpc);
		//		changeEpcOp.setAccessPassword(accessPassword);
		//		addOp(changeEpcOp);
	}

	@Override
	public void writeData(String epc, String data, int memoryBank, short wordPointer) throws ReaderException {
		this.writeData(epc, data, memoryBank, wordPointer, null);
	}

	@Override
	public void writeData(String epc, String data, int memoryBank, short wordPointer, String accessPassword) throws ReaderException {
		WriteDataOp writeDataOp = new WriteDataOp();
		writeDataOp.setEpc(epc);
		writeDataOp.setData(data);
		writeDataOp.setAccessPassword(accessPassword);
		writeDataOp.setMemoryBank(memoryBank);
		writeDataOp.setWordPointer(wordPointer);
		addOp(writeDataOp);
	}

	@Override
	public int getGpoCount() {
		return gpoCount;
	}

	@Override
	public int getGpiCount() {
		return gpiCount;
	}

}
