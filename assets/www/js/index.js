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

    // deviceready Event Handler - The scope of 'this' is the event.
    onDeviceReady: function() {
		app.speedometer.innerHTML = "000";
    }
};
