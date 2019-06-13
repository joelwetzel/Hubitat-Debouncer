/**
 *  Presence Debouncer Instance
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.json.*
	
definition(
	parent: "joelwetzel:Debouncer",
    name: "Presence Debouncer Instance",
    namespace: "joelwetzel",
    author: "Joel Wetzel",
    description: "Child app that is instantiated by the Debouncer app.  It creates the binding between the physical presence sensor and the virtual debounced presence sensor.",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")


def wrappedLock = [
	name:				"wrappedSensor",
	type:				"capability.presenceSensor",
	title:				"Wrapped Presence Sensor",
	description:		"Select the presence sensor to debounce.",
	multiple:			false,
	required:			true
]


def debounceTime = [
	name:				"debounceTime",
	type:				"number",
	title:				"The delay time of the debouncing algorithm in seconds.",
	defaultValue:		2,
	required:			true
]


preferences {
	page(name: "mainPage", title: "", install: true, uninstall: true) {
		section(getFormat("title", "Presence Debouncer Instance")) {
		}
		section("") {
			input wrappedLock
			input debounceTime
			input autoRefreshOption
			input (
				type: "bool",
				name: "enableDebugLogging",
				title: "Enable Debug Logging?",
				required: true,
				defaultValue: false
			)
		}
	}
}


def installed() {
	log.info "Installed with settings: ${settings}"

	addChildDevice("joelwetzel", "Enhanced Virtual Presence Sensor", "Debounced-${wrappedSensor.displayName}", null, [name: "Debounced-${wrappedSensor.displayName}", label: "Debounced ${wrappedSensor.displayName}", completedSetup: true, isComponent: true])
	
	initialize()
}


def uninstalled() {
    childDevices.each {
		log.info "Deleting child device: ${it.displayName}"
		deleteChildDevice(it.deviceNetworkId)
	}
}


def updated() {
	log.info "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


def initialize() {
//	def debouncedSensor = getChildDevice("Debounced-${wrappedSensor.displayName}")
	
	subscribe(wrappedSensor, "presence", wrappedSensorHandler)

	// Generate a label for this child app
	app.updateLabel("Debounced ${wrappedSensor.displayName}")
	
	// Make sure the Debounced state matches the WrappedSensor upon initialization.
	wrappedSensorHandler(null)
}


def wrappedSensorHandler(evt) {
    runIn(debounceTime, wrappedSensorDelayedHandler);
}


def wrappedSensorDelayedHandler() {
	def debouncedSensor = getChildDevice("Debounced-${wrappedSensor.displayName}")

	if (wrappedSensor.currentValue("presence") == "present" && debouncedSensor.currentValue("presence") != "present") {
		log "${wrappedSensor.displayName}:present detected"
		log "${debouncedSensor.displayName}:setting present"
		//reliableLock.markAsLocked()
        debouncedSensor.arrived()
	}
	else if (wrappedSensor.currentValue("presence") == "not present" && debouncedSensor.currentValue("presence") != "not present") {
		log "${wrappedSensor.displayName}:notPresent detected"
		log "${debouncedSensor.displayName}:setting notPresent"
		//reliableLock.markAsUnlocked()
        debouncedSensor.departed()
	}
}


def getFormat(type, myText=""){
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def log(msg) {
	if (enableDebugLogging) {
		log.debug(msg)	
	}
}





