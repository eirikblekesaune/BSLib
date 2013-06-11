MotorMixEnvir {
	var mixer;
	var <envir;
	var <buttonTypes;
	var <encoderDeltaRange, <encoderRange;
    var <syncFunctions;

	classvar <protoEvent;

	*initClass{
		protoEvent = ();
		8.do {|i|
			[\fader, \touch, \lhs, \rhs, \select, \encoder, \mute, \solo, \multi_sw,
			\burn_sw, \multi, \burn].do({|jtem|
				protoEvent.put((jtem ++ "_" ++ i).asSymbol, 0);
			});
			protoEvent.put(\rotSwitch, 0);
			protoEvent.put(\rotButton, 0);
		};
	}

	*new{arg mixer;
		^super.new.init(mixer);
	}

	init{arg argMixer;
		mixer = argMixer;
		envir = this.class.protoEvent;
		buttonTypes = ();
		mixer.faderAction = {|num, value|
			var key;
			key = ("fader_" ++ num).asSymbol;
			envir.put(key, value);
			this.changed(key);
		};
		mixer.buttonAction = {|name, num, value|
			var key, type;
			key = (name ++ "_" ++ num).asSymbol;

			type = buttonTypes.at(key) ? \mom;
			switch(type,
				{\toggle}, {
					if(value == 1, {
						var currentValue = envir.at(key);
						value = (currentValue + 1) % 2;
						this.put(key, value);
						this.changed(key);
					});
				},
				{\mom}, {
					this.put(key, value);
					this.changed(key);
                }
			);
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
                {|mx| mx.setMotorPosition(i, envir.at(key))}
            );
        });
	}

    put{arg key, val;
        envir.put(key, val);
        this.dosync(key);
    }

    at{arg key;
        envir.at(key);
    }

    //perform sync function for specific key
    dosync{arg key;
        syncFunctions.at(key).value(mixer);
    }

    setSyncFunction{arg key, func;
        syncFunctions.put(key, func);
    }

	setButtonType{arg key, type, value = 0;
		if(protoEvent.includesKey(key.asSymbol), {
			//check type
			buttonTypes.put(key.asSymbol, type);
		});
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

	refreshMixer{

	}
}