/**
 * GeoGig Web API
 * GeoGig Web API.  You can find out more about GeoGig at [http://geogig.org](http://geogig.org)
 *
 * OpenAPI spec version: 0.1.0
 * Contact: groldan@boundlessgeo.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 *
 * Swagger Codegen version: unset
 *
 * Do not edit the class manually.
 *
 */

(function(root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define(['ApiClient'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS-like environments that support module.exports, like Node.
    module.exports = factory(require('../ApiClient'));
  } else {
    // Browser globals (root is window)
    if (!root.Geogig) {
      root.Geogig = {};
    }
    root.Geogig.ProgressInfo = factory(root.Geogig.ApiClient);
  }
}(this, function(ApiClient) {
  'use strict';




  /**
   * The ProgressInfo model module.
   * @module model/ProgressInfo
   * @version 0.1-SNAPSHOT
   */

  /**
   * Constructs a new <code>ProgressInfo</code>.
   * Information about the progress of an asynchronous task
   * @alias module:model/ProgressInfo
   * @class
   */
  var exports = function() {
    var _this = this;



  };

  /**
   * Constructs a <code>ProgressInfo</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:model/ProgressInfo} obj Optional instance to populate.
   * @return {module:model/ProgressInfo} The populated <code>ProgressInfo</code> instance.
   */
  exports.constructFromObject = function(data, obj) {
    if (data) {
      obj = obj || new exports();

      if (data.hasOwnProperty('task_description')) {
        obj['task_description'] = ApiClient.convertToType(data['task_description'], 'String');
      }
      if (data.hasOwnProperty('progress_description')) {
        obj['progress_description'] = ApiClient.convertToType(data['progress_description'], 'String');
      }
    }
    return obj;
  }

  /**
   * @member {String} task_description
   * @default 'Waiting...'
   */
  exports.prototype['task_description'] = 'Waiting...';
  /**
   * @member {String} progress_description
   * @default ''
   */
  exports.prototype['progress_description'] = '';



  return exports;
}));


