/*
 *  Copyright 2016 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */
metadata {
	// Automatically generated. Make future change here.
	definition(name: "SmartPower Outlet + Graph", namespace: "bswenson", author: "Brian Swenson", ocfDeviceType: "oic.d.smartplug") {
		capability "Actuator"
		capability "Switch"
		capability "Power Meter"
        capability "Energy Meter"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Health Check"
		capability "Outlet"
        
        command "toggle"

		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite", model: "3200", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite", model: "3200-Sgb", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite", model: "4257050-RZHAC", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,000F,0B04", outClusters: "0019", manufacturer: "SmartThings", model: "outletv4", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019"
	}
                 

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	preferences {
		section {
			image(name: 'educationalcontent', multiple: true, images: [
					"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS1.jpg",
					"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS2.jpg"
			])
            input "maxPoints", "number", defaultValue: 1000, title: "Max number of points to plot", description: "Max number of points to plot", displayDuringSetup: false
            input "maxTime", "number", defaultValue: 60, title: "Max number of minutes to plot", description: "Max number of minutes to plot", displayDuringSetup: false
		}
	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "off", label: 'Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: 'Turning On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "turningOff", label: 'Turning Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
			tileAttribute("power", key: "SECONDARY_CONTROL") {
				attributeState "power", label: '${currentValue} W'
			}
		}
        /*
        multiAttributeTile(name: "power", type: "thermostat", width: 6, height: 4) {
        tileAttribute("device.power", key: "PRIMARY_CONTROL") {
				attributeState("power", action: "toggle", label:'${currentValue}W')//, nextState: "0")
		}
        
        tileAttribute("device.switch", key: "OPERATING_STATE") {
            attributeState("on", backgroundColor:"#00A0DC")
            attributeState("off", backgroundColor:"#555555")
        }
    }*/
		
         htmlTile(name:"graph",
				 action: "generateGraph",
				 refreshInterval: 10,
				 width: 6, height: 4,
				 whitelist: ["www.gstatic.com"])
		/*
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
		}*/

		standardTile("power", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "power", label: '${currentValue} W'
		}

		main "power"
		details(["switch", "graph"])//, "refresh"])
	}
}

mappings {
	path("/generateGraph") {action: [GET: "generateGraphHTML"]}
}


// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "description is $description"

	def event = zigbee.getEvent(description)

	if (event) {
		if (event.name == "power") {
			def value = (event.value as Integer) / 10
			event = createEvent(name: event.name, value: value, descriptionText: '{{ device.displayName }} power is {{ value }} Watts', translatable: true)
		} else if (event.name == "switch") {
			def descriptionText = event.value == "on" ? '{{ device.displayName }} is On' : '{{ device.displayName }} is Off'
			event = createEvent(name: event.name, value: event.value, descriptionText: descriptionText, translatable: true)
		}
	} else {
		def cluster = zigbee.parse(description)

		if (cluster && cluster.clusterId == 0x0006 && cluster.command == 0x07) {
			if (cluster.data[0] == 0x00) {
				log.debug "ON/OFF REPORTING CONFIG RESPONSE: " + cluster
				event = createEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
			} else {
				log.warn "ON/OFF REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
				event = null
			}
		} else {
			log.warn "DID NOT PARSE MESSAGE for description : $description"
			log.debug "${cluster}"
		}
	}
	return event ? createEvent(event) : event
}

def toggle() {
	//sendEvent(name: "power", value: "0")
	if (device.currentValue("switch") == 'on' ){off()}
    else {on()}
}

def off() {
	zigbee.off()
}

def on() {
	zigbee.on()
}
/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	return zigbee.onOffRefresh()
}

def refresh() {
	zigbee.onOffRefresh() + zigbee.electricMeasurementPowerRefresh()
}

def configure() {
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

	// OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
	refresh() + zigbee.onOffConfig(0, 300) + zigbee.electricMeasurementPowerConfig()
}



String getDataString()
{ 
   def minDate = new Date()
   def cnt = 0
   def mD = new Date(now() - 60000*settings.maxTime)
   def dataString = ""
   
   while ((cnt < settings.maxPoints) && (minDate > mD)){
   	   def theStates = device.statesBetween("power", mD-1, minDate, [max: settings.maxPoints])
       theStates.each()
        {
            if (minDate > it.date){ minDate = it.date }
            if (it.date > mD){
            	cnt += 1
            	dataString += ["new Date( $it.date.time )",it.floatValue].toString() + ","
            }
        }
    }
	return dataString
}

def generateGraphHTML() {
	def html = """
		<!DOCTYPE html>
			<html>
				<head>
					<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
					<script type="text/javascript">
						google.charts.load('current', {packages: ['corechart']});
						google.charts.setOnLoadCallback(drawGraph);
						function drawGraph() {
							var data = new google.visualization.DataTable();
							data.addColumn('datetime', 'time');
							data.addColumn('number', 'Power');
							data.addRows([
								${getDataString()}
							]);
							var options = {
								fontName: 'San Francisco, Roboto, Arial',
								height: 660,
                                curveType: 'none',
          						pointSize: 7,
								hAxis: {
									title: 'Time',
                                    textStyle: {color: '#004CFF', fontSize: '20'},
									titleTextStyle: {color: '#004CFF', fontSize: '20'},
                                    format: 'H:mm',
                                    gridlines: {
                                        count: -1,
                                        units: {
                                          days: {format: ['MMM dd']},
                                          hours: {format: ['HH:mm', 'ha']},
                                        }
                                      }
								},
								series: {
									0: {targetAxisIndex: 0, color: '#FF0000', lineWidth: 1}
								},
								vAxes: {
									0: {
										title: 'Power [W]',
										format: 'decimal',
										textStyle: {color: '#004CFF', fontSize: '20'},
										titleTextStyle: {color: '#004CFF', fontSize: '20'}
									},
								},
								legend: {
									position: 'none'
								},
								chartArea: {
									width: 860,
									height: '80%',
                                    left:80,
                                    top:20
								}
							};
							var chart = new google.visualization.SteppedAreaChart(document.getElementById('chart_div'));
							chart.draw(data, options);
						}
					</script>
				</head>
				<body>
					<div id="chart_div"></div>
				</body>
			</html>
		"""
	render contentType: "text/html", data: html, status: 200
}
