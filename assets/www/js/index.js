/*
 * http://www.apache.org/licenses/LICENSE-2.0
 */
var app = {
	// Application views
	views: ['speedometer', 'status'],
	setView: function(index) {
		app.currentView.style.display = 'none';
		if (index >= 0 && index < app.views.length) {
			app.currentView = app[app.views[index]];
			app.currentView.style.display = 'block';
		}
	},
		
    // Application Constructor
    initialize: function() {
        this.bindEvents();
		this.navbar = document.getElementById(navbar);
		for (var i in this.views) {
			this[this.views[i]] = document.getElementById(this.views[i]);
			this[this.views[i]].style.display = 'none';
		}
		this.currentView = document.getElementById(this.views[0]);
		this.currentView.style.display = 'block';
		this.batteryStatusLog = document.getElementById("batteryStatusLog");
		this.batteryStatusBar = document.getElementById("batteryStatusBar");
		this.networkStatusLog = document.getElementById("networkStatusLog");
    },

    // Bind Event Listeners
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
        document.addEventListener('pause', this.onPause, false);
        document.addEventListener('resume', this.onDeviceReady, false);
    },

    // Geolocation management (speedometer view)
    positionWatchId: -1,
    positionWatchCb: function(position) {
    	if (position.coords.speed) {
    		var kmh = position.coords.speed * 3.6;
    		app.speedometer.innerHTML = kmh.toFixed(0);
    	}
    	else
    		app.speedometer.innerHTML = "000";
    },
    positionWatchOptions: {
    	frequency: 1000,
    	maximumAge: 60000, 
    	timeout: 10000,
    	enableHighAccuracy: true
    },
    // geolocation error management
    geoErrorCb: function() {
    	app.speedometer.innerHTML = "XXX";
    },
    
    // battery status management (status view)
    batteryLevelCritical: 15,
    batteryLevelLow: 30,
    onBatteryStatus: function(status) {
    	app.batteryStatusLog.innerHTML = "Battery level: " + status.level.toString() + "&#37;";
    	if (status.isPlugged) 
    		app.batteryStatusLog.innerHTML += " (charging)";
    	app.batteryStatusBar.style.width = status.level.toString() + "%";
    	if (status.level <= app.batteryLevelCritical)
    		app.batteryStatusBar.style.backgroundColor = "#FF0000";
    	else if (status.level <= app.batteryLevelLow)
    		app.batteryStatusBar.style.backgroundColor = "#FFFF00";
    	else
    		app.batteryStatusBar.style.backgroundColor = "#00FF00";
    },

    // network status management (status view)
    networkStates: {},
    networkStateInitialize: function() {
	    app.networkStates[Connection.UNKNOWN]  = 'Unknown connection';
	    app.networkStates[Connection.ETHERNET] = 'Ethernet connection';
	    app.networkStates[Connection.WIFI]     = 'WiFi connection';
	    app.networkStates[Connection.CELL_2G]  = 'Cell 2G connection';
	    app.networkStates[Connection.CELL_3G]  = 'Cell 3G connection';
	    app.networkStates[Connection.CELL_4G]  = 'Cell 4G connection';
	    app.networkStates[Connection.CELL]     = 'Cell generic connection';
	    app.networkStates[Connection.NONE]     = 'No network connection';
    },
    networkStateCb: function(networkState) {
    	app.networkStatusLog.innerHTML = "Network:<br>" + app.networkStates[networkState];
    },

    // device pause: clear watches
    onPause: function() {
		app.speedometer.innerHTML = "---";
		navigator.geolocation.clearWatch(app.positionWatchId);
		window.removeEventListener('batterystatus', app.onBatteryStatus, false);
    },
    
    // deviceready Event Handler - The scope of 'this' is the event.
    onDeviceReady: function() {
		window.addEventListener('batterystatus', app.onBatteryStatus, false);
		app.speedometer.innerHTML = "---";
		if (navigator.geolocation)
			app.positionWatchId = navigator.geolocation.watchPosition(app.positionWatchCb, app.geoErrorCb, app.positionWatchOptions);
		else
			geoErrorCb();
		if (navigator.network) {
			app.networkStateInitialize();
			app.networkStateCb(navigator.network.connection.type);
		}
		else
			app.networkStatusLog.innerHTML = "Network unavailable";
    }
    
};
