
XBeeAPI {
	var <serialPort, parseRoutine;
	var <>rxAction, <frameID;
	
	classvar startDelimiter = 0x7E;
	//classvar frameTypes = IdentityDictionary[\144, \rxReceived];
	
	*new {arg argSerialPort;
		^super.new.init(argSerialPort);
	}
	
	init{ arg argSerialPort;
		serialPort = argSerialPort;
		frameID = 1;
		"XBeeAPI initialized".postln;
	}
	
	getFrameID {
		frameID = (frameID % 254) + 1;
		^frameID;	
	}
	
	parseRXFrame { arg length;
		var serialAddr = "", networkAddr, receiveOption, data;
		8.do { serialAddr = serialAddr ++ serialPort.read.asHexString(2) };
		networkAddr = ( serialPort.read << 8).bitOr( serialPort.read );
		receiveOption = serialPort.read;
		data = {serialPort.read} ! (length - 12);
		rxAction.value(serialAddr, networkAddr, receiveOption, data);
	}
	
	sendTXFrame { arg serialAddressArray, networkAddressArray, payload, options = 0;
		var frame, lengthHi, lengthLo, checksum, payloadLength, temp_frameID;
		//sending without network address significantly decreases speed
		networkAddressArray = networkAddressArray ? [0xFF, 0xFE];
		payloadLength = payload.size;
		lengthHi = (payloadLength + 14) >> 8;//only payload length varies
		lengthLo = (payloadLength + 14).bitAnd(0xFF);
		temp_frameID = this.getFrameID;
		checksum = 0xFF - ([networkAddressArray, serialAddressArray, 
					payload, 0x10, temp_frameID, options].flatten.sum.bitAnd(0xFF));
		frame = [0x7E, //start delimiter
				lengthHi,
				lengthLo, 
				0x10, //tx request frame type
				temp_frameID,
				serialAddressArray,
				networkAddressArray,
				0x00, //radius
				options, //options
				payload, 
				checksum
		].flatten;
		//"frame: %\n".postf(frame);
		//"verify %\n".postf([networkAddressArray, serialAddressArray, 
		//			payload, 0x10, temp_frameID, checksum].flatten.sum.asHexString);
		serialPort.putAll(frame);
	}
	
	start{
		if((parseRoutine.isPlaying.not) or: (parseRoutine.isNil)) {
			parseRoutine = Routine({
				var byte, length, frameType;
				inf.do {
					byte = serialPort.read;	
					if(byte == 126) {
						length = (serialPort.read << 8).bitOr(serialPort.read);
						frameType = serialPort.read;
						switch(frameType, 
							144, { this.parseRXFrame(length); }//0x90
						);
					}
				}	
			}).play;
		} 
		{
			"parse routine is already playing!".postln;
		}
		
	}
	
	stop {
		if((parseRoutine.notNil) and: (parseRoutine.isPlaying)) {
			parseRoutine.stop;	
		}	
	}
}