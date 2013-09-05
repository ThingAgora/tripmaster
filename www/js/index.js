/*
 * http://www.apache.org/licenses/LICENSE-2.0
 */
var app = {
    // Application Constructor
    initialize: function() {
        this.bindEvents();
		this.speedometer = document.getElementById('speedometer');
    },

    // Bind Event Listeners
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },

    // Error management
    errorCb: function() {
    	app.speedometer.innerHTML = "XXX";
    },
    
    // Geolocation management
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
    
    // deviceready Event Handler - The scope of 'this' is the event.
    onDeviceReady: function() {
		app.speedometer.innerHTML = "---";
		if (navigator.geolocation)
			app.positionWatchId = navigator.geolocation.watchPosition(app.positionWatchCb, app.errorCb, app.positionWatchOptions);
		else
			errorCb();
    }
};
