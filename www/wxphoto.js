module.exports = {
    pick: function (successCallback, errorCallback，options) {
        cordova.exec(successCallback, errorCallback, "WXPhoto", "pick", [options]);
    },
    pickVideo: function(successCallback, errorCallback) {
    	cordova.exec(successCallback, errorCallback, "WXPhoto", "pickVideo", []);
    },
    compressVideo: function(sourceUrl, destPath, successCallback, errorCallback) {
    	cordova.exec(successCallback, errorCallback, "WXPhoto", "compressVideo", [sourceUrl, destPath]);
    },
    initialize: function() {
    	window.supportVideoUpload = true;
    }
}