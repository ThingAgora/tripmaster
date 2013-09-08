/*
 * http://www.apache.org/licenses/LICENSE-2.0
 */
var app = {
	// Application views
	views: ['speedometer', 'status'],
	setView: function(index) {
		app.currentView.style.display = 'none';
		if (index > 0 && index < app.views.length) {
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
    },

    // Bind Event Listeners
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
        document.addEventListener('pause', this.onPause, false);
        document.addEventListener('resume', this.onDeviceReady, false);
    },

    // Error management
    errorCb: function() {
    	app.currentView.innerHTML = "XXX";
    },
    
    // Geolocation management (speedometer)
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
    // clear position watch on device pause
    onPause: function() {
		app.speedometer.innerHTML = "---";
		navigator.geolocation.clearWatch(app.positionWatchId);
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
