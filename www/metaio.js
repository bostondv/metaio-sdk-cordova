// Metaio Cordova Plugin
// (c) Boston Dell-Vandenberg <boston@pomelodesign.com>
// MetaioPlugin.h may be freely distributed under the MIT license.

var exec = require('cordova/exec');

/**
 * Metaio object
 */

module.exports = {

  isLoaded: false,
  
  /**
   * Open Metaio view
   *
   * Checks if view is already loaded and if true, shows the view, and if false, loads a new view. Sets isLoaded to true on successful callback from Cordova.
   *
   * @param {Function} success
   * @param {Function} error
   */
  open: function(success, error) {
    function successWrapper() {
      metaio.isLoaded = true;
      if (typeof(success) === 'function') {
        success();
      }
    }
    exec(successWrapper, error, 'Metaio', 'open', []);
  },

  /**
   * Close Metaio view
   *
   * Checks if view is loaded and closes it if true, returns error callback if false. Sets isLoaded to false on successful callback from Cordova.
   *
   * @param {Function} success
   * @param {Function} error
   * @param {string} url
   */
  close: function(success, error, url) {
    exec(success, error, 'Metaio', 'close', [url]);
  },

  /**
   * Destroy Metaio view
   *
   * Completely remove the view.
   *
   * @param {Function} success
   * @param {Function} error
   */
  destroy: function(success, error) {
    function successWrapper() {
      metaio.isLoaded = false;
      if (typeof(success) === 'function') {
        success();
      }
    }
    exec(successWrapper, error, 'Metaio', 'destroy', []);
  }

};