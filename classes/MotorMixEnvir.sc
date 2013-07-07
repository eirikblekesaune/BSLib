MotorMixEnvir {
	var mixer;
	var <buttonTypes;
	var <envir;
	var <syncFunctions;
	var <dispatchFunctions;
	var encoderSpec, encoderDelta;

	*new{
		^super.new.init();
	}

	init{
		envir = ();
		buttonTypes = ();
		dispatchFunctions = ();
		8.do {|i|
			[\fader, \touch, \lhs, \rhs, \select, \encoder, \mute, \solo, \multi_sw,
				\burn_sw, \multi, \burn].do({|jtem|
				var buttonKey = (jtem ++ "." ++ (i + 1)).asSymbol;
				envir.put(buttonKey, 0);
				buttonTypes.put(buttonKey, \toggle);
			});
			envir.put(\rotSwitch, 0);
			envir.put(\rotButton, 0);
		};
		encoderSpec = [0.0, 1.0].asSpec;
		encoderDelta = [0.01, 0.1].asSpec;

		//Set up sync functions
		syncFunctions = ();
		[\lhs, \rhs, \solo, \mute, \select, \burn_sw, \multi_sw].do({|item|
			8.do({|j|
				var key = (item ++ "." ++ (j + 1)).asSymbol;
				syncFunctions.put(key,
					{|mx| //mixer and value are sent as args
						mx.setLEDState(item, j, envir.at(key));
					}
				);
			});
		});
		8.do({|i|
			var key = ("fader." ++ (i + 1)).asSymbol;
			syncFunctions.put(key,
				{|mx| mx.setMotorPosition(i, (envir.at(key) * 512).asInteger)}
			);
		});
		//		setEncoderDisplay
		8.do({|i|
			var key = ("encoder." ++ (i + 1)).asSymbol;
			syncFunctions.put(key,
				{|mx| mx.setEncoderDisplay(i, envir.at(key) * 127.0)}
			);
		});

	}

	attachMotorMix{arg mixer_;
		if(mixer.notNil, {this.detatchMotorMix(mixer)});
		mixer = mixer_;
		mixer.faderAction_(this.prFaderAction);
		mixer.buttonAction_(this.prButtonAction);
		mixer.encoderAction_(this.prEncoderAction);
		this.refreshMixer;
	}

	detatchMotorMix{
		if(mixer.notNil, {
			mixer.faderAction_(nil);
			mixer.buttonAction_(nil);
			mixer.encoderAction_(nil);
			mixer.clearLCD;
			mixer = nil;
		});
	}

	refreshMixer{
		if(mixer.notNil,{
			envir.keysValuesDo({|key, val|
				this.sync(key);
			});
		});
	}

	put{arg key, val;
		envir.put(key, val);
		this.sync(key);
	}

	at{arg key;
		^envir.at(key);
	}

	//perform sync function for specific key
	sync{arg key;
		mixer !? { syncFunctions.at(key).value(mixer); };
	}

	setSyncFunction{arg key, func;
		syncFunctions.put(key, func);
	}

	setButtonType{arg key, type;
		buttonTypes.put(key.asSymbol, type);
	}

	setAction{arg key, func;
		dispatchFunctions.put(key, func);
	}

	removeAction{arg key;
		dispatchFunctions.removeAt;
		^this;
	}

	// setEncoderDeltaRange{arg num, minval, maxval;
	// 	encoderDeltaRange.put(num, (min: minval, max: maxval));
	// }

	// setEncoderRange{arg num, minval, maxval;
	// 	var key, prevValue, value;
	// 	key = ("encoder." ++ num).asSymbol;
	// 	encoderRange.put(num, (min: minval, max: maxval));
	// 	//clip value
	// 	prevValue = envir.at(key);
	// 	value = prevValue.clip(minval, maxval);
	// 	if(prevValue != value, {
	// 		envir.put(key, value);
	// 		this.changed(key);
	// 	});
	// }

	prFaderAction{
		^{|num, value|
			var key;
			key = ("fader." ++ num).asSymbol;
			envir.put(key, value / 512.0);
			this.changed(mixer, key, value);
			dispatchFunctions.at(key).value(this.at(key));
		}
	}

	prButtonAction{
		^{|name, num, value|
			var key, type;
			key = (name ++ "." ++ num).asSymbol;

			type = buttonTypes.at(key) ? \toggle;
			switch(type,
				\toggle, {
					if(value == 1, {
						var currentValue = envir.at(key);
						value = (currentValue + 1) % 2;
						this.put(key, value);
						this.changed(mixer, key, value);
					});
				},
				\mom, {
					this.put(key, value);
					this.changed(mixer, key, value);
				}
			);
			dispatchFunctions.at(key).value(this.at(key));
		};
	}

	prEncoderAction{
		^{|num, value|
			var key, currentValue, delta, min, max;
			var deltaMin, deltaMax;
			key = ("encoder." ++ num).asSymbol;
			currentValue = envir.at(key);
			delta = if(value > 64,
				{value.linlin(64, 127, encoderDelta.minval, encoderDelta.maxval)},
				{value.linlin(0, 63, encoderDelta.minval.neg, encoderDelta.maxval.neg)}
			);
			value = encoderSpec.map(currentValue + delta);
			this.put(key, value);
			"Encoder %\n".postf(value);
			dispatchFunctions.at(key).value(this.at(key));
			this.changed(key, value);
		};
	}
}