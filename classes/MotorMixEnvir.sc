MotorMixEnvir {
	var mixer;
	var <buttonTypes;
	var <envir;
	var <encoderDeltaRange, <encoderRange;
	var <syncFunctions;

	*new{
		^super.new.init();
	}

	init{
		envir = ();
		buttonTypes = ();
		8.do {|i|
			[\fader, \touch, \lhs, \rhs, \select, \encoder, \mute, \solo, \multi_sw,
				\burn_sw, \multi, \burn].do({|jtem|
				var buttonKey = (jtem ++ "_" ++ i).asSymbol;
				envir.put(buttonKey, 0);
				buttonTypes.put(buttonKey, \toggle);
			});
			envir.put(\rotSwitch, 0);
			envir.put(\rotButton, 0);
		};
		// encoderDeltaRange = (min: 0.001, max: 0.1) ! 8;
		// encoderRange = (min: 0.0, max: 1.0) ! 8;
		// mixer.encoderAction = {|num, value|
		//     var key, currentValue, delta, min, max;
		//     var deltaMin, deltaMax;
		//     key = ("encoder_" ++ num).asSymbol;
		//     currentValue = envir.at(key);
		//     deltaMin = encoderDeltaRange.at(\min);
		//     deltaMax = encoderDeltaRange.at(\max);
		//     delta = if(value > 64,
		//         {value.linlin(64, 127, deltaMin, deltaMax)},
		//         {value.linlin(0, 63, deltaMin, deltaMax.neg)}
		//     );
		//     value = currentValue + delta;
		//     min = encoderRange.at(\min);
		//     max = encoderRange.at(\max);
		//     envir.put(key, value.clip(min, max));
		//     this.changed(key);
		// };

		//Set up sync functions
		syncFunctions = ();
		[\lhs, \rhs, \solo, \mute, \select, \burn_sw, \multi_sw].do({|item|
			8.do({|j|
				var key = (item ++ "_" ++ j).asSymbol;
				syncFunctions.put(key,
					{|mx| //mixer and value are sent as args
						mx.setLEDState(item, j, envir.at(key));
					}
				);
			});
		});
		8.do({|i|
			var key = ("fader_" ++ i).asSymbol;
			syncFunctions.put(key,
				{|mx| mx.setMotorPosition(i, (envir.at(key) * 512).asInteger)}
			);
		});
	}

	attachMotorMix{arg mixer_;
		if(mixer.notNil, {this.detatchMotorMix(mixer)});
		mixer = mixer_;
		mixer.faderAction_(this.prFaderAction);
		mixer.buttonAction_(this.prButtonAction);
		this.refreshMixer;
	}

	detatchMotorMix{
		if(mixer.notNil, {
			mixer.faderAction_(nil);
			mixer.buttonAction_(nil);
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
		envir.at(key);
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

	setEncoderDeltaRange{arg num, minval, maxval;
		encoderDeltaRange.put(num, (min: minval, max: maxval));
	}

	setEncoderRange{arg num, minval, maxval;
		var key, prevValue, value;
		key = ("encoder_" ++ num).asSymbol;
		encoderRange.put(num, (min: minval, max: maxval));
		//clip value
		prevValue = envir.at(key);
		value = prevValue.clip(minval, maxval);
		if(prevValue != value, {
			envir.put(key, value);
			this.changed(key);
		});
	}

	prFaderAction{
		^{|num, value|
			var key;
			key = ("fader_" ++ num).asSymbol;
			envir.put(key, value / 512.0);
			this.changed(mixer, key, value);
		}
	}

	prButtonAction{
		^{|name, num, value|
			var key, type;
			key = (name ++ "_" ++ num).asSymbol;

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
		};
	}
}